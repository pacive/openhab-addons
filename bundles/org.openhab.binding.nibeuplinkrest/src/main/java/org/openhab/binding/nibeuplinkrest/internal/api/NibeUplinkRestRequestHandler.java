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
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.core.auth.client.oauth2.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Mode;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Thermostat;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    /**
     * Different types of requests, stored in the {@link Request} object to
     * decide what to do after the request is completed
     */
    public enum RequestType {
        SYSTEM,
        STATUS,
        PARAMETER_GET,
        SOFTWARE,
        MODE_GET,
        CONFIG,
        CATEGORIES,
        PARAMETER_SET,
        MODE_SET,
        THERMOSTAT,
        SYSTEMS,
        ALARM
    }

    private static final String CONTENT_TYPE = "application/json";
    private static final String BEARER = "Bearer ";
    public static final String REQUEST_TYPE = "requestType";
    public static final String SYSTEM_ID = "systemId";
    public static final int NO_SYSTEM_ID = -1;

    private final Gson serializer = new Gson();

    private final OAuthClientService oAuthClient;
    private final HttpClient httpClient;

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestRequestHandler.class);

    public NibeUplinkRestRequestHandler(OAuthClientService oAuthClient, HttpClient httpClient) {
        this.oAuthClient = oAuthClient;
        this.httpClient = httpClient;
    }

    /**
     * Create a request for connected systems
     * @return
     */
    public Request createConnectedSystemsRequest() {
        return prepareRequest(HttpMethod.GET, API_SYSTEMS, NO_SYSTEM_ID, RequestType.SYSTEMS);
    }

    /**
     * Create a request for a specific system
     * @param systemId
     * @return
     */
    public Request createSystemRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_SYSTEM_WITH_ID, systemId, RequestType.SYSTEM);
    }

    /**
     * Create a request for a specific system
     * @param systemId
     * @return
     */
    public Request createStatusRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_STATUS, systemId, RequestType.STATUS);
    }

    /**
     * Create a request for alarm info
     * @param systemId
     * @return
     */
    public Request createAlarmInfoRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_NOTIFICATIONS, systemId, RequestType.ALARM);
    }

    /**
     * Create a request for system configuration
     * @param systemId
     * @return
     */
    public Request createSystemConfigRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_CONFIG, systemId, RequestType.CONFIG);
    }

    /**
     * Create a request for software info
     * @param systemId
     * @return
     */
    public Request createSoftwareRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_SOFTWARE, systemId, RequestType.SOFTWARE);
    }

    /**
     * Create a request for the system's categories
     * @param systemId
     * @param includeParameters
     * @return
     */
    public Request createCategoriesRequest(int systemId, boolean includeParameters) {
        Request req = prepareRequest(HttpMethod.GET, API_CATEGORIES, systemId, RequestType.CATEGORIES);
        req.param(API_QUERY_INCLUDE_PARAMETERS, Boolean.toString(includeParameters));
        return req;
    }

    /**
     * Create a request to get parameters
     * @param systemId
     * @param parameterIds
     * @return
     */
    public Request createGetParametersRequest(int systemId, Set<Integer> parameterIds) {
        Request req = prepareRequest(HttpMethod.GET, API_PARAMETERS, systemId, RequestType.PARAMETER_GET);
        parameterIds.forEach(p -> {
            req.param(API_QUERY_PARAMETER_IDS, p.toString());
        });
        return req;
    }

    /**
     * Create a request to set parameters
     * @param systemId
     * @param parameters
     * @return
     */
    public Request createSetParametersRequest(int systemId, Map<Integer, Integer> parameters) {
        Request req = prepareRequest(HttpMethod.PUT, API_PARAMETERS, systemId, RequestType.PARAMETER_SET);
        Map<String, Map<Integer, Integer>> wrapper = Collections.singletonMap("settings", parameters);
        req.content(new StringContentProvider(serializer.toJson(wrapper)), CONTENT_TYPE);
        return req;
    }

    /**
     * Create a request to get the system's mode
     * @param systemId
     * @return
     */
    public Request createGetModeRequest(int systemId) {
        return prepareRequest(HttpMethod.GET, API_MODE, systemId, RequestType.MODE_GET);
    }

    /**
     * Create a request to set the system's mode
     * @param systemId
     * @param mode
     * @return
     */
    public Request createSetModeRequest(int systemId, Mode mode) {
        Request req = prepareRequest(HttpMethod.PUT, API_MODE, systemId, RequestType.MODE_SET);
        String body = serializer.toJson(Collections.singletonMap("mode", mode));
        req.content(new StringContentProvider(body), CONTENT_TYPE);
        return req;
    }

    /**
     * Create a request to set a thermostat
     * @param systemId
     * @param thermostat
     * @return
     */
    public Request createSetThermostatRequest(int systemId, Thermostat thermostat) {
        Request req = prepareRequest(HttpMethod.POST, API_THERMOSTATS, systemId, RequestType.THERMOSTAT);
        req.content(new StringContentProvider(serializer.toJson(thermostat)), CONTENT_TYPE);
        return req;
    }

    /**
     * Prepare the request with default values
     * @param method
     * @param endPoint
     * @param systemId
     * @param requestType
     * @return
     */
    private Request prepareRequest(HttpMethod method, String endPoint, int systemId, RequestType requestType) {
        Request req = systemId == NO_SYSTEM_ID ?
                httpClient.newRequest(endPoint) :
                httpClient.newRequest(String.format(endPoint, systemId));
        req.method(method);
        req.accept(CONTENT_TYPE);
        req.followRedirects(true);
        req.attribute(SYSTEM_ID, systemId);
        req.attribute(REQUEST_TYPE, requestType);
        return req;
    }

    /**
     * Make the request, and retry if Nibe Uplink responds with a 401
     * @param req The request to send
     * @return The body of the response as a String
     * @throws NibeUplinkRestException
     */
    public String makeRequestWithRetry(Request req) throws NibeUplinkRestException {
        ContentResponse resp;
        resp = sendRequest(req);
        if (resp.getStatus() == UNAUTHORIZED_401) {
            resp = sendRequest(req);
        }
        return handleResponse(resp);
    }

    /**
     * Add OAuth token and send the request. Catch any errors an re-throw a {@link NibeUplinkRestException}
     * @param req
     * @return
     */
    private ContentResponse sendRequest(Request req) throws NibeUplinkRestException {
        String token;
        try {
            AccessTokenResponse accessTokenResponse = oAuthClient.getAccessTokenResponse();
            if (accessTokenResponse == null) {
                throw new NibeUplinkRestException("Error getting access token response");
            }
            token = accessTokenResponse.getAccessToken();
        } catch (OAuthException | OAuthResponseException | IOException e) {
            throw new NibeUplinkRestException("Error retrieving token:" + e.getClass() + ": " + e.getMessage(), e);
        }
        req.header(HttpHeader.AUTHORIZATION, BEARER + token);

        logger.trace("Sending {} request to {} {}", req.getMethod(), req.getPath(),
                req.getContent() != null ?
                        "with data " + new String(req.getContent().iterator().next().array()) : "");
        ContentResponse resp;
        try {
            resp = req.send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new NibeUplinkRestException("Failed to send HTTP request", e);
        }
        return resp;
    }

    /**
     * Check the response code and act accordingly. Throws an exception holding the response
     * code on error, to be handled elsewhere.
     * @param resp
     * @return
     */
    private String handleResponse(ContentResponse resp) throws NibeUplinkRestHttpException {
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
}
