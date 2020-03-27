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
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.auth.client.oauth2.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.api.model.QueuedUpdate;
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
import java.util.stream.Collectors;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestConnector implements NibeUplinkRestApi, AccessTokenRefreshListener {

    private final String ACCEPT = "application/json";
    private final String BEARER = "Bearer ";

    private final OAuthClientService oAuthClient;
    private final HttpClient httpClient;
    private final Map<Integer, NibeSystem> cachedSystems = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Category>> cachedCategories = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, Parameter>> cachedParameters = new HashMap<>();
    private final Map<Integer, Set<Integer>> trackedParameters = new HashMap<>();
    private final Map<Integer, Mode> cachedModes = new HashMap<>();
    private final Deque<Integer> systemsQueue = new ConcurrentLinkedDeque<>();
    private final Map<Integer, Deque<Integer>> queuedParameters = new ConcurrentHashMap<>();
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
        Request req = prepareRequest(API_SYSTEMS, HttpMethod.GET);
        String resp = makeRequest(req);
        List<NibeSystem> systems = parseSystemList(resp);
        for (NibeSystem system : systems) {
            cachedSystems.put(system.getSystemId(), system);
            cachedCategories.putIfAbsent(system.getSystemId(), new HashMap<>());
            cachedParameters.putIfAbsent(system.getSystemId(), new HashMap<>());
            trackedParameters.putIfAbsent(system.getSystemId(), new HashSet<>());
        }
        return systems;
    }

    @Override
    public NibeSystem getSystem(int systemId) {
        if (cachedSystems.containsKey(systemId)) {
            return cachedSystems.get(systemId);
        }
        cachedCategories.putIfAbsent(systemId, new HashMap<>());
        cachedParameters.putIfAbsent(systemId, new HashMap<>());
        trackedParameters.putIfAbsent(systemId, new HashSet<>());
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
    public Category getCategory(int systemId, String categoryId) {
        Map<String, Category> cachedValues = cachedCategories.get(systemId);
        if (cachedValues.get(categoryId) == null) {
            updateCategories(systemId, true);
        }
        return cachedCategories.get(systemId).get(categoryId);
    }

    @Override
    public @Nullable List<Parameter> getParameters(int systemId, Set<Integer> parameterIds) {
        Set<Integer> tracked = trackedParameters.get(systemId);
        if (tracked.containsAll(parameterIds)) {
            return cachedParameters.get(systemId).entrySet().stream()
                    .filter(p -> parameterIds.contains(p.getKey()))
                    .map(p -> p.getValue()).collect(Collectors.toList());
        }

        HashSet<Integer> newParams = new HashSet(parameterIds);
        newParams.removeAll(tracked);
        tracked.addAll(newParams);
        newParams.forEach(p -> queuedParameters.get(systemId).addFirst(p));

        return cachedParameters.get(systemId).entrySet().stream()
                .filter(p -> parameterIds.contains(p.getKey()))
                .map(p -> p.getValue()).collect(Collectors.toList());
    }

    @Override
    public @Nullable List<QueuedUpdate> setParameters(int systemId, Map<Integer, Integer> parameters) {
        return null;
    }

    @Override
    public Mode getMode(int systemId) {
        if (cachedModes.get(systemId) == null) {
            return updateMode(systemId);
        }
        return cachedModes.get(systemId);
    }

    @Override
    public void setMode(int systemId, Mode mode) {
        Request req = prepareRequest(String.format(API_MODE, systemId), HttpMethod.PUT);
        String body = serializer.toJson(Collections.singletonMap("map", mode));
        req.content(new StringContentProvider(body));
        makeRequest(req);
    }

    @Override
    public @Nullable List<Thermostat> getThermostats(int systemId) {
        return null;
    }

    @Override
    public void setThermostat(int systemId, Thermostat thermostat) {

    }

    private SystemConfig getSystemConfig(int systemId) {
        NibeSystem system = cachedSystems.get(systemId);
        if (system == null) {
            system = updateSystem(systemId);
        }
        if (system.getConfig() == null) {
            return updateSystemConfig(systemId);
        }
        return system.getConfig();
    }

    private SoftwareInfo getSoftwareInfo(int systemId) {
        NibeSystem system = cachedSystems.get(systemId);
        if (system == null) {
            system = updateSystem(systemId);
        }
        if (system.getSoftwareInfo() == null) {
            return updateSoftwareVersion(systemId);
        }
        return system.getSoftwareInfo();
    }

    @Override
    public void onAccessTokenResponse(AccessTokenResponse accessTokenResponse) {
        bearerToken = accessTokenResponse.getAccessToken();
    }

    private NibeSystem updateSystem(int systemId) {
        logger.debug("Updating system");
        Request req = prepareRequest(String.format(API_SYSTEM_WITH_ID, systemId), HttpMethod.GET);
        String resp = makeRequest(req);
        NibeSystem system = parseSystem(resp);
        system.setConfig(getSystemConfig(systemId));
        system.setSoftwareInfo(getSoftwareInfo(systemId));
        cachedSystems.put(systemId, system);
        return system;
    }

    private SystemConfig updateSystemConfig(int systemId) {
        Request req = prepareRequest(String.format(API_CONFIG, systemId), HttpMethod.GET);
        String resp = makeRequest(req);
        return parseSystemConfig(resp);
    }

    private SoftwareInfo updateSoftwareVersion(int systemId) {
        Request req = prepareRequest(String.format(API_SOFTWARE, systemId), HttpMethod.GET);
        String resp = makeRequest(req);
        return parseSoftwareInfo(resp);
    }

    private List<Category> updateCategories(int systemId, boolean includeParameters) {
        Request req = prepareRequest(String.format(API_CATEGORIES, systemId), HttpMethod.GET);
        req.param(API_QUERY_INCLUDE_PARAMETERS, Boolean.toString(includeParameters));
        String resp = makeRequest(req);
        List<Category> categories = parseCategoryList(resp);
        Map<String, Category> categoryCache = cachedCategories.get(systemId);
        Map<Integer, Parameter> parameterCache = cachedParameters.get(systemId);
        for (Category category : categories) {
            categoryCache.putIfAbsent(category.getCategoryId(), category);
            for (Parameter parameter : category.getParameters()) {
                parameterCache.put(parameter.getParameterId(), parameter);
            }
        }
        return categories;
    }

    private void updateCategory(int systemId, String categoryId) {
        Request req = prepareRequest(String.format(API_CATEGORY_WITH_ID, systemId, categoryId), HttpMethod.GET);
        String resp = makeRequest(req);
        List<Parameter> parameters = parseParameterList(resp);
        Map<Integer, Parameter> parameterCache = cachedParameters.get(systemId);
        for (Parameter parameter : parameters) {
            parameterCache.put(parameter.getParameterId(), parameter);
        }
    }

    private List<Parameter> updateParameters(int systemId, Set<Integer> parameterIds) {
        Request req = prepareRequest(String.format(API_PARAMETERS, systemId), HttpMethod.GET);
        req.param(API_QUERY_PARAMETER_IDS, StringConvert.toCommaList(parameterIds));
        String resp = makeRequest(req);
        List<Parameter> parameters = parseParameterList(resp);
        Map<Integer, Parameter> parameterCache = cachedParameters.get(systemId);
        for (Parameter parameter : parameters) {
            parameterCache.put(parameter.getParameterId(), parameter);
        }
        return parameters;
    }

    private Mode updateMode(int systemId) {
        Request req = prepareRequest(String.format(API_MODE, systemId), HttpMethod.GET);
        String resp = makeRequest(req);
        Mode mode = parseMode(resp);
        cachedModes.put(systemId, mode);
        return mode;
    }

    @Override
    public void addCallbackListener(int systemId, NibeUplinkRestCallbackListener listener) {
        queuedParameters.putIfAbsent(systemId, new ConcurrentLinkedDeque<>());
        listeners.putIfAbsent(systemId, listener);
        trackedParameters.putIfAbsent(systemId, new HashSet<>());
    }

    @Override
    public void removeCallbackListener(int systemId) {
        queuedParameters.remove(systemId);
        listeners.remove(systemId);
        trackedParameters.remove(systemId);
    }

    private Request prepareRequest(String uri, HttpMethod method) {
        try {
            if (bearerToken.equals("") ||
                    oAuthClient.getAccessTokenResponse() == null ||
                    oAuthClient.getAccessTokenResponse().isExpired(LocalDateTime.now(), 5)) {
                refreshToken();
            }
        } catch (OAuthException | OAuthResponseException | IOException e) {
            throw new NibeUplinkException("Error retrieving token", e);
        }
        Request req = httpClient.newRequest(uri);
        req.method(method);
        req.accept(ACCEPT);
        req.followRedirects(true);
        req.header(HttpHeader.AUTHORIZATION, BEARER + bearerToken);
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
