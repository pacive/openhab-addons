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
package org.openhab.binding.zaptec.internal.api;

import static org.openhab.binding.zaptec.internal.ZaptecBindingConstants.API_CHARGERS;
import static org.openhab.binding.zaptec.internal.ZaptecBindingConstants.CONTENT_TYPE;

import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.zaptec.internal.ZaptecBindingConstants;
import org.openhab.binding.zaptec.internal.handler.ZaptecAccountHandler;

import com.google.gson.JsonElement;

/**
 * The {@link ZaptecBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class RequestFactory {

    public static Request createGetChargersRequest(HttpClient httpClient, ZaptecAccountHandler accountHandler) {
        return finalizeRequest(httpClient.newRequest(API_CHARGERS).method(HttpMethod.GET), accountHandler);
    }

    private static Request finalizeRequest(Request req, ZaptecAccountHandler accountHandler) {
        return req.accept(CONTENT_TYPE).header(HttpHeader.AUTHORIZATION, accountHandler.getAccessToken());
    }
}
