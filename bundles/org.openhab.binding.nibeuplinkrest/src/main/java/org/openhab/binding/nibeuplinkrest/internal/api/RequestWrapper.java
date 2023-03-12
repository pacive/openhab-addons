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

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class RequestWrapper {
    /**
     * Different types of requests, stored in the {@link Request} object to
     * decide what to do after the request is completed
     */
    public enum RequestType {
        SYSTEMS,
        SYSTEM,
        CONFIG,
        CATEGORIES,
        STATUS,
        SOFTWARE,
        ALARM,
        PARAMETER_GET,
        MODE_GET,
        PARAMETER_SET,
        MODE_SET,
        THERMOSTAT
    }

    private final Logger logger = LoggerFactory.getLogger(RequestWrapper.class);

    private static final String CONTENT_TYPE = "application/json";
    private static final String BEARER = "Bearer ";
    private static final int NO_SYSTEM_ID = -1;

    private final RequestType type;
    private final int systemId;
    private final Map<String, Set<?>> queryParams;
    private final @Nullable String body;

    public RequestWrapper(RequestType type) {
        this(type, NO_SYSTEM_ID);
    }

    public RequestWrapper(RequestType type, int systemId) {
        this(type, systemId, Map.of());
    }

    public RequestWrapper(RequestType type, int systemId, Map<String, Set<?>> queryParams) {
        this(type, systemId, queryParams, null);
    }

    public RequestWrapper(RequestType type, int systemId, String body) {
        this(type, systemId, Map.of(), body);
    }

    public RequestWrapper(RequestType type, int systemId, Map<String, Set<?>> queryParams, @Nullable String body) {
        this.type = type;
        this.systemId = systemId;
        this.queryParams = queryParams;
        this.body = body;
    }

    public ContentResponse send(HttpClient httpClient, OAuthClientService oAuthClient) throws NibeUplinkRestException {
        if (logger.isTraceEnabled()) {
            logger.trace("Sending {} request to {} {}", getMethod(), getURL(),
                    this.body != null ? "with data " + body : "");
        }
        Request req = httpClient.newRequest(getURL()).method(getMethod()).accept(CONTENT_TYPE).followRedirects(true)
                .timeout(5, TimeUnit.SECONDS);

        if (!queryParams.isEmpty()) {
            queryParams.forEach((key, values) -> values.forEach((value) -> req.param(key, String.valueOf(value))));
        }

        if (body != null) {
            req.content(new StringContentProvider(body), CONTENT_TYPE);
        }

        String token;
        try {
            @Nullable
            AccessTokenResponse accessTokenResponse = oAuthClient.getAccessTokenResponse();
            if (accessTokenResponse == null) {
                throw new NibeUplinkRestException("Error getting access token response");
            }
            token = accessTokenResponse.getAccessToken();
        } catch (OAuthException | OAuthResponseException | IOException e) {
            throw new NibeUplinkRestException("Error retrieving token:" + e.getClass() + ": " + e.getMessage(), e);
        }
        req.header(HttpHeader.AUTHORIZATION, BEARER + token);
        try {
            return req.send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new NibeUplinkRestException("Failed to send HTTP request", e);
        }
    }

    private HttpMethod getMethod() {
        switch (type) {
            case PARAMETER_SET:
            case MODE_SET:
                return HttpMethod.PUT;
            case THERMOSTAT:
                return HttpMethod.POST;
            default:
                return HttpMethod.GET;
        }
    }

    private String getURL() {
        switch (type) {
            case SYSTEMS:
                return API_SYSTEMS;
            case SYSTEM:
                return String.format(API_SYSTEM_WITH_ID, systemId);
            case CONFIG:
                return String.format(API_CONFIG, systemId);
            case CATEGORIES:
                return String.format(API_CATEGORIES, systemId);
            case STATUS:
                return String.format(API_STATUS, systemId);
            case SOFTWARE:
                return String.format(API_SOFTWARE, systemId);
            case ALARM:
                return String.format(API_NOTIFICATIONS, systemId);
            case PARAMETER_GET:
            case PARAMETER_SET:
                return String.format(API_PARAMETERS, systemId);
            case MODE_GET:
            case MODE_SET:
                return String.format(API_MODE, systemId);
            case THERMOSTAT:
                return String.format(API_THERMOSTATS, systemId);
            default:
                return "";
        }
    }

    public RequestType getType() {
        return type;
    }

    public int getSystemId() {
        return systemId;
    }
}
