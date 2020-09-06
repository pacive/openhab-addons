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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestConnector;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Category;
import org.openhab.binding.nibeuplinkrest.internal.discovery.NibeUplinkRestDiscoveryService;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestTypeFactory;
import org.openhab.core.auth.client.oauth2.*;
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
 * The {@link NibeUplinkRestBridgeHandler} handles the connection to the Nibe Uplink API.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestBridgeHandler.class);

    private final OAuthFactory oAuthFactory;
    private final HttpClient httpClient;
    private final NibeUplinkRestTypeFactory typeFactory;

    private @NonNullByDefault({}) OAuthClientService oAuthClient;
    private @NonNullByDefault({}) NibeUplinkRestConnector nibeUplinkRestApi;
    private @NonNullByDefault({}) NibeUplinkRestBridgeConfiguration config;

    public NibeUplinkRestBridgeHandler(Bridge bridge, OAuthFactory oAuthFactory, HttpClient httpClient,
            NibeUplinkRestTypeFactory typeFactory) {
        super(bridge);
        this.oAuthFactory = oAuthFactory;
        this.httpClient = httpClient;
        this.typeFactory = typeFactory;
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
        oAuthClient = oAuthFactory.createOAuthClientService(thing.getUID().getAsString(), TOKEN_ENDPOINT, AUTH_ENDPOINT,
                config.clientId, config.clientSecret, SCOPE, false);
        nibeUplinkRestApi = new NibeUplinkRestConnector(this, oAuthClient, httpClient, scheduler, config.updateInterval,
                config.softwareUpdateCheckInterval);
        scheduler.execute(() -> {
            logger.debug("Rebuilding thing-types");
            if (isAuthorized()) {
                updateStatus(ThingStatus.ONLINE);
                nibeUplinkRestApi.getConnectedSystems().forEach(system -> {
                    try {
                        system.setConfig(nibeUplinkRestApi.getSystemConfig(system.getSystemId()));
                        List<Category> categories = nibeUplinkRestApi.getCategories(system.getSystemId(), true);
                        typeFactory.createThingType(system, categories);
                    } catch (NibeUplinkRestException e) {
                        logger.debug("Unable to build thing types: {}", e.getMessage());
                    }
                });
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                        "Waiting for OAuth authorization");
            }
        });
    }

    @Override
    public void dispose() {
        if (nibeUplinkRestApi != null) {
            nibeUplinkRestApi.cancelAllJobs();
        }
        nibeUplinkRestApi = null;
        oAuthClient.close();
        oAuthFactory.ungetOAuthService(thing.getUID().getAsString());
    }

    /**
     * Callback to authorize with Nibe uplink's OAuth endpoint after the user have authenticated.
     *
     * @param authCode
     * @param baseURL
     * @throws OAuthException
     * @throws OAuthResponseException
     * @throws IOException
     */
    public void authorize(String authCode, String baseURL) throws OAuthException, OAuthResponseException, IOException {
        oAuthClient.getAccessTokenResponseByAuthorizationCode(authCode, baseURL);
        logger.debug("Authorization successful, setting thing {} online", thing.getUID().getAsString());
        updateStatus(ThingStatus.ONLINE);
    }

    /**
     * Checks if there is a valid OAuth token
     *
     * @return true if a valid token exists
     */
    public boolean isAuthorized() {
        AccessTokenResponse token = null;
        try {
            token = oAuthClient.getAccessTokenResponse();
        } catch (OAuthException | IOException | OAuthResponseException e) {
            logger.debug("Error getting access token");
        }
        return token != null;
    }

    /**
     * Callback to construct the authorization url the user is redirected to in order to authenticate
     *
     * @param baseURL Url the user should be redirected to after authentication (i.e. url hosted by openHAB)
     * @return URL the user should access to authenticate
     */
    public String getAuthorizationUrl(String baseURL) {
        try {
            return oAuthClient.getAuthorizationUrl(baseURL, SCOPE, thing.getUID().getAsString());
        } catch (OAuthException e) {
            logger.warn("Error constructing Authorization URL");
            return "";
        }
    }

    /**
     * Get a handle to the {@link NibeUplinkRestApi} instance that handles the connection to Nibe uplink
     *
     * @return
     */
    public NibeUplinkRestApi getApiConnection() {
        return nibeUplinkRestApi;
    }

    /**
     * Callback to signal the bridge handler that there's an error with the connection, to mark it as offline
     *
     * @param responseCode The http response code returned from Nibe uplink
     */
    public void signalServerError(int responseCode) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, String.valueOf(responseCode));
    }

    /**
     * Callback to signal the bridge handler that connection has been restored, so the bridge can be set online
     */
    public void signalServerOnline() {
        updateStatus(ThingStatus.ONLINE);
    }

    /**
     * Get a handle to the {@link NibeUplinkRestTypeFactory} that handles the creation of thing types
     * 
     * @return
     */
    public NibeUplinkRestTypeFactory getTypeFactory() {
        return typeFactory;
    }
}
