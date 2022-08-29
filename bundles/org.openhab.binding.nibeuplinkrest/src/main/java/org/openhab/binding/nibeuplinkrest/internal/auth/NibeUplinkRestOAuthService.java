/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.nibeuplinkrest.internal.auth;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBridgeHandler;
import org.openhab.core.thing.ThingUID;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Anders Alfredsson - Initial Contribution
 */
@Component(service = NibeUplinkRestOAuthService.class, immediate = true, configurationPid = "binding.nibeuplinkrest.authservice")
@NonNullByDefault
public class NibeUplinkRestOAuthService {

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestOAuthService.class);

    private final BundleContext bundleContext;
    private final HttpService httpService;
    private final Map<String, NibeUplinkRestBridgeHandler> bridgeHandlers = new HashMap<>();

    @Activate
    public NibeUplinkRestOAuthService(ComponentContext componentContext, Map<String, Object> properties,
            @Reference HttpService httpService) {
        this.bundleContext = componentContext.getBundleContext();
        this.httpService = httpService;
        try {
            this.httpService.registerServlet(SERVLET_PATH, createServlet(), new Hashtable<>(),
                    this.httpService.createDefaultHttpContext());
            this.httpService.registerResources(SERVLET_IMG_PATH, SERVLET_RESOURCE_IMG_DIR, null);
        } catch (ServletException | NamespaceException | IOException e) {
            logger.warn("Error starting OAuthService: {}", e.getMessage());
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        httpService.unregister(SERVLET_PATH);
        httpService.unregister(SERVLET_IMG_PATH);
    }

    /**
     * Create a servlet to enable OAuth flow
     * 
     * @return
     * @throws IOException
     */
    private HttpServlet createServlet() throws IOException {
        Map<String, NibeUplinkRestOAuthServletTemplate> templates = new HashMap<>();
        templates.put(SERVLET_TEMPLATE_INDEX,
                new NibeUplinkRestOAuthServletTemplate(bundleContext, SERVLET_TEMPLATE_INDEX_FILE));
        templates.put(SERVLET_TEMPLATE_ACCOUNT,
                new NibeUplinkRestOAuthServletTemplate(bundleContext, SERVLET_TEMPLATE_ACCOUNT_FILE));
        return new NibeUplinkRestOAuthServlet(this, templates);
    }

    /**
     * Inject a bridge handler to make callbacks to
     * 
     * @param handler
     */
    public void addBridgeHandler(NibeUplinkRestBridgeHandler handler) {
        bridgeHandlers.put(handler.getThing().getUID().getAsString(), handler);
    }

    /**
     * Get all bridge handlers using the service
     * 
     * @return
     */
    public Map<String, NibeUplinkRestBridgeHandler> getBridgeHandlers() {
        return bridgeHandlers;
    }

    /**
     * Get a speceific bridge handler
     * 
     * @param thingUID
     * @return
     */
    public @Nullable NibeUplinkRestBridgeHandler getBridgeHandler(ThingUID thingUID) {
        return getBridgeHandler(thingUID.getAsString());
    }

    /**
     * Get a specific bridge handler
     * 
     * @param thingUID
     * @return
     */
    public @Nullable NibeUplinkRestBridgeHandler getBridgeHandler(String thingUID) {
        return bridgeHandlers.get(thingUID);
    }
}
