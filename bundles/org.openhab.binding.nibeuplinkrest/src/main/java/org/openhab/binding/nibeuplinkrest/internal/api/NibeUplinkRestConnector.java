/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.binding.nibeuplinkrest.internal.api;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;
import static org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestResponseParser.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.nibeuplinkrest.internal.api.RequestWrapper.RequestType;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Category;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Mode;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.api.model.SystemConfig;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Thermostat;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestHttpException;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBridgeHandler;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestConnector implements NibeUplinkRestApi {

    private static final int MAX_QUEUE_SIZE = 30;

    private final NibeUplinkRestBridgeHandler bridgeHandler;
    private final ScheduledExecutorService scheduler;
    private int updateInterval;
    private int softwareUpdateCheckInterval;

    private final Map<Integer, NibeSystem> cachedSystems = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Category>> cachedCategories = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, Thermostat>> thermostats = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> trackedParameters = new ConcurrentHashMap<>();
    private final Map<Integer, Mode> modes = new ConcurrentHashMap<>();
    private final BlockingDeque<RequestWrapper> queuedRequests = new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);
    private final Map<Integer, NibeUplinkRestCallbackListener> listeners = new ConcurrentHashMap<>();
    private @Nullable Future<?> standardRequestProducer;
    private @Nullable Future<?> softwareRequestProducer;
    private @Nullable Future<?> thermostatRequestProducer;
    private @Nullable Future<?> modeRequestProducer;
    private @Nullable Future<?> isAliveRequestProducer;
    private @Nullable Future<?> requestProcessor;

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestConnector.class);
    private final NibeUplinkRestRequestHandler requests;

    public NibeUplinkRestConnector(NibeUplinkRestBridgeHandler bridgeHandler, OAuthClientService oAuthClient,
            HttpClient httpClient, ScheduledExecutorService scheduler, int updateInterval,
            int softwareUpdateCheckInterval) {
        this.bridgeHandler = bridgeHandler;
        this.scheduler = scheduler;
        this.updateInterval = updateInterval;
        this.softwareUpdateCheckInterval = softwareUpdateCheckInterval;
        requests = new NibeUplinkRestRequestHandler(oAuthClient, httpClient);
    }

    @Override
    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
        logger.debug("Update interval changed, reloading scheduler");
        @Nullable
        Future<?> localRef = standardRequestProducer;
        if (localRef != null) {
            localRef.cancel(false);
        }
        standardRequestProducer = scheduler.scheduleWithFixedDelay(this::queueStandardRequests, updateInterval,
                updateInterval, TimeUnit.SECONDS);
    }

    @Override
    public void setSoftwareUpdateCheckInterval(int softwareUpdateCheckInterval) {
        this.softwareUpdateCheckInterval = softwareUpdateCheckInterval;
        logger.debug("Software update check interval changed, reloading scheduler");
        @Nullable
        Future<?> localRef = softwareRequestProducer;
        if (localRef != null) {
            localRef.cancel(false);
        }
        if (softwareUpdateCheckInterval > 0) {
            softwareRequestProducer = scheduler.scheduleWithFixedDelay(this::queueSoftwareRequests,
                    softwareUpdateCheckInterval, softwareUpdateCheckInterval, TimeUnit.DAYS);
        }
    }

    @Override
    public List<NibeSystem> getConnectedSystems() {
        logger.debug("Checking connected systems...");
        List<NibeSystem> systems;
        RequestWrapper req = requests.createConnectedSystemsRequest();
        try {
            String resp = requests.makeRequestWithRetry(req);
            systems = parseSystemList(resp).orElse(List.of());
        } catch (NibeUplinkRestException e) {
            systems = List.of();
        }
        logger.debug("{} systems found", systems.size());
        for (NibeSystem system : systems) {
            cachedSystems.putIfAbsent(system.getSystemId(), system);
        }
        return systems;
    }

    @Override
    public SystemConfig getSystemConfig(int systemId) throws NibeUplinkRestException {
        @Nullable
        NibeSystem system = cachedSystems.get(systemId);
        @Nullable
        SystemConfig config = null;
        if (system != null) {
            config = system.getConfig();
        }
        if (config == null) {
            RequestWrapper req = requests.createSystemConfigRequest(systemId);
            String resp = requests.makeRequestWithRetry(req);
            config = parseSystemConfig(resp).orElseThrow(() -> new NibeUplinkRestException("Unable to get config"));
        }
        if (system != null) {
            system.setConfig(config);
        }
        return config;
    }

    @Override
    public List<Category> getCategories(int systemId, boolean includeParameters) throws NibeUplinkRestException {
        Map<String, Category> cachedValues = cachedCategories.get(systemId);
        if (cachedValues == null || cachedValues.isEmpty()) {
            RequestWrapper req = requests.createCategoriesRequest(systemId, includeParameters);
            String resp = requests.makeRequestWithRetry(req);
            List<Category> categories = parseCategoryList(resp).orElse(List.of());
            cachedValues = new HashMap<>();
            for (Category category : categories) {
                cachedValues.putIfAbsent(category.getCategoryId(), category);
            }
            cachedCategories.put(systemId, cachedValues);
            return categories;
        }
        return new ArrayList<>(cachedValues.values());
    }

    @Override
    public void requestSystem(int systemId) {
        RequestWrapper req = requests.createSystemRequest(systemId);
        if (!queuedRequests.offerFirst(req)) {
            logger.debug("Queue full, request discarded");
        }
    }

    @Override
    public void requestLatestAlarm(int systemId) {
        RequestWrapper req = requests.createAlarmInfoRequest(systemId);
        if (!queuedRequests.offerFirst(req)) {
            logger.debug("Queue full, request discarded");
        }
    }

    @Override
    public void requestSoftwareInfo(int systemId) {
        RequestWrapper req = requests.createSoftwareRequest(systemId);
        if (!queuedRequests.offerFirst(req)) {
            logger.debug("Queue full, request discarded");
        }
    }

    @Override
    public void requestParameters(int systemId, Set<Integer> parameterIds) {
        RequestWrapper req = requests.createGetParametersRequest(systemId, parameterIds);
        if (!queuedRequests.offerFirst(req)) {
            logger.debug("Queue full, request discarded");
        }
    }

    @Override
    public void setParameters(int systemId, Map<Integer, Number> parameters) {
        logger.debug("Setting parameters: {}", parameters);
        RequestWrapper req = requests.createSetParametersRequest(systemId, parameters);
        if (!queuedRequests.offerFirst(req)) {
            logger.debug("Queue full, request discarded");
        }
    }

    @Override
    public void requestMode(int systemId) {
        logger.debug("Requesting mode from Nibe uplink");
        RequestWrapper req = requests.createGetModeRequest(systemId);
        if (!queuedRequests.offerFirst(req)) {
            logger.debug("Queue full, request discarded");
        }
    }

    @Override
    public void setMode(int systemId, Mode mode) {
        logger.debug("Setting mode: {}", mode);
        RequestWrapper req = requests.createSetModeRequest(systemId, mode);
        if (!queuedRequests.offerFirst(req)) {
            logger.debug("Queue full, request discarded");
        }
        if (mode != Mode.DEFAULT_OPERATION) {
            modes.put(systemId, mode);
        } else {
            modes.remove(systemId);
        }
    }

    @Override
    public void setThermostat(int systemId, Thermostat thermostat) {
        logger.debug("Setting thermostat '{}': temperature {}, setpoint {}", thermostat.getName(),
                thermostat.getActualTemp(), thermostat.getTargetTemp());
        RequestWrapper req = requests.createSetThermostatRequest(systemId, thermostat);
        if (!queuedRequests.offerFirst(req)) {
            logger.debug("Queue full, request discarded");
        }

        Map<Integer, Thermostat> systemThermostats = thermostats.get(systemId);
        if (systemThermostats != null) {
            systemThermostats.put(thermostat.getId(), thermostat);
        } else {
            systemThermostats = new HashMap<>();
            systemThermostats.put(thermostat.getId(), thermostat);
            thermostats.put(systemId, systemThermostats);
        }
    }

    @Override
    public void removeThermostat(int systemId, int thermostatId) {
        Map<Integer, Thermostat> systemThermostats = thermostats.get(systemId);
        if (systemThermostats != null) {
            logger.debug("Removing thermostat {}", thermostatId);
            systemThermostats.remove(thermostatId);
            if (systemThermostats.isEmpty()) {
                thermostats.remove(systemId);
            }
        }
    }

    @Override
    public synchronized void addCallbackListener(int systemId, NibeUplinkRestCallbackListener listener) {
        logger.debug("Adding callback listener for system {}", systemId);
        listeners.put(systemId, listener);
        trackedParameters.putIfAbsent(systemId, new HashSet<>());
        @Nullable
        Future<?> localRef = standardRequestProducer;
        if (localRef == null || localRef.isCancelled()) {
            startPolling(true);
        }
    }

    @Override
    public synchronized void removeCallbackListener(int systemId) {
        logger.debug("Removing callback listener for system {}", systemId);
        listeners.remove(systemId);
        if (listeners.isEmpty()) {
            cancelPolling();
        }
        trackedParameters.remove(systemId);
        modes.remove(systemId);
    }

    @Override
    public synchronized void addTrackedParameter(int systemId, int parameterId) {
        @Nullable
        Set<Integer> systemTrackedParameters = trackedParameters.getOrDefault(systemId, new HashSet<>());
        if (listeners.get(systemId) == null) {
            logger.debug("No listener for system {} adding tracked parameters anyway.", systemId);
        }
        trackedParameters.putIfAbsent(systemId, systemTrackedParameters);
        if (systemTrackedParameters.add(parameterId)) {
            logger.trace("System {} is now tracking parameter {}", systemId, parameterId);
        }
    }

    @Override
    public synchronized void removeTrackedParameter(int systemId, int parameterId) {
        Set<Integer> systemTrackedParameters = trackedParameters.get(systemId);
        if (systemTrackedParameters == null) {
            logger.debug("No tracked parameters for system {}", systemId);
            return;
        }
        if (systemTrackedParameters.remove(parameterId)) {
            logger.trace("System {} is no longer tracking parameter {}", systemId, parameterId);
        }
    }

    /**
     * Cancel all running scheduled jobs
     */
    public synchronized void cancelAllJobs() {
        cancelPolling();
        logger.debug("Clearing queue");
        queuedRequests.clear();
    }

    /**
     * Start all request producers and the request processor with their respective intervals
     */
    private synchronized void startPolling(boolean immediate) {
        logger.debug("Start polling jobs");
        @Nullable
        Future<?> localRef = standardRequestProducer;
        if (localRef == null || localRef.isCancelled()) {
            standardRequestProducer = scheduler.scheduleWithFixedDelay(this::queueStandardRequests,
                    immediate ? 0 : updateInterval, updateInterval, TimeUnit.SECONDS);
            logger.debug("Standard request producer started with interval {} seconds", updateInterval);
        }
        localRef = softwareRequestProducer;
        if (softwareUpdateCheckInterval > 0) {
            if (localRef == null || localRef.isCancelled()) {
                softwareRequestProducer = scheduler.scheduleWithFixedDelay(this::queueSoftwareRequests,
                        immediate ? 0 : softwareUpdateCheckInterval, softwareUpdateCheckInterval, TimeUnit.DAYS);
                logger.debug("Software request producer started with interval {} days", softwareUpdateCheckInterval);
            }
        }
        localRef = thermostatRequestProducer;
        if (localRef == null || localRef.isCancelled()) {
            thermostatRequestProducer = scheduler.scheduleWithFixedDelay(this::queueThermostatRequests,
                    THERMOSTAT_UPDATE_INTERVAL, THERMOSTAT_UPDATE_INTERVAL, TimeUnit.MINUTES);
            logger.debug("Thermostat request producer started");
        }
        localRef = modeRequestProducer;
        if (localRef == null || localRef.isCancelled()) {
            modeRequestProducer = scheduler.scheduleWithFixedDelay(this::queueModeRequests,
                    immediate ? 0 : MODE_UPDATE_INTERVAL, MODE_UPDATE_INTERVAL, TimeUnit.MINUTES);
            logger.debug("Mode request producer started");
        }
        localRef = requestProcessor;
        if (localRef == null || localRef.isCancelled()) {
            requestProcessor = scheduler.scheduleWithFixedDelay(this::processRequests, 0, REQUEST_INTERVAL,
                    TimeUnit.SECONDS);
            logger.debug("Request processor started");
        }
    }

    /**
     * Cancel all request producers and clear the request queue
     */
    private synchronized void cancelPolling() {
        logger.debug("Stopping all request producers");
        @Nullable
        Future<?> localRef = standardRequestProducer;
        if (localRef != null) {
            localRef.cancel(false);
        }
        localRef = softwareRequestProducer;
        if (localRef != null) {
            localRef.cancel(false);
        }
        localRef = modeRequestProducer;
        if (localRef != null) {
            localRef.cancel(false);
        }
        localRef = thermostatRequestProducer;
        if (localRef != null) {
            localRef.cancel(false);
        }
        localRef = requestProcessor;
        if (localRef != null) {
            localRef.cancel(true);
        }
    }

    /**
     * Queue requests for system and parameter updates
     */
    private void queueStandardRequests() {
        listeners.forEach((systemId, listener) -> {
            logger.trace("Queueing system and status request for {}", systemId);
            RequestWrapper systemRequest = requests.createSystemRequest(systemId);
            RequestWrapper statusRequest = requests.createStatusRequest(systemId);
            if (!queuedRequests.offer(systemRequest) || !queuedRequests.offer(statusRequest)) {
                logger.debug("Queue full, request discarded");
            }
        });

        trackedParameters.forEach((systemId, parameterSet) -> {
            if (!parameterSet.isEmpty() && listeners.get(systemId) != null) {
                Iterator<Integer> i = parameterSet.iterator();
                int counter = 0;
                Set<Integer> parameters = new HashSet<>();
                // Only 15 parameters are allowed in each request, iterate through the tracked parameters
                // and queue one request per 15.
                while (i.hasNext()) {
                    parameters.add(i.next());
                    counter++;
                    if (counter == MAX_PARAMETERS_PER_REQUEST) {
                        logger.trace("Queueing parameter request for {} with {} parameters", systemId,
                                parameters.size());
                        RequestWrapper req = requests.createGetParametersRequest(systemId, parameters);
                        if (!queuedRequests.offer(req)) {
                            logger.debug("Queue full, request discarded");
                        }
                        parameters.clear();
                        counter = 0;
                    }
                }
                if (!parameters.isEmpty()) {
                    logger.trace("Queueing parameter request for {} with {} parameters", systemId, parameters.size());
                    RequestWrapper req = requests.createGetParametersRequest(systemId, parameters);
                    if (!queuedRequests.offer(req)) {
                        logger.debug("Queue full, request discarded");
                    }
                }
            }
        });
    }

    /**
     * Queue requests to periodically update virtual thermostats
     */
    private void queueThermostatRequests() {
        thermostats.forEach((systemId, thermostatMap) -> {
            if (!thermostatMap.isEmpty()) {
                thermostatMap.values().forEach((thermostat) -> {
                    logger.trace("Queueing thermostat request for {}, thermostat {}", systemId, thermostat.getName());
                    RequestWrapper req = requests.createSetThermostatRequest(systemId, thermostat);
                    if (!queuedRequests.offer(req)) {
                        logger.debug("Queue full, request discarded");
                    }
                });
            }
        });
    }

    /**
     * Queue requests to periodically update the system's mode if it's not default
     */
    private void queueModeRequests() {
        modes.forEach((systemId, mode) -> {
            if (mode != Mode.DEFAULT_OPERATION) {
                logger.trace("Queueing mode request for {} with mode {}", systemId, mode);
                RequestWrapper req = requests.createSetModeRequest(systemId, mode);
                if (!queuedRequests.offer(req)) {
                    logger.debug("Queue full, request discarded");
                }
            }
        });
    }

    /**
     * Queue request for software info
     */
    private void queueSoftwareRequests() {
        for (int systemId : listeners.keySet()) {
            logger.trace("Queueing software update request for {}", systemId);
            RequestWrapper req = requests.createSoftwareRequest(systemId);
            if (!queuedRequests.offer(req)) {
                logger.debug("Queue full, request discarded");
            }
        }
    }

    private void startAliveCheck() {
        isAliveRequestProducer = scheduler.scheduleWithFixedDelay(() -> {
            RequestWrapper req = requests.createConnectedSystemsRequest();
            try {
                requests.makeRequestWithRetry(req);
                // If there was no exception the connection works again - resume polling
                @Nullable
                Future<?> localRef = isAliveRequestProducer;
                if (localRef != null) {
                    logger.debug("Nibe Uplink back online");
                    bridgeHandler.signalServerOnline();
                    startPolling(false);
                    localRef.cancel(false);
                    isAliveRequestProducer = null;
                }
            } catch (NibeUplinkRestHttpException e) {
                logger.trace("Server error: {}", e.getResponseCode());
            } catch (NibeUplinkRestException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Request failed: {}", Optional.ofNullable(e.getCause()).orElse(e).getMessage());
                }
            }

        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Retrieves requests from the queue an send them with 5 second intervals, to stay
     * clear of the rate limit
     */
    private void processRequests() {
        RequestWrapper req;
        try {
            req = queuedRequests.take();
        } catch (InterruptedException e) {
            logger.trace("Interrupted while waiting for queue");
            return;
        }

        Integer systemId = req.getSystemId();
        RequestType requestType = req.getType();

        NibeUplinkRestCallbackListener listener = listeners.get(systemId);
        String resp;

        if (queuedRequests.size() > MAX_QUEUE_SIZE - 5) {
            logger.warn("Request queue nearly full, consider increasing update interval");
        }

        logger.trace("Executing request of type {} for system {}", requestType, systemId);
        try {
            resp = requests.makeRequestWithRetry(req);
        } catch (NibeUplinkRestHttpException e) {
            // If Nibe's server has problems, stop polling and periodically check if back online
            if (e.getResponseCode() >= 500) {
                logger.debug("Server error, cancelling requests and starting alive check.");
                bridgeHandler.signalServerError(e.getResponseCode());
                cancelPolling();
                // Put request back first in the queue to be retried when connection is restored
                queuedRequests.offerFirst(req);
                startAliveCheck();
            } else {
                logger.debug("Nibe Uplink responded with an error: {}", e.getMessage());
            }
            return;
        } catch (NibeUplinkRestException e) {
            logger.debug("Failed to get data from Nibe Uplink: {}",
                    Optional.ofNullable(e.getCause()).orElse(e).getMessage());
            return;
        }

        if (listener == null) {
            logger.debug("No listener for systemId {}", systemId);
            return;
        }

        // Callback
        try {
            switch (requestType) {
                case SYSTEM:
                    Optional<NibeSystem> system = parseSystem(resp);
                    if (system.isPresent()) {
                        cachedSystems.put(systemId, system.get());
                        listener.systemUpdated(system.get());
                    }
                    break;
                case STATUS:
                    listener.statusUpdated(parseStatus(resp));
                    break;
                case PARAMETER_GET:
                    parseParameterList(resp).ifPresent(listener::parametersUpdated);
                    break;
                case MODE_GET:
                    parseMode(resp).ifPresent(listener::modeUpdated);
                    break;
                case SOFTWARE:
                    parseSoftwareInfo(resp).ifPresent(listener::softwareUpdateAvailable);
                    break;
                case ALARM:
                    parseAlarmInfoList(resp).ifPresent(alarmInfo -> listener.alarmInfoUpdated(alarmInfo.get(0)));
                default:
                    break;
            }
        } catch (JsonParseException e) {
            logger.debug("Failed to parse json: {}\n{}", e.getMessage(), resp);
        }
    }
}
