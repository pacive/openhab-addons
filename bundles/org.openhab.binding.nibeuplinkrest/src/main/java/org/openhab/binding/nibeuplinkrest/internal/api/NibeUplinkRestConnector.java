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
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
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

    private final NibeUplinkRestBridgeHandler bridgeHandler;
    private final ScheduledExecutorService scheduler;
    private long updateInterval;
    private long softwareUpdateCheckInterval;

    private final Map<Integer, @Nullable NibeSystem> cachedSystems = new ConcurrentHashMap<>();
    private final Map<Integer, @Nullable Map<String, Category>> cachedCategories = new ConcurrentHashMap<>();
    private final Map<Integer, @Nullable Map<Integer, Thermostat>> thermostats = new ConcurrentHashMap<>();
    private final Map<Integer, @Nullable Set<Integer>> trackedParameters = new ConcurrentHashMap<>();
    private final Map<Integer, @Nullable Mode> modes = new ConcurrentHashMap<>();
    private final Deque<@Nullable Request> queuedRequests = new ConcurrentLinkedDeque<>();
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
        if (standardRequestProducer != null) { standardRequestProducer.cancel(false); }
        standardRequestProducer = scheduler.scheduleWithFixedDelay(this::queueStandardRequests, 1,
                updateInterval, TimeUnit.SECONDS);
    }

    @Override
    public void setSoftwareUpdateCheckInterval(int softwareUpdateCheckInterval) {
        this.softwareUpdateCheckInterval = softwareUpdateCheckInterval;
        if (softwareRequestProducer != null) { softwareRequestProducer.cancel(false); }
        if (softwareUpdateCheckInterval > 0) {
            softwareRequestProducer = scheduler.scheduleWithFixedDelay(this::queueSoftwareRequests, 0,
                    softwareUpdateCheckInterval, TimeUnit.DAYS);
        }
    }

    @Override
    public List<NibeSystem> getConnectedSystems() {
        cachedSystems.clear();
        Request req = requests.createConnectedSystemsRequest();
        String resp = requests.makeRequest(req);
        List<NibeSystem> systems = parseSystemList(resp);
        for (NibeSystem system : systems) {
            cachedSystems.put(system.getSystemId(), system);
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
        }
        cachedSystems.put(systemId, system);
        return system;
    }

    @Override
    public SystemConfig getSystemConfig(int systemId) {
        Request req = requests.createSystemConfigRequest(systemId);
        String resp = requests.makeRequest(req);
        SystemConfig config = parseSystemConfig(resp);
        NibeSystem system = cachedSystems.get(systemId);
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
        if (systemTrackedParameters == null || listeners.get(systemId) == null) {
            throw new NibeUplinkRestException("No listener for system " + systemId);
        }
        if (systemTrackedParameters.add(parameterId)) {
            logger.trace("System {} is now tracking parameter {}", systemId, parameterId);
        }
    }

    @Override
    public void removeTrackedParameter(int systemId, int parameterId) {
        Set<Integer> systemTrackedParameters = trackedParameters.get(systemId);
        if (systemTrackedParameters == null || listeners.get(systemId) == null) {
            throw new NibeUplinkRestException("No listener for system " + systemId);
        }
        if (systemTrackedParameters.remove(parameterId)) {
            logger.trace("System {} is no longer tracking parameter {}", systemId, parameterId);
        }
    }

    @Override
    public void setParameters(int systemId, Map<Integer, Integer> parameters) {
        Request req = requests.createSetParametersRequest(systemId, parameters);
        requests.makeRequest(req);
    }

    @Override
    public void setMode(int systemId, Mode mode) {
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
            systemThermostats.remove(thermostatId);
            if (systemThermostats.isEmpty()) {
                thermostats.remove(systemId);
            }
        }
    }

    @Override
    public void addCallbackListener(int systemId, NibeUplinkRestCallbackListener listener) {
        listeners.put(systemId, listener);
        trackedParameters.putIfAbsent(systemId, new HashSet<>());
        if (standardRequestProducer == null || standardRequestProducer.isCancelled()) {
            startPolling();
        }
    }

    @Override
    public void removeCallbackListener(int systemId) {
        listeners.remove(systemId);
        if (listeners.isEmpty()) {
            cancelPolling();
        }
        trackedParameters.remove(systemId);
    }

    private void startPolling() {
        if (standardRequestProducer == null || standardRequestProducer.isCancelled()) {
            standardRequestProducer = scheduler.scheduleWithFixedDelay(this::queueStandardRequests,
                    REQUEST_INTERVAL - 1, updateInterval, TimeUnit.SECONDS);
        }
        if (softwareUpdateCheckInterval > 0) {
            if (softwareRequestProducer == null || softwareRequestProducer.isCancelled()) {
                softwareRequestProducer = scheduler.scheduleWithFixedDelay(this::queueSoftwareRequests,
                        0, softwareUpdateCheckInterval, TimeUnit.DAYS);
            }
        }
        if (thermostatRequestProducer == null || thermostatRequestProducer.isCancelled()) {
            thermostatRequestProducer = scheduler.scheduleWithFixedDelay(this::queueThermostatRequests,
                    THERMOSTAT_UPDATE_INTERVAL, THERMOSTAT_UPDATE_INTERVAL, TimeUnit.MINUTES);
        }
        if (modeRequestProducer == null || modeRequestProducer.isCancelled()) {
            modeRequestProducer = scheduler.scheduleWithFixedDelay(this::queueModeRequests,
                    MODE_UPDATE_INTERVAL / 2, MODE_UPDATE_INTERVAL, TimeUnit.MINUTES);
        }
        if (requestProcessor == null || requestProcessor.isCancelled()) {
            requestProcessor = scheduler.scheduleWithFixedDelay(this::processRequests, REQUEST_INTERVAL,
                    REQUEST_INTERVAL, TimeUnit.SECONDS);
        }
    }

    private void cancelPolling() {
        if (standardRequestProducer != null) { standardRequestProducer.cancel(false); }
        if (softwareRequestProducer != null) { softwareRequestProducer.cancel(false); }
        if (modeRequestProducer != null) { modeRequestProducer.cancel(false); }
        if (thermostatRequestProducer != null) { thermostatRequestProducer.cancel(false); }
        queuedRequests.clear();
    }

    private void queueStandardRequests() {
        listeners.forEach((systemId, listener) -> {
            if (listener != null) {
                Request req = requests.createSystemRequest(systemId);
                queuedRequests.add(req);
            }
        });

        trackedParameters.forEach((systemId, parameterSet) -> {
            if (parameterSet != null && !parameterSet.isEmpty()) {
                Iterator<Integer> i = parameterSet.iterator();
                int counter = 0;
                Set<Integer> parameters = new HashSet<>();
                while (i.hasNext()) {
                    parameters.add(i.next());
                    counter++;
                    if (counter == MAX_PARAMETERS_PER_REQUEST) {
                        Request req = requests.createGetParametersRequest(systemId, parameters);
                        queuedRequests.add(req);
                        parameters.clear();
                        counter = 0;
                    }
                }
                if (!parameters.isEmpty()) {
                    Request req = requests.createGetParametersRequest(systemId, parameters);
                    queuedRequests.add(req);
                }
            }
        });
    }

    private void queueThermostatRequests() {
        thermostats.forEach((systemId, thermostatMap) -> {
            if (thermostatMap != null && !thermostatMap.isEmpty()) {
                thermostatMap.values().forEach((thermostat) -> {
                    Request req = requests.createSetThermostatRequest(systemId, thermostat);
                    queuedRequests.add(req);
                });
            }
        });
    }

    private void queueModeRequests() {
        modes.forEach((systemId, mode) -> {
            if (mode != null && mode != Mode.DEFAULT_OPERATION) {
                Request req = requests.createSetModeRequest(systemId, mode);
                queuedRequests.add(req);
            }
        });
    }

    private void queueSoftwareRequests() {
        for (int systemId : listeners.keySet()) {
            Request req = requests.createSoftwareRequest(systemId);
            queuedRequests.add(req);
        }
    }

    private void processRequests() {
        Request req = queuedRequests.poll();
        int systemId;
        RequestType requestType;
        NibeUplinkRestCallbackListener listener;
        String resp;

        if (req == null) { return; }

        if (queuedRequests.size() >= updateInterval / REQUEST_INTERVAL) {
            logger.warn("Request queue too large, consider increasing update interval");
        }

        systemId = (int) req.getAttributes().get(NibeUplinkRestRequestHandler.SYSTEM_ID);
        requestType = (RequestType) req.getAttributes().get(NibeUplinkRestRequestHandler.REQUEST_TYPE);
        listener = listeners.get(systemId);

        if (listener == null) {
            logger.debug("No listener for systemId {}", systemId);
            return;
        }

        try {
            resp = requests.makeRequest(req);
        } catch (NibeUplinkRestHttpException e) {
            if (e.getResponseCode() >= 500) {
                bridgeHandler.signalServerError(e.getResponseCode());
            }
            cancelPolling();
            isAliveRequestProducer = scheduler.scheduleWithFixedDelay(() -> {
                queuedRequests.add(requests.createConnectedSystemsRequest());
            }, 1, 1, TimeUnit.MINUTES);
            return;
        } catch (NibeUplinkRestException e) {
            logger.warn("Failed to get data from Nibe Uplink: {}", e.getMessage());
            return;
        }

        if (isAliveRequestProducer != null) {
            isAliveRequestProducer.cancel(false);
            bridgeHandler.signalServerOnline();
            startPolling();
        }

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
                listener.softWareUpdateAvailable(parseSoftwareInfo(resp));
                break;
            default:
                break;
        }
    }
}
