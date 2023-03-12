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
package org.openhab.binding.nibeuplinkrest.internal.auth;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBridgeHandler;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestOAuthServlet extends HttpServlet {

    private static final long serialVersionUID = 985091377740195L;

    private static final String REPLACE_CALLBACK = "callbackURL";
    private static final String REPLACE_ACCOUNTS = "accounts";
    private static final String REPLACE_ACCOUNT_LABEL = "account.thingLabel";
    private static final String REPLACE_ACCOUNT_REDIRECT = "account.redirectURI";
    private static final String REPLACE_ACCOUNT_AUTHORIZED = "account.authorized";

    private static final String CONTENT_TYPE = "text/html";

    private final NibeUplinkRestOAuthService oAuthService;
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
        if (req == null || resp == null) {
            throw new ServletException();
        }

        logger.debug("Received GET request: {}", req.getRequestURI());

        String servletBaseURL = Optional.ofNullable(req.getRequestURL()).orElseThrow(ServletException::new).toString();
        NibeUplinkRestOAuthServletTemplate indexTemplate = templates.get(SERVLET_TEMPLATE_INDEX);

        if (indexTemplate == null) {
            throw new ServletException();
        }

        handleCallback(req.getQueryString(), servletBaseURL);
        resp.setContentType(CONTENT_TYPE);
        indexTemplate.addReplacement(REPLACE_CALLBACK, servletBaseURL);
        indexTemplate.addReplacement(REPLACE_ACCOUNTS, formatAccounts(servletBaseURL));
        resp.getWriter().print(indexTemplate.replaceAll());
        resp.getWriter().close();
    }

    /**
     * Adds a list of available accounts to the response, with links for authentication
     * 
     * @param baseURL
     * @return
     */
    private String formatAccounts(String baseURL) throws ServletException {
        StringBuilder accounts = new StringBuilder();
        NibeUplinkRestOAuthServletTemplate template = templates.get(SERVLET_TEMPLATE_ACCOUNT);

        if (template == null) {
            throw new ServletException();
        }

        oAuthService.getBridgeHandlers().values().forEach(handler -> {
            String authURL = handler.getAuthorizationUrl(baseURL);
            template.addReplacement(REPLACE_ACCOUNT_LABEL, handler.getThing().getLabel());
            template.addReplacement(REPLACE_ACCOUNT_REDIRECT, authURL);
            template.addReplacement(REPLACE_ACCOUNT_AUTHORIZED,
                    handler.isAuthorized() ? "Authorized" : "Not authorized");
            accounts.append(template.replaceAll());
        });
        return accounts.toString();
    }

    /**
     * Handle requests to the servlet
     * 
     * @param queryString
     * @param baseURL
     */
    private void handleCallback(@Nullable String queryString, String baseURL) {
        if (queryString == null) {
            return;
        }

        final MultiMap<String> params = new MultiMap<>();
        UrlEncoded.decodeTo(queryString, params, StandardCharsets.UTF_8);
        @Nullable
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
