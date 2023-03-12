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

import static org.eclipse.jetty.http.HttpStatus.*;
import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;
import static org.openhab.binding.nibeuplinkrest.internal.api.RequestWrapper.*;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Mode;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Thermostat;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestHttpException;
import org.openhab.core.auth.client.oauth2.OAuthClientService;

import com.google.gson.Gson;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestRequestHandler {

    private final Gson serializer = new Gson();

    private final OAuthClientService oAuthClient;
    private final HttpClient httpClient;

    public NibeUplinkRestRequestHandler(OAuthClientService oAuthClient, HttpClient httpClient) {
        this.oAuthClient = oAuthClient;
        this.httpClient = httpClient;
    }

    /**
     * Create a request for connected systems
     * 
     * @return
     */
    public RequestWrapper createConnectedSystemsRequest() {
        return new RequestWrapper(RequestType.SYSTEMS);
    }

    /**
     * Create a request for a specific system
     * 
     * @param systemId
     * @return
     */
    public RequestWrapper createSystemRequest(int systemId) {
        return new RequestWrapper(RequestType.SYSTEM, systemId);
    }

    /**
     * Create a request for a specific system
     * 
     * @param systemId
     * @return
     */
    public RequestWrapper createStatusRequest(int systemId) {
        return new RequestWrapper(RequestType.STATUS, systemId);
    }

    /**
     * Create a request for alarm info
     * 
     * @param systemId
     * @return
     */
    public RequestWrapper createAlarmInfoRequest(int systemId) {
        return new RequestWrapper(RequestType.ALARM, systemId);
    }

    /**
     * Create a request for system configuration
     * 
     * @param systemId
     * @return
     */
    public RequestWrapper createSystemConfigRequest(int systemId) {
        return new RequestWrapper(RequestType.CONFIG, systemId);
    }

    /**
     * Create a request for software info
     * 
     * @param systemId
     * @return
     */
    public RequestWrapper createSoftwareRequest(int systemId) {
        return new RequestWrapper(RequestType.SOFTWARE, systemId);
    }

    /**
     * Create a request for the system's categories
     * 
     * @param systemId
     * @param includeParameters
     * @return
     */
    public RequestWrapper createCategoriesRequest(int systemId, boolean includeParameters) {
        return new RequestWrapper(RequestType.CATEGORIES, systemId,
                Map.of(API_QUERY_INCLUDE_PARAMETERS, Set.of(includeParameters)));
    }

    /**
     * Create a request to get parameters
     * 
     * @param systemId
     * @param parameterIds
     * @return
     */
    public RequestWrapper createGetParametersRequest(int systemId, Set<Integer> parameterIds) {
        return new RequestWrapper(RequestType.PARAMETER_GET, systemId,
                Map.of(API_QUERY_PARAMETER_IDS, Set.copyOf(parameterIds)));
    }

    /**
     * Create a request to set parameters
     * 
     * @param systemId
     * @param parameters
     * @return
     */
    public RequestWrapper createSetParametersRequest(int systemId, Map<Integer, Integer> parameters) {
        Map<String, Map<Integer, Integer>> wrapper = Map.of("settings", parameters);
        return new RequestWrapper(RequestType.PARAMETER_SET, systemId, serializer.toJson(wrapper));
    }

    /**
     * Create a request to get the system's mode
     * 
     * @param systemId
     * @return
     */
    public RequestWrapper createGetModeRequest(int systemId) {
        return new RequestWrapper(RequestType.MODE_GET, systemId);
    }

    /**
     * Create a request to set the system's mode
     * 
     * @param systemId
     * @param mode
     * @return
     */
    public RequestWrapper createSetModeRequest(int systemId, Mode mode) {
        return new RequestWrapper(RequestType.MODE_SET, systemId, serializer.toJson(Map.of("mode", mode)));
    }

    /**
     * Create a request to set a thermostat
     * 
     * @param systemId
     * @param thermostat
     * @return
     */
    public RequestWrapper createSetThermostatRequest(int systemId, Thermostat thermostat) {
        return new RequestWrapper(RequestType.THERMOSTAT, systemId, serializer.toJson(thermostat));
    }

    /**
     * Make the request, and retry if Nibe Uplink responds with a 401
     * 
     * @param req The request to send
     * @return The body of the response as a String
     * @throws NibeUplinkRestException
     */
    public String makeRequestWithRetry(RequestWrapper req) throws NibeUplinkRestException {
        ContentResponse resp;
        resp = req.send(httpClient, oAuthClient);
        if (resp.getStatus() == UNAUTHORIZED_401) {
            resp = req.send(httpClient, oAuthClient);
        }
        return handleResponse(resp);
    }

    /**
     * Check the response code and act accordingly. Throws an exception holding the response
     * code on error, to be handled elsewhere.
     * 
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
                        String.format("Rate limit exceeded: %s", resp.getContentAsString()), resp.getStatus());
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
