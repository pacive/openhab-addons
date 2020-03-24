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
        Request req = prepareRequest(SYSTEM, HttpMethod.GET);
        ContentResponse resp = makeRequest(req);
        return parseSystemList(resp.getContentAsString());
    }

    @Override
    public NibeSystem getSystem(int systemId) {
        Request req = prepareRequest(SYSTEM +"/" + systemId, HttpMethod.GET);
        ContentResponse resp = makeRequest(req);
        return parseSystem(resp.getContentAsString());
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

    @Override
    public void onAccessTokenResponse(AccessTokenResponse accessTokenResponse) {
        bearerToken = accessTokenResponse.getAccessToken();
    }

    private Request prepareRequest(String uri, HttpMethod method) {
        try {
            if (bearerToken.equals("") || oAuthClient.getAccessTokenResponse()
                    .isExpired(LocalDateTime.now(), 5)) {
                bearerToken = oAuthClient.refreshToken().getAccessToken();
            }
        } catch (OAuthException | OAuthResponseException | IOException e) {
            throw new NibeUplinkException("Error retreiving token", e);
        }
        Request req = httpClient.newRequest(uri);
        req.method(method);
        req.accept(ACCEPT);
        req.header("Authorization", BEARER + bearerToken);
        return req;
    }

    private ContentResponse makeRequest(Request req) throws NibeUplinkException {
        ContentResponse resp;
        try {
            resp = req.send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new NibeUplinkException("Failed to send HTTP request", e);
        }
        return resp;
    }
}
