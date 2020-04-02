/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import static org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestRequestHandler.RequestType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.smarthome.core.auth.client.oauth2.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestHttpException;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestConnector implements NibeUplinkRestApi {

    private static final int MAX_QUEUE_SIZE = 30;

    private final NibeUplinkRestBridgeHandler bridgeHandler;
    private final ScheduledExecutorService scheduler;
    private long updateInterval;
    private long softwareUpdateCheckInterval;

    private final Map<Integer, @Nullable NibeSystem> cachedSystems = new ConcurrentHashMap<>();
    private final Map<Integer, @Nullable Map<String, Category>> cachedCategories = new ConcurrentHashMap<>();
    private final Map<Integer, @Nullable Map<Integer, Thermostat>> thermostats = new ConcurrentHashMap<>();
    private final Map<Integer, @Nullable Set<Integer>> trackedParameters = new ConcurrentHashMap<>();
    private final Map<Integer, @Nullable Mode> modes = new ConcurrentHashMap<>();
    private final BlockingQueue<@Nullable Request> queuedRequests = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    private final Map<Integer, @Nullable NibeUplinkRestCallbackListener> listeners = new ConcurrentHashMap<>();
    private @Nullable Future<?> standardRequestProducer;
    private @Nullable Future<?> softwareRequestProducer;
    private @Nullable Future<?> thermostatRequestProducer;
    private @Nullable Future<?> modeRequestProducer;
    private @Nullable Future<?> isAliveRequestProducer;
    private @Nullable Future<?> requestProcessor;

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestConnector.class);
    private final NibeUplinkRestRequestHandler requests;

    public NibeUplinkRestConnector(NibeUplinkRestBridgeHandler bridgeHandler, OAuthClientService oAuthClient, HttpClient httpClient,
                                   ScheduledExecutorService scheduler, long updateInterval,
                                   long softwareUpdateCheckInterval) {
        this.bridgeHandler = bridgeHandler;
        this.scheduler = scheduler;
        this.updateInterval = updateInterval;
        this.softwareUpdateCheckInterval = softwareUpdateCheckInterval;
        requests = new NibeUplinkRestRequestHandler(oAuthClient, httpClient);
    }

    @Override
    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
        logger.debug("Update interval changed, reloading schedueler");
        if (standardRequestProducer != null) { standardRequestProducer.cancel(false); }
        standardRequestProducer = scheduler.scheduleWithFixedDelay(this::queueStandardRequests, 1,
                updateInterval, TimeUnit.SECONDS);
    }

    @Override
    public void setSoftwareUpdateCheckInterval(int softwareUpdateCheckInterval) {
        this.softwareUpdateCheckInterval = softwareUpdateCheckInterval;
        logger.debug("Software update check interval changed, reloading scheduler");
        if (softwareRequestProducer != null) { softwareRequestProducer.cancel(false); }
        if (softwareUpdateCheckInterval > 0) {
            softwareRequestProducer = scheduler.scheduleWithFixedDelay(this::queueSoftwareRequests, 0,
                    softwareUpdateCheckInterval, TimeUnit.DAYS);
        }
    }

    @Override
    public List<NibeSystem> getConnectedSystems() {
        logger.debug("Checking connected systems...");
        Request req = requests.createConnectedSystemsRequest();
        String resp = requests.makeRequest(req);
        List<NibeSystem> systems = parseSystemList(resp);
        logger.debug("{} systems found", systems.size());
        for (NibeSystem system : systems) {
            cachedSystems.putIfAbsent(system.getSystemId(), system);
        }
        return systems;
    }

    @Override
    public NibeSystem getSystem(int systemId) {
        NibeSystem system = cachedSystems.get(systemId);
        if (system == null) {
            Request req = requests.createSystemRequest(systemId);
            String resp = requests.makeRequest(req);
            system = parseSystem(resp);
            cachedSystems.put(systemId, system);
        }
        return system;
    }

    @Override
    public SystemConfig getSystemConfig(int systemId) {
        NibeSystem system = cachedSystems.get(systemId);
        SystemConfig config = null;
        if (system != null) {
            config = system.getConfig();
        }
        if (config == null) {
            Request req = requests.createSystemConfigRequest(systemId);
            String resp = requests.makeRequest(req);
            config = parseSystemConfig(resp);
        }
        if (system != null) {
            system.setConfig(config);
        }
        return config;
    }

    @Override
    public SoftwareInfo getSoftwareInfo(int systemId) {
        Request req = requests.createSoftwareRequest(systemId);
        String resp = requests.makeRequest(req);
        return parseSoftwareInfo(resp);
    }

    @Override
    public List<Category> getCategories(int systemId, boolean includeParameters) {
        Map<String, Category> cachedValues = cachedCategories.get(systemId);
        if (cachedValues == null || cachedValues.isEmpty()) {
            Request req = requests.createCategoriesRequest(systemId, includeParameters);
            String resp = requests.makeRequest(req);
            List<Category> categories = parseCategoryList(resp);
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
    public void addTrackedParameter(int systemId, int parameterId) {
        Set<Integer> systemTrackedParameters = trackedParameters.get(systemId);
        if (systemTrackedParameters == null) {
            if (listeners.get(systemId) == null) {
                logger.debug("No listener for system {} adding tracked parameters anyway.", systemId);
            }
            systemTrackedParameters = new HashSet<>();
            trackedParameters.putIfAbsent(systemId, systemTrackedParameters);
        }
        if (systemTrackedParameters.add(parameterId)) {
            logger.trace("System {} is now tracking parameter {}", systemId, parameterId);
        }
    }

    @Override
    public void removeTrackedParameter(int systemId, int parameterId) {
        Set<Integer> systemTrackedParameters = trackedParameters.get(systemId);
        if (systemTrackedParameters == null) {
            logger.debug("No tracked parameters for system {}", systemId);
            return;
        }
        if (systemTrackedParameters.remove(parameterId)) {
            logger.trace("System {} is no longer tracking parameter {}", systemId, parameterId);
        }
    }

    @Override
    public void setParameters(int systemId, Map<Integer, Integer> parameters) {
        logger.debug("Setting parameters: {}", parameters);
        Request req = requests.createSetParametersRequest(systemId, parameters);
        requests.makeRequest(req);
    }

    @Override
    public void setMode(int systemId, Mode mode) {
        logger.debug("Setting mode: {}", mode);
        Request req = requests.createSetModeRequest(systemId, mode);
        requests.makeRequest(req);
        if (mode != Mode.DEFAULT_OPERATION) {
            modes.put(systemId, mode);
        } else {
            modes.remove(systemId);
        }
    }

    @Override
    public void setThermostat(int systemId, Thermostat thermostat) {
        logger.debug("Setting thermostat '{}': temperature {}, setpoint {}", thermostat.getName(),
                thermostat.getCurrentTemperature(), thermostat.getTargetTemperature());
        Request req = requests.createSetThermostatRequest(systemId, thermostat);
        requests.makeRequest(req);

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
            logger.debug("Removing thermostat {}", systemThermostats.get(thermostatId).getName());
            systemThermostats.remove(thermostatId);
            if (systemThermostats.isEmpty()) {
                thermostats.remove(systemId);
            }
        }
    }

    @Override
    public void addCallbackListener(int systemId, NibeUplinkRestCallbackListener listener) {
        logger.debug("Adding callback listener for system {}", systemId);
        listeners.put(systemId, listener);
        trackedParameters.putIfAbsent(systemId, new HashSet<>());
        if (standardRequestProducer == null || standardRequestProducer.isCancelled()) {
            startPolling();
        }
    }

    @Override
    public void removeCallbackListener(int systemId) {
        logger.debug("Removing callback listener for system {}", systemId);
        listeners.remove(systemId);
        if (listeners.isEmpty()) {
            cancelPolling();
        }
        trackedParameters.remove(systemId);
        modes.remove(systemId);
    }

    /**
     * Cancel all running scheduled jobs
     */
    public void cancelAllJobs() {
        cancelPolling();
        logger.debug("Stopping request processor");
        if (requestProcessor != null) { requestProcessor.cancel(false); }
    }

    /**
     * Start all request producers and the request processor with their respective intervals
     */
    private void startPolling() {
        logger.debug("Start polling jobs");
        if (standardRequestProducer == null || standardRequestProducer.isCancelled()) {
            standardRequestProducer = scheduler.scheduleWithFixedDelay(this::queueStandardRequests,
                    REQUEST_INTERVAL - 1, updateInterval, TimeUnit.SECONDS);
            logger.trace("Standard request producer started with interval {} seconds", updateInterval);
        }
        if (softwareUpdateCheckInterval > 0) {
            if (softwareRequestProducer == null || softwareRequestProducer.isCancelled()) {
                softwareRequestProducer = scheduler.scheduleWithFixedDelay(this::queueSoftwareRequests,
                        0, softwareUpdateCheckInterval, TimeUnit.DAYS);
                logger.trace("Software request producer started with interval {} days", softwareUpdateCheckInterval);
            }
        }
        if (thermostatRequestProducer == null || thermostatRequestProducer.isCancelled()) {
            thermostatRequestProducer = scheduler.scheduleWithFixedDelay(this::queueThermostatRequests,
                    THERMOSTAT_UPDATE_INTERVAL, THERMOSTAT_UPDATE_INTERVAL, TimeUnit.MINUTES);
            logger.trace("Thermostat request producer started");
        }
        if (modeRequestProducer == null || modeRequestProducer.isCancelled()) {
            modeRequestProducer = scheduler.scheduleWithFixedDelay(this::queueModeRequests,
                    MODE_UPDATE_INTERVAL / 2, MODE_UPDATE_INTERVAL, TimeUnit.MINUTES);
            logger.trace("Mode request producer started");
        }
        if (requestProcessor == null || requestProcessor.isCancelled()) {
            requestProcessor = scheduler.scheduleWithFixedDelay(this::processRequests, REQUEST_INTERVAL,
                    REQUEST_INTERVAL, TimeUnit.SECONDS);
            logger.trace("Request processor started");
        }
    }

    /**
     * Cancel all request producers and clear the request queue
     */
    private void cancelPolling() {
        logger.debug("Stopping all request producers and clearing queue");
        if (standardRequestProducer != null) { standardRequestProducer.cancel(false); }
        if (softwareRequestProducer != null) { softwareRequestProducer.cancel(false); }
        if (modeRequestProducer != null) { modeRequestProducer.cancel(false); }
        if (thermostatRequestProducer != null) { thermostatRequestProducer.cancel(false); }
        queuedRequests.clear();
    }

    /**
     * Queue requests for system and parameter updates
     */
    private void queueStandardRequests() {
        listeners.forEach((systemId, listener) -> {
            if (listener != null) {
                logger.trace("Queueing system request for {}", systemId);
                try {
                    Request req = requests.createSystemRequest(systemId);
                    queuedRequests.add(req);
                } catch (RuntimeException e) {
                    logger.warn("{}", e.getMessage());
                }
            }
        });

        trackedParameters.forEach((systemId, parameterSet) -> {
            if (parameterSet != null && !parameterSet.isEmpty() && listeners.get(systemId) != null) {
                Iterator<Integer> i = parameterSet.iterator();
                int counter = 0;
                Set<Integer> parameters = new HashSet<>();
                // Only 15 parameter are allowed in each request, so queue as many as necessary
                while (i.hasNext()) {
                    parameters.add(i.next());
                    counter++;
                    if (counter == MAX_PARAMETERS_PER_REQUEST) {
                        logger.trace("Queueing parameter request for {} with {} parameters", systemId, parameters.size());
                        try {
                            Request req = requests.createGetParametersRequest(systemId, parameters);
                            queuedRequests.add(req);
                        } catch (RuntimeException e) {
                            logger.warn("{}", e.getMessage());
                        }
                        parameters.clear();
                        counter = 0;
                    }
                }
                if (!parameters.isEmpty()) {
                    logger.trace("Queueing parameter request for {} with {} parameters", systemId, parameters.size());
                    try {
                        Request req = requests.createGetParametersRequest(systemId, parameters);
                        queuedRequests.add(req);
                    } catch (RuntimeException e) {
                        logger.warn("{}", e.getMessage());
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
            if (thermostatMap != null && !thermostatMap.isEmpty()) {
                thermostatMap.values().forEach((thermostat) -> {
                    logger.trace("Queueing thermostat request for {}, thermostat {}", systemId, thermostat.getName());
                    try {
                        Request req = requests.createSetThermostatRequest(systemId, thermostat);
                        queuedRequests.add(req);
                    } catch (RuntimeException e) {
                        logger.warn("{}", e.getMessage());
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
            if (mode != null && mode != Mode.DEFAULT_OPERATION) {
                logger.trace("Queueing mode request for {} with mode {}", systemId, mode);
                try {
                    Request req = requests.createSetModeRequest(systemId, mode);
                    queuedRequests.add(req);
                } catch (RuntimeException e) {
                    logger.warn("{}", e.getMessage());
                }
            } else {
                try {
                    Request req = requests.createGetModeRequest(systemId);
                    queuedRequests.add(req);
                } catch (RuntimeException e) {
                    logger.warn("{}", e.getMessage());
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
            try {
                Request req = requests.createSoftwareRequest(systemId);
                queuedRequests.add(req);
            } catch (RuntimeException e) {
                logger.warn("{}", e.getMessage());
            }
        }
    }

    /**
     * Retrieves requests from the queue an send them with 5 second intervals, to stay
     * clear of the rate limit
     */
    private void processRequests() {
        Request req;
        try {
            req = queuedRequests.poll(updateInterval * 2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.trace("No requests queued for more than {} seconds", updateInterval * 2);
            return;
        }

        int systemId;
        RequestType requestType;
        NibeUplinkRestCallbackListener listener;
        String resp;

        if (req == null) { return; }

        if (queuedRequests.size() > MAX_QUEUE_SIZE - 5) {
            logger.warn("Request queue nearly full, consider increasing update interval");
        }

        systemId = (int) req.getAttributes().get(NibeUplinkRestRequestHandler.SYSTEM_ID);
        requestType = (RequestType) req.getAttributes().get(NibeUplinkRestRequestHandler.REQUEST_TYPE);
        listener = listeners.get(systemId);

        if (listener == null) {
            logger.debug("No listener for systemId {}", systemId);
            return;
        }

        logger.trace("Executing request of type {} for system {}", requestType, systemId);
        try {
            resp = requests.makeRequest(req);
        } catch (NibeUplinkRestHttpException e) {
            // If Nibe's server has problems, stop polling and periodically check if back online
            if (e.getResponseCode() >= 500) {
                logger.debug("Server error, cancelling requests and starting alive check.");
                bridgeHandler.signalServerError(e.getResponseCode());
                cancelPolling();
                isAliveRequestProducer = scheduler.scheduleWithFixedDelay(() -> {
                    queuedRequests.add(requests.createConnectedSystemsRequest());
                }, 1, 1, TimeUnit.MINUTES);
            } else {
                logger.debug("Nibe Uplink responded with an error: {}", e.getMessage());
            }
            return;
        } catch (RuntimeException e) {
            logger.debug("Failed to get data from Nibe Uplink: {}", e.getMessage());
            return;
        }

        // If we get to here the connection works
        if (isAliveRequestProducer != null) {
            logger.debug("Nibe Uplink back online");
            isAliveRequestProducer.cancel(false);
            isAliveRequestProducer = null;
            bridgeHandler.signalServerOnline();
            startPolling();
        }

        // Callback
        switch (requestType) {
            case SYSTEM:
                listener.systemUpdated(parseSystem(resp));
                break;
            case PARAMETER_GET:
                listener.parametersUpdated(parseParameterList(resp));
                break;
            case MODE_GET:
                listener.modeUpdated(parseMode(resp));
                break;
            case SOFTWARE:
                listener.softwareUpdateAvailable(parseSoftwareInfo(resp));
                break;
            default:
                break;
        }
    }
}
