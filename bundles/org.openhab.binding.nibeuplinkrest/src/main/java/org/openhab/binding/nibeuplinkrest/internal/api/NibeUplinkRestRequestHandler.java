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
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestHttpException;
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

    public enum RequestType {
        SYSTEM,
        PARAMETER_GET,
        SOFTWARE,
        MODE_GET,
        CONFIG,
        CATEGORIES,
        PARAMETER_SET,
        MODE_SET,
        THERMOSTAT,
        SYSTEMS
    }

    private static final String CONTENT_TYPE = "application/json";
    private static final String BEARER = "Bearer ";
    public static final String REQUEST_TYPE = "requestType";
    public static final String SYSTEM_ID = "systemId";
    public static final int NO_SYSTEM_ID = -1;

    private final Gson serializer = new Gson();

    private @Nullable String bearerToken;

    private final OAuthClientService oAuthClient;
    private final HttpClient httpClient;

    public NibeUplinkRestRequestHandler(OAuthClientService oAuthClient, HttpClient httpClient) {
        this.oAuthClient = oAuthClient;
        this.httpClient = httpClient;
    }

    public Request createConnectedSystemsRequest() {
        return prepareRequest(HttpMethod.GET, API_SYSTEMS, NO_SYSTEM_ID, RequestType.SYSTEMS);
    }

    public Request createSystemRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_SYSTEM_WITH_ID, systemId, RequestType.SYSTEM);
    }

    public Request createSystemConfigRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_CONFIG, systemId, RequestType.CONFIG);
    }

    public Request createSoftwareRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_SOFTWARE, systemId, RequestType.SOFTWARE);
    }

    public Request createCategoriesRequest(int systemId, boolean includeParameters) {
        Request req = prepareRequest(HttpMethod.GET, API_CATEGORIES, systemId, RequestType.CATEGORIES);
        req.param(API_QUERY_INCLUDE_PARAMETERS, Boolean.toString(includeParameters));
        return req;
    }

    public Request createGetParametersRequest(int systemId, Set<Integer> parameterIds) {
        Request req = prepareRequest(HttpMethod.GET, API_PARAMETERS, systemId, RequestType.PARAMETER_GET);
        req.param(API_QUERY_PARAMETER_IDS, StringConvert.toCommaList(parameterIds));
        return req;
    }

    public Request createSetParametersRequest(int systemId, Map<Integer, Integer> parameters) {
        Request req = prepareRequest(HttpMethod.PUT, API_PARAMETERS, systemId, RequestType.PARAMETER_SET);
        Map<String, Map<Integer, Integer>> wrapper = Collections.singletonMap("settings", parameters);
        req.content(new StringContentProvider(serializer.toJson(wrapper)), CONTENT_TYPE);
        return req;
    }

    public Request createGetModeRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_MODE, systemId, RequestType.MODE_GET);
    }

    public Request createSetModeRequest(int systemId, Mode mode) {
        Request req = prepareRequest(HttpMethod.PUT, API_MODE, systemId, RequestType.MODE_SET);
        String body = serializer.toJson(Collections.singletonMap("mode", mode));
        req.content(new StringContentProvider(body), CONTENT_TYPE);
        return req;
    }

    public Request createSetThermostatRequest(int systemId, Thermostat thermostat) {
        Request req = prepareRequest(HttpMethod.POST, API_THERMOSTATS, systemId, RequestType.THERMOSTAT);
        req.content(new StringContentProvider(serializer.toJson(thermostat)), CONTENT_TYPE);
        return req;
    }

    private Request prepareRequest(HttpMethod method, String endPoint, int systemId, RequestType requestType) {
        try {
            AccessTokenResponse response = oAuthClient.getAccessTokenResponse();
            if (response == null ||
                    response.isExpired(LocalDateTime.now(), 5) ||
                    bearerToken == null) {
                refreshToken();
            }
        } catch (OAuthException | OAuthResponseException | IOException e) {
            throw new NibeUplinkRestException("Error retrieving token", e);
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

    public String makeRequest(Request req) throws NibeUplinkRestException {
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
            throw new NibeUplinkRestException("Failed to send HTTP request", e);
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
                throw new NibeUplinkRestHttpException(
                        String.format("Bad request: %s, message: %s", resp.getStatus(), resp.getContentAsString()),
                resp.getStatus());
            case TOO_MANY_REQUESTS_429:
                throw new NibeUplinkRestHttpException(
                        String.format("Rate limit exceeded: %s", resp.getContentAsString()),
                        resp.getStatus());
            case INTERNAL_SERVER_ERROR_500:
            case NOT_IMPLEMENTED_501:
            case BAD_GATEWAY_502:
            case SERVICE_UNAVAILABLE_503:
            case GATEWAY_TIMEOUT_504:
                throw new NibeUplinkRestHttpException(
                        String.format("Server error: %s, message: %s", resp.getStatus(), resp.getContentAsString()),
                        resp.getStatus());
            default:
                throw new NibeUplinkRestHttpException(String.format("Unhandled http response: %s", resp.getStatus()),
                        resp.getStatus());
        }
    }

    private void refreshToken() {
        try {
            bearerToken = oAuthClient.refreshToken().getAccessToken();
        } catch (OAuthException | IOException | OAuthResponseException e) {
            throw new NibeUplinkRestException("Unable to refresh token", e);
        }
    }
}
