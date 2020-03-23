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
package org.openhab.binding.nibeuplinkrest.internal;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.auth.client.oauth2.*;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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

    private @NonNullByDefault({}) OAuthClientService oAuthService;

    private @Nullable NibeUplinkRestBridgeConfiguration config;

    public NibeUplinkRestBridgeHandler(Bridge bridge, OAuthFactory oAuthFactory, HttpClient httpClient) {
        super(bridge);
        this.oAuthFactory = oAuthFactory;
        this.httpClient = httpClient;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        //if (CHANNEL_1.equals(channelUID.getId())) {
            //if (command instanceof RefreshType) {
                // TODO: handle data refresh
            //}

            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information:
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        //}
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        config = getConfigAs(NibeUplinkRestBridgeConfiguration.class);
        oAuthService = oAuthFactory.createOAuthClientService(thing.getUID().getAsString(), TOKEN_ENDPOINT,
                AUTH_ENDPOINT, config.clientId, config.clientSecret, SCOPE, false);
        updateStatus(ThingStatus.UNKNOWN);
        logger.debug("Finished initializing!");
    }

    public void authorize(String authCode, String baseURL) throws OAuthException, OAuthResponseException, IOException {
        oAuthService.getAccessTokenResponseByAuthorizationCode(authCode, baseURL);
    }

    public String getAuthorizationUrl(String baseURL) {
        try {
            return oAuthService.getAuthorizationUrl(baseURL, SCOPE, thing.getUID().getAsString());
        } catch (OAuthException e) {
            logger.warn("Error constructing Authorization URL");
            return "";
        }
    }
}
