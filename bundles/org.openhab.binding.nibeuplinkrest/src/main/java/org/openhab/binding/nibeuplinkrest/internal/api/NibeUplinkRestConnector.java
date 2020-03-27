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
import static org.eclipse.jetty.http.HttpStatus.*;

import com.google.gson.Gson;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.auth.client.oauth2.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkException;
import org.openhab.binding.nibeuplinkrest.internal.util.StringConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestConnector implements NibeUplinkRestApi, AccessTokenRefreshListener {

    private static final String CONTENT_TYPE = "application/json";
    private static final String BEARER = "Bearer ";
    private static final String REQUEST_TYPE = "requestType";
    private static final String SYSTEM_ID = "systemId";
    private static final int REQUEST_TYPE_SYSTEM = 1;
    private static final int REQUEST_TYPE_PARAMETER_GET = 2;
    private static final int REQUEST_TYPE_SOFTWARE = 3;
    private static final int REQUEST_TYPE_MODE_GET = 4;
    private static final int REQUEST_TYPE_CONFIG = 5;
    private static final int REQUEST_TYPE_CATEGORIES = 6;
    private static final int REQUEST_TYPE_PARAMETER_SET = 7;
    private static final int REQUEST_TYPE_MODE_SET = 8;
    private static final int REQUEST_TYPE_THERMOSTAT = 9;
    private static final int REQUEST_TYPE_SYSTEMS = 10;


    private final OAuthClientService oAuthClient;
    private final HttpClient httpClient;
    private final Map<Integer, NibeSystem> cachedSystems = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Category>> cachedCategories = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> trackedParameters = new HashMap<>();
    private final Deque<Request> queuedRequests = new ConcurrentLinkedDeque<>();
    private final Map<Integer, NibeUplinkRestCallbackListener> listeners = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestConnector.class);
    private final Gson serializer = new Gson();

    private String bearerToken = "";


    public NibeUplinkRestConnector(OAuthClientService oAuthClient, HttpClient httpClient) {
        this.oAuthClient = oAuthClient;
        this.httpClient = httpClient;
    }

    @Override
    public List<NibeSystem> getConnectedSystems() {
        cachedSystems.clear();
        Request req = prepareRequest(HttpMethod.GET, API_SYSTEMS, 0, REQUEST_TYPE_SYSTEMS);
        String resp = makeRequest(req);
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
            throw new NibeUplinkException("No listener for system " + systemId);
        }
        if (systemTrackedParameters.add(parameterId)) {
            logger.trace("System {} is now tracking parameter {}", systemId, parameterId);
        }
    }

    @Override
    public void removeTrackedParameter(int systemId, int parameterId) {
        Set<Integer> systemTrackedParameters = trackedParameters.get(systemId);
        if (systemTrackedParameters == null || listeners.get(systemId) == null) {
            throw new NibeUplinkException("No listener for system " + systemId);
        }
        if (systemTrackedParameters.remove(parameterId)) {
            logger.trace("System {} is no longer tracking parameter {}", systemId, parameterId);
        }
    }

    @Override
    public void setParameters(int systemId, Map<Integer, Integer> parameters) {
        Request req = createSetParametersRequest(systemId, parameters);
        makeRequest(req);
    }

    @Override
    public void setMode(int systemId, Mode mode) {
        Request req = createSetModeRequest(systemId, mode);
        makeRequest(req);
    }

    @Override
    public void setThermostat(int systemId, Thermostat thermostat) {
        Request req = createSetThermostatRequest(systemId, thermostat);
        makeRequest(req);
    }

    @Override
    public SystemConfig getSystemConfig(int systemId) {
        Request req = createSystemConfigRequest(systemId);
        String resp = makeRequest(req);
        SystemConfig config = parseSystemConfig(resp);
        if (cachedSystems.get(systemId) != null) {
            cachedSystems.get(systemId).setConfig(config);
        }
        return config;
    }

    @Override
    public SoftwareInfo getSoftwareInfo(int systemId) {
        Request req = createSoftwareRequest(systemId);
        String resp = makeRequest(req);
        return parseSoftwareInfo(resp);
    }

    @Override
    public void onAccessTokenResponse(AccessTokenResponse accessTokenResponse) {
        bearerToken = accessTokenResponse.getAccessToken();
    }

    private Request createSystemRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_SYSTEM_WITH_ID, systemId, REQUEST_TYPE_SYSTEM);
    }

    private Request createSystemConfigRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_CONFIG, systemId, REQUEST_TYPE_CONFIG);
    }

    private Request createSoftwareRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_SOFTWARE, systemId, REQUEST_TYPE_SOFTWARE);
    }

    private Request createCategoriesRequest(int systemId, boolean includeParameters) {
        Request req = prepareRequest(HttpMethod.GET, API_CATEGORIES, systemId, REQUEST_TYPE_CATEGORIES);
        req.param(API_QUERY_INCLUDE_PARAMETERS, Boolean.toString(includeParameters));
        return req;
    }

    private Request createGetParametersRequest(int systemId, Set<Integer> parameterIds) {
        Request req = prepareRequest(HttpMethod.GET, API_PARAMETERS, systemId, REQUEST_TYPE_PARAMETER_GET);
        req.param(API_QUERY_PARAMETER_IDS, StringConvert.toCommaList(parameterIds));
        return req;
    }

    private Request createSetParametersRequest(int systemId, Map<Integer, Integer> parameters) {
        Request req = prepareRequest(HttpMethod.PUT, API_PARAMETERS, systemId, REQUEST_TYPE_PARAMETER_SET);
        Map<String, Map<Integer, Integer>> wrapper = Collections.singletonMap("settings", parameters);
        req.content(new StringContentProvider(serializer.toJson(wrapper)), CONTENT_TYPE);
        return req;
    }

    private Request createGetModeRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_MODE, systemId, REQUEST_TYPE_MODE_GET);
    }

    private Request createSetModeRequest(int systemId, Mode mode) {
        Request req = prepareRequest(HttpMethod.PUT, API_MODE, systemId, REQUEST_TYPE_MODE_SET);
        String body = serializer.toJson(Collections.singletonMap("mode", mode));
        req.content(new StringContentProvider(body), CONTENT_TYPE);
        return req;
    }

    private Request createSetThermostatRequest(int systemId, Thermostat thermostat) {
        Request req = prepareRequest(HttpMethod.POST, API_THERMOSTATS, systemId, REQUEST_TYPE_THERMOSTAT);
        req.content(new StringContentProvider(serializer.toJson(thermostat)), CONTENT_TYPE);
        return req;
    }

    private NibeSystem updateSystem(int systemId) {
        Request req = createSystemRequest(systemId);
        String resp = makeRequest(req);
        NibeSystem system = parseSystem(resp);
        if (cachedSystems.containsKey(systemId)) {
            system.setConfig(cachedSystems.get(systemId).getConfig());
        }
        cachedSystems.put(systemId, system);
        return system;
    }

    private SystemConfig updateSystemConfig(int systemId) {
        Request req = createSystemConfigRequest(systemId);
        String resp = makeRequest(req);
        return parseSystemConfig(resp);
    }

    private List<Category> updateCategories(int systemId, boolean includeParameters) {
        Request req = createCategoriesRequest(systemId, includeParameters);
        String resp = makeRequest(req);
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
    }

    @Override
    public void removeCallbackListener(int systemId) {
        listeners.remove(systemId);
        trackedParameters.remove(systemId);
    }

    private Request prepareRequest(HttpMethod method, String endPoint, int systemId, int requestType) {
        try {
            if (oAuthClient.getAccessTokenResponse() == null ||
                    oAuthClient.getAccessTokenResponse().isExpired(LocalDateTime.now(), 5) ||
                    bearerToken.equals("")) {
                refreshToken();
            }
        } catch (OAuthException | OAuthResponseException | IOException e) {
            throw new NibeUplinkException("Error retrieving token", e);
        }
        Request req = systemId == 0 ?
                httpClient.newRequest(endPoint) :
                httpClient.newRequest(String.format(endPoint, systemId));
        req.method(method);
        req.accept(CONTENT_TYPE);
        req.followRedirects(true);
        req.header(HttpHeader.AUTHORIZATION, BEARER + bearerToken);
        req.attribute(SYSTEM_ID, systemId);
        req.attribute(REQUEST_TYPE, requestType);
        return req;
    }

    private String makeRequest(Request req) throws NibeUplinkException {
        ContentResponse resp;
        resp = sendRequest(req);
        if (resp.getStatus() == UNAUTHORIZED_401) {
            refreshToken();
            resp = sendRequest(req);
        }
        return handleResponse(resp);
    }

    private ContentResponse sendRequest(Request req) {
        ContentResponse resp;
        try {
            resp = req.send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new NibeUplinkException("Failed to send HTTP request", e);
        }
        return resp;
    }

    private String handleResponse(ContentResponse resp) {
        switch (resp.getStatus()) {
            case OK_200:
                return resp.getContentAsString();
            case NO_CONTENT_204:
                return "";
            case BAD_REQUEST_400:
            case UNAUTHORIZED_401:
            case FORBIDDEN_403:
            case NOT_FOUND_404:
                throw new NibeUplinkException(
                        String.format("Bad request: %s, message: %s", resp.getStatus(), resp.getContentAsString()));
            case TOO_MANY_REQUESTS_429:
                throw new NibeUplinkException(
                        String.format("Rate limit exceeded: %s", resp.getContentAsString()));
            case INTERNAL_SERVER_ERROR_500:
            case NOT_IMPLEMENTED_501:
            case BAD_GATEWAY_502:
            case SERVICE_UNAVAILABLE_503:
            case GATEWAY_TIMEOUT_504:
                throw new NibeUplinkException(
                        String.format("Server error: %s, message: %s", resp.getStatus(), resp.getContentAsString()));
            default:
                throw new NibeUplinkException(String.format("Unhandled http respose: %s", resp.getStatus()));
        }
    }

    private void refreshToken() {
        try {
            bearerToken = oAuthClient.refreshToken().getAccessToken();
        } catch (OAuthException | IOException | OAuthResponseException e) {
            throw new NibeUplinkException("Unable to refresh token", e);
        }
    }
}
