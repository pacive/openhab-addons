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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.smarthome.core.auth.client.oauth2.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestConnector implements NibeUplinkRestApi {

   private final ScheduledExecutorService scheduler;
    private long updateInterval;

    private final Map<Integer, NibeSystem> cachedSystems = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Category>> cachedCategories = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, Thermostat>> thermostats = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> trackedParameters = new ConcurrentHashMap<>();
    private final Deque<Request> queuedRequests = new ConcurrentLinkedDeque<>();
    private final Map<Integer, NibeUplinkRestCallbackListener> listeners = new ConcurrentHashMap<>();
    private @Nullable Future<?> standardRequestProducer;
    private @Nullable Future<?> softwareRequestProducer;
    private @Nullable Future<?> thermostatRequestProducer;
    private @Nullable Future<?> modeRequestProducer;
    private @Nullable Future<?> requestProcessor;

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestConnector.class);
    private final NibeUplinkRestRequestHandler requests;

    public NibeUplinkRestConnector(OAuthClientService oAuthClient, HttpClient httpClient,
                                   ScheduledExecutorService scheduler, long updateInterval) {
        this.scheduler = scheduler;
        this.updateInterval = updateInterval;
        requests = new NibeUplinkRestRequestHandler(oAuthClient, httpClient);
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
        if (cachedSystems.containsKey(systemId)) {
            return cachedSystems.get(systemId);
        }
        return updateSystem(systemId);
    }

    @Override
    public List<Category> getCategories(int systemId, boolean includeParameters) {
        Map<String, Category> cachedValues = cachedCategories.get(systemId);
        if (cachedValues.isEmpty()) {
            return updateCategories(systemId, includeParameters);
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

        if (thermostatRequestProducer == null || thermostatRequestProducer.isCancelled()) {
            thermostatRequestProducer = scheduler.scheduleWithFixedDelay(this::queueThermostatRequests,
                    THERMOSTAT_UPDATE_INTERVAL, THERMOSTAT_UPDATE_INTERVAL, TimeUnit.MINUTES);
        }
    }

    @Override
    public void removeThermostat(int systemId, int thermostatId) {
        Map<Integer, Thermostat> systemThermostats = thermostats.get(systemId);
        if (systemThermostats != null) {
            systemThermostats.remove(thermostatId);
            if (systemThermostats.isEmpty()) {
                thermostats.remove(systemId);
                if (thermostats.isEmpty()) {
                    thermostatRequestProducer.cancel(false);
                }
            }
        }
    }

    @Override
    public SystemConfig getSystemConfig(int systemId) {
        Request req = requests.createSystemConfigRequest(systemId);
        String resp = requests.makeRequest(req);
        SystemConfig config = parseSystemConfig(resp);
        if (cachedSystems.get(systemId) != null) {
            cachedSystems.get(systemId).setConfig(config);
        }
        return config;
    }

    @Override
    public SoftwareInfo getSoftwareInfo(int systemId) {
        Request req = requests.createSoftwareRequest(systemId);
        String resp = requests.makeRequest(req);
        return parseSoftwareInfo(resp);
    }

    private NibeSystem updateSystem(int systemId) {
        Request req = requests.createSystemRequest(systemId);
        String resp = requests.makeRequest(req);
        NibeSystem system = parseSystem(resp);
        if (cachedSystems.containsKey(systemId)) {
            system.setConfig(cachedSystems.get(systemId).getConfig());
        }
        cachedSystems.put(systemId, system);
        return system;
    }

    private List<Category> updateCategories(int systemId, boolean includeParameters) {
        Request req = requests.createCategoriesRequest(systemId, includeParameters);
        String resp = requests.makeRequest(req);
        List<Category> categories = parseCategoryList(resp);
        Map<String, Category> categoryCache = cachedCategories.get(systemId);
            for (Category category : categories) {
                categoryCache.putIfAbsent(category.getCategoryId(), category);
            }
        return categories;
    }

    @Override
    public void addCallbackListener(int systemId, NibeUplinkRestCallbackListener listener) {
        listeners.putIfAbsent(systemId, listener);
        trackedParameters.putIfAbsent(systemId, new HashSet<>());
        standardRequestProducer = scheduler.scheduleWithFixedDelay(this::queueStandardRequests, 1,
                updateInterval, TimeUnit.SECONDS);
    }

    @Override
    public void removeCallbackListener(int systemId) {
        listeners.remove(systemId);
        if (listeners.isEmpty()) {
            standardRequestProducer.cancel(false);
        }
        trackedParameters.remove(systemId);
    }

    private void queueStandardRequests() {

    }

    private void queueThermostatRequests() {

    }

    private void queueModeRequests() {

    }
}
