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
package org.openhab.binding.nibeuplinkrest.internal.handler;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.auth.client.oauth2.*;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestConnector;
import org.openhab.binding.nibeuplinkrest.internal.discovery.NibeUplinkRestDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * The {@link NibeUplinkRestBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestBridgeHandler.class);

    private final OAuthFactory oAuthFactory;
    private final HttpClient httpClient;

    private @NonNullByDefault({}) OAuthClientService oAuthClient;
    private @NonNullByDefault({}) NibeUplinkRestApi nibeUplinkRestApi;
    private @NonNullByDefault({}) NibeUplinkRestBridgeConfiguration config;

    public NibeUplinkRestBridgeHandler(Bridge bridge, OAuthFactory oAuthFactory, HttpClient httpClient) {
        super(bridge);
        this.oAuthFactory = oAuthFactory;
        this.httpClient = httpClient;
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(NibeUplinkRestDiscoveryService.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @Override
    public void initialize() {
        logger.debug("Initializing Rest Api Bridge");
        config = getConfigAs(NibeUplinkRestBridgeConfiguration.class);
        oAuthClient = oAuthFactory.createOAuthClientService(thing.getUID().getAsString(), TOKEN_ENDPOINT,
                AUTH_ENDPOINT, config.clientId, config.clientSecret, SCOPE, false);
        nibeUplinkRestApi = new NibeUplinkRestConnector(oAuthClient, httpClient);
        oAuthClient.addAccessTokenRefreshListener((NibeUplinkRestConnector) nibeUplinkRestApi);
        updateStatus(ThingStatus.UNKNOWN);
        scheduler.execute(() -> {
            if (isAuthorized()) {
                updateStatus(ThingStatus.ONLINE);
            }
        });
    }

    @Override
    public void dispose() {
        oAuthClient.removeAccessTokenRefreshListener((NibeUplinkRestConnector) nibeUplinkRestApi);
    }

    public void authorize(String authCode, String baseURL) throws OAuthException, OAuthResponseException, IOException {
        oAuthClient.getAccessTokenResponseByAuthorizationCode(authCode, baseURL);
        logger.debug("Authorization successful, setting thing {} online", thing.getUID().getAsString());
        updateStatus(ThingStatus.ONLINE);
    }

    public boolean isAuthorized() {
        AccessTokenResponse token = null;
        try {
            token = oAuthClient.getAccessTokenResponse();
        } catch (OAuthException | IOException | OAuthResponseException e) {
            logger.debug("Error getting access token");
        }
        return token != null;
    }

    public String getAuthorizationUrl(String baseURL) {
        try {
            return oAuthClient.getAuthorizationUrl(baseURL, SCOPE, thing.getUID().getAsString());
        } catch (OAuthException e) {
            logger.warn("Error constructing Authorization URL");
            return "";
        }
    }

    public NibeUplinkRestApi getApiConnection() {
        return nibeUplinkRestApi;
    }
}
