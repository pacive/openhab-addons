/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.zaptec.internal.handler;

import static org.openhab.binding.zaptec.internal.ZaptecBindingConstants.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.zaptec.internal.ZaptecConfiguration;
import org.openhab.binding.zaptec.internal.api.ZaptecApi;
import org.openhab.binding.zaptec.internal.api.ZaptecApiImpl;
import org.openhab.binding.zaptec.internal.discovery.ZaptecDiscoveryService;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZaptecAccountHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class ZaptecAccountHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(ZaptecAccountHandler.class);
    private final HttpClient httpClient;
    private final OAuthFactory oAuthFactory;
    private final ZaptecApiImpl api;
    private @Nullable OAuthClientService oAuthClientService;

    public ZaptecAccountHandler(Bridge bridge, HttpClient httpClient, OAuthFactory oAuthFactory) {
        super(bridge);
        this.httpClient = httpClient;
        this.oAuthFactory = oAuthFactory;
        this.api = new ZaptecApiImpl(this, httpClient);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(ZaptecDiscoveryService.class);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        oAuthClientService = oAuthFactory.createOAuthClientService(thing.getUID().getAsString(), TOKEN_ENDPOINT, null,
                "", null, SCOPE, false);

        updateStatus(ThingStatus.UNKNOWN);

        scheduler.execute(this::authorize);

        // These logging types should be primarily used by bindings
        // logger.trace("Example trace message");
        // logger.debug("Example debug message");
        // logger.warn("Example warn message");
        //
        // Logging to INFO should be avoided normally.
        // See https://www.openhab.org/docs/developer/guidelines.html#f-logging

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    private void authorize() {
        AccessTokenResponse token;
        OAuthClientService oAuthClientService = this.oAuthClientService;
        if (oAuthClientService != null) {
            try {
                token = oAuthClientService.getAccessTokenResponse();
                if (token == null) {
                    logger.debug("No existing refresh token, trying fresh login");
                    ZaptecConfiguration config = getConfigAs(ZaptecConfiguration.class);
                    String user = config.getUsername();
                    String password = config.getPassword();
                    token = oAuthClientService.getAccessTokenByResourceOwnerPasswordCredentials(user, password, SCOPE);
                }
                updateStatus(ThingStatus.ONLINE);
            } catch (OAuthException | IOException | OAuthResponseException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
                logger.warn("Error communicating with server, retry in 60 s");
                scheduler.schedule(this::authorize, 60, TimeUnit.SECONDS);
            }
        } else {
            logger.error("OAuthClientService not available");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR);
        }
    }

    public @Nullable String getAccessToken() {
        OAuthClientService oAuthClientService = this.oAuthClientService;
        if (oAuthClientService != null) {
            try {
                AccessTokenResponse token = oAuthClientService.getAccessTokenResponse();
                if (token != null) {
                    return token.getTokenType() + " " + token.getAccessToken();
                }
            } catch (OAuthException | IOException | OAuthResponseException e) {
                return null;
            }
        }
        return null;
    }

    public ZaptecApi getApi() {
        return this.api;
    }
}
