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

import com.google.gson.Gson;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.smarthome.core.auth.client.oauth2.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Mode;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Thermostat;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkException;
import org.openhab.binding.nibeuplinkrest.internal.util.StringConvert;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.eclipse.jetty.http.HttpStatus.*;
import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestRequestHandler {

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
    private static final int NO_SYSTEM_ID = -1;

    private final Gson serializer = new Gson();

    private @Nullable String bearerToken;

    private final OAuthClientService oAuthClient;
    private final HttpClient httpClient;

    public NibeUplinkRestRequestHandler(OAuthClientService oAuthClient, HttpClient httpClient) {
        this.oAuthClient = oAuthClient;
        this.httpClient = httpClient;
    }

    public Request createConnectedSystemsRequest() {
        return prepareRequest(HttpMethod.GET, API_SYSTEMS, NO_SYSTEM_ID, REQUEST_TYPE_SYSTEMS);
    }

    public Request createSystemRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_SYSTEM_WITH_ID, systemId, REQUEST_TYPE_SYSTEM);
    }

    public Request createSystemConfigRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_CONFIG, systemId, REQUEST_TYPE_CONFIG);
    }

    public Request createSoftwareRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_SOFTWARE, systemId, REQUEST_TYPE_SOFTWARE);
    }

    public Request createCategoriesRequest(int systemId, boolean includeParameters) {
        Request req = prepareRequest(HttpMethod.GET, API_CATEGORIES, systemId, REQUEST_TYPE_CATEGORIES);
        req.param(API_QUERY_INCLUDE_PARAMETERS, Boolean.toString(includeParameters));
        return req;
    }

    public Request createGetParametersRequest(int systemId, Set<Integer> parameterIds) {
        Request req = prepareRequest(HttpMethod.GET, API_PARAMETERS, systemId, REQUEST_TYPE_PARAMETER_GET);
        req.param(API_QUERY_PARAMETER_IDS, StringConvert.toCommaList(parameterIds));
        return req;
    }

    public Request createSetParametersRequest(int systemId, Map<Integer, Integer> parameters) {
        Request req = prepareRequest(HttpMethod.PUT, API_PARAMETERS, systemId, REQUEST_TYPE_PARAMETER_SET);
        Map<String, Map<Integer, Integer>> wrapper = Collections.singletonMap("settings", parameters);
        req.content(new StringContentProvider(serializer.toJson(wrapper)), CONTENT_TYPE);
        return req;
    }

    public Request createGetModeRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_MODE, systemId, REQUEST_TYPE_MODE_GET);
    }

    public Request createSetModeRequest(int systemId, Mode mode) {
        Request req = prepareRequest(HttpMethod.PUT, API_MODE, systemId, REQUEST_TYPE_MODE_SET);
        String body = serializer.toJson(Collections.singletonMap("mode", mode));
        req.content(new StringContentProvider(body), CONTENT_TYPE);
        return req;
    }

    public Request createSetThermostatRequest(int systemId, Thermostat thermostat) {
        Request req = prepareRequest(HttpMethod.POST, API_THERMOSTATS, systemId, REQUEST_TYPE_THERMOSTAT);
        req.content(new StringContentProvider(serializer.toJson(thermostat)), CONTENT_TYPE);
        return req;
    }

    private Request prepareRequest(HttpMethod method, String endPoint, int systemId, int requestType) {
        try {
            if (oAuthClient.getAccessTokenResponse() == null ||
                    oAuthClient.getAccessTokenResponse().isExpired(LocalDateTime.now(), 5) ||
                    bearerToken == null) {
                refreshToken();
            }
        } catch (OAuthException | OAuthResponseException | IOException e) {
            throw new NibeUplinkException("Error retrieving token", e);
        }
        Request req = systemId == NO_SYSTEM_ID ?
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

    public String makeRequest(Request req) throws NibeUplinkException {
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
                throw new NibeUplinkException(String.format("Unhandled http response: %s", resp.getStatus()));
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
