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

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthException;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestOAuthServlet extends HttpServlet {

    private static final long serialVersionUID = 985091377740195L;

    private final String REPLACE_CALLBACK = "callbackURL";
    private final String REPLACE_ACCOUNTS = "accounts";
    private final String REPLACE_ACCOUNT_LABEL = "account.thingLabel";
    private final String REPLACE_ACCOUNT_REDIRECT = "account.redirectURI";

    private final String CONTENT_TYPE = "text/html";

    private NibeUplinkRestOAuthService oAuthService;
    private final Map<String, NibeUplinkRestOAuthServletTemplate> templates;

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestOAuthServlet.class);

    public NibeUplinkRestOAuthServlet(NibeUplinkRestOAuthService oAuthService,
                                      Map<String, NibeUplinkRestOAuthServletTemplate> templates) {
        this.oAuthService = oAuthService;
        this.templates = templates;
    }

    @Override
    protected void doGet(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp)
            throws ServletException, IOException {
        if (req == null || resp == null) { throw new ServletException(); }

        logger.debug("Received GET request: {}", req.getRequestURI());
        final String servletBaseURL = req.getRequestURL().toString();
        final NibeUplinkRestOAuthServletTemplate indexTemplate = templates.get(SERVLET_TEMPLATE_INDEX);

        handleCallback(req.getQueryString(), servletBaseURL);
        resp.setContentType(CONTENT_TYPE);
        indexTemplate.addReplacement(REPLACE_CALLBACK, servletBaseURL);
        indexTemplate.addReplacement(REPLACE_ACCOUNTS, formatAccounts(servletBaseURL));
        resp.getWriter().print(indexTemplate.replaceAll());
        resp.getWriter().close();
    }

    private String formatAccounts(String baseURL) {
        StringBuilder accounts = new StringBuilder();
        NibeUplinkRestOAuthServletTemplate template = templates.get(SERVLET_TEMPLATE_ACCOUNT);
        oAuthService.getBridgeHandlers().forEach(h -> {
            String authURL = h.getAuthorizationUrl(baseURL);
            template.addReplacement(REPLACE_ACCOUNT_LABEL, h.getThing().getLabel());
            template.addReplacement(REPLACE_ACCOUNT_REDIRECT, authURL);
            accounts.append(template.replaceAll());
        });
        return accounts.toString();
    }

    private void handleCallback(@Nullable String queryString, String baseURL) {
        if (queryString == null) {
            return;
        }

        final MultiMap<String> params = new MultiMap<>();
        UrlEncoded.decodeTo(queryString, params, StandardCharsets.UTF_8);
        NibeUplinkRestBridgeHandler handler = oAuthService.getBridgeHandler(params.getString("state"));
        if (handler == null) {
            logger.warn("No handler for thing {}", params.getString("state"));
        } else {
            try {
                handler.authorize(params.getString("code"), baseURL);
            } catch (OAuthException | OAuthResponseException | IOException e) {
                logger.warn("Error authorizing thing: {}", e.getMessage());
            }
        }
    }
}
