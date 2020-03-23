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
package org.openhab.binding.nibeuplinkrest.internal.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBridgeHandler;
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.util.*;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

/**
 * @author Anders Alfredsson - Initial Contribution
 */
@Component(service = NibeUplinkRestOAuthService.class, immediate = true, configurationPid = "binding.nibeuplinkrest.authservice")
@NonNullByDefault
public class NibeUplinkRestOAuthService {

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestOAuthService.class);

    private @NonNullByDefault({}) BundleContext bundleContext;
    private @NonNullByDefault({}) HttpService httpService;
    private final List<NibeUplinkRestBridgeHandler> bridgeHandlers = new ArrayList<>();

    @Activate
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        try {
            this.bundleContext = componentContext.getBundleContext();
            httpService.registerServlet(SERVLET_PATH, createServlet(), new Hashtable<>(),
                    httpService.createDefaultHttpContext());
            httpService.registerResources(SERVLET_IMG_PATH, SERVLET_RESOURCE_IMG_DIR, null);
        } catch (ServletException | NamespaceException | IOException e) {
            logger.warn("Error starting OAuthService: {}", e.getMessage());
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {

    }

    private HttpServlet createServlet() throws IOException {
        Map<String, NibeUplinkRestOAuthServletTemplate> templates = new HashMap<>();
        templates.put(SERVLET_TEMPLATE_INDEX, new NibeUplinkRestOAuthServletTemplate(bundleContext,
                SERVLET_TEMPLATE_INDEX_FILE));
        templates.put(SERVLET_TEMPLATE_ACCOUNT, new NibeUplinkRestOAuthServletTemplate(bundleContext,
                SERVLET_TEMPLATE_ACCOUNT_FILE));
        return new NibeUplinkRestOAuthServlet(this, templates);
    }

    public void addBridgeHandler(NibeUplinkRestBridgeHandler handler) {
        if (!bridgeHandlers.contains(handler)) {
            bridgeHandlers.add(handler);
        }
    }

    public List<NibeUplinkRestBridgeHandler> getBridgeHandlers() {
        return bridgeHandlers;
    }

    public @Nullable NibeUplinkRestBridgeHandler getBridgeHandler(ThingUID thingUID) {
        return getBridgeHandler(thingUID.getAsString());
    }

    public @Nullable NibeUplinkRestBridgeHandler getBridgeHandler(String thingUID) {
        NibeUplinkRestBridgeHandler handler = null;
        for (NibeUplinkRestBridgeHandler h : bridgeHandlers) {
            if (h.getThing().getUID().getAsString().equals(thingUID))
                handler = h;
        }
        return handler;

    }

    @Reference
    protected void setHttpService(HttpService httpService) { this.httpService = httpService; }

    protected void unsetHttpService(HttpService httpService) { this.httpService = null; }
}
