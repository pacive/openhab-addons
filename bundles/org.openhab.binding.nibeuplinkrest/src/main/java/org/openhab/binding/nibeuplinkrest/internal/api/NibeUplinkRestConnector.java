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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.auth.client.oauth2.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestConnector implements NibeUplinkRestApi, AccessTokenRefreshListener {

    private final String ACCEPT = "application/json";
    private final String BEARER = "Bearer ";
    private final String SYSTEM = "system";

    private final OAuthClientService oAuthClient;
    private final HttpClient httpClient;
    private String bearerToken = "";
    private Map<String, Object> storedValues = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestConnector.class);

    public NibeUplinkRestConnector(OAuthClientService oAuthClient, HttpClient httpClient) {
        this.oAuthClient = oAuthClient;
        this.httpClient = httpClient;
    }

    @Override
    public List<NibeSystem> getConnectedSystems() {
        Request req = prepareRequest(SYSTEMS, HttpMethod.GET);
        String resp = makeRequest(req);
        return parseSystemList(resp);
    }

    @Override
    public NibeSystem getSystem(int systemId) {
        if (!storedValues.containsKey(SYSTEM)) {
            return updateSystem(systemId);
        }
        return (NibeSystem) storedValues.get(SYSTEM);
    }

    @Override
    public @Nullable List<Category> getCategories() {
        return null;
    }

    @Override
    public @Nullable Category getCategory(String categoryId) {
        return null;
    }

    @Override
    public @Nullable List<Parameter> getParameters(List<Integer> parameterIds) {
        return null;
    }

    @Override
    public @Nullable List<Queue> setParameters(List<Parameter> parameters) {
        return null;
    }

    @Override
    public @Nullable Mode getMode() {
        return null;
    }

    @Override
    public void setMode(Mode mode) {

    }

    @Override
    public @Nullable List<Thermostat> getThermostats() {
        return null;
    }

    @Override
    public void setThermostat(Thermostat thermostat) {

    }

    private SystemConfig getSystemConfig(int systemId) {
        if (storedValues.containsKey(SYSTEM)) {
            NibeSystem system = (NibeSystem) storedValues.get(SYSTEM);
            SystemConfig config = system.getConfig();
            if (config != null) {
                return config;
            }
        }
        return updateSystemConfig(systemId);
    }

    private SoftwareInfo getSoftwareInfo(int systemId) {
        if (storedValues.containsKey(SYSTEM)) {
            NibeSystem system = (NibeSystem) storedValues.get(SYSTEM);
            SoftwareInfo softwareInfo = system.getSoftwareInfo();
            if (softwareInfo != null) {
                return softwareInfo;
            }
        }
        return updateSoftwareVersion(systemId);
    }

    @Override
    public void onAccessTokenResponse(AccessTokenResponse accessTokenResponse) {
        bearerToken = accessTokenResponse.getAccessToken();
    }

    private NibeSystem updateSystem(int systemId) {
        logger.debug("Updating system");
        Request req = prepareRequest(String.format(SYSTEM_WITH_ID, systemId), HttpMethod.GET);
        String resp = makeRequest(req);
        NibeSystem system = parseSystem(resp);
        system.setConfig(getSystemConfig(systemId));
        system.setSoftwareInfo(getSoftwareInfo(systemId));
        storedValues.put(SYSTEM, system);
        return system;
    }

    private SystemConfig updateSystemConfig(int systemId) {
        Request req = prepareRequest(String.format(SYSTEM_CONFIG, systemId), HttpMethod.GET);
        String resp = makeRequest(req);
        return parseSystemConfig(resp);
    }

    private SoftwareInfo updateSoftwareVersion(int systemId) {
        Request req = prepareRequest(String.format(SYSTEM_SOFTWARE, systemId), HttpMethod.GET);
        String resp = makeRequest(req);
        return parseSoftwareInfo(resp);
    }

    private Request prepareRequest(String uri, HttpMethod method) {
        try {
            if (bearerToken.equals("") ||
                    oAuthClient.getAccessTokenResponse() == null ||
                    oAuthClient.getAccessTokenResponse().isExpired(LocalDateTime.now(), 5)) {
                bearerToken = oAuthClient.refreshToken().getAccessToken();
            }
        } catch (OAuthException | OAuthResponseException | IOException e) {
            throw new NibeUplinkException("Error retrieving token", e);
        }
        Request req = httpClient.newRequest(uri);
        req.method(method);
        req.accept(ACCEPT);
        req.header("Authorization", BEARER + bearerToken);
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
            case NO_CONTENT_204:
                return resp.getContentAsString();
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
