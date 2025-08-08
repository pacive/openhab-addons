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

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bouncycastle.cert.ocsp.Req;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.binding.zaptec.internal.handler.ZaptecAccountHandler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * The {@link ZaptecApiImpl} class is the concrete implementation of the ZaptecApi
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class ZaptecApiImpl implements ZaptecApi {

    private final ZaptecAccountHandler accountHandler;
    private final HttpClient httpClient;
    private static final Gson gson = new Gson();

    public ZaptecApiImpl(ZaptecAccountHandler accountHandler, HttpClient httpClient) {
        this.accountHandler = accountHandler;
        this.httpClient = httpClient;
    }

    @Override
    public void getChargers(Consumer<JsonElement> successCallback, BiConsumer<Integer, String> errorCallback) {
        sendRequest(RequestFactory.createGetChargersRequest(httpClient, accountHandler), successCallback, errorCallback);
    }

    @Override
    public JsonArray getInstallations() {
        return new JsonArray();
    }

    private void sendRequest(Request req, Consumer<JsonElement> successCallback, BiConsumer<Integer, String> errorCallback) {
        req.send(new BufferingResponseListener() {
            @Override
            public void onComplete(Result result) {
                if (result.getResponse().getStatus() == HttpStatus.OK_200) {
                    String json = getContentAsString(StandardCharsets.UTF_8);
                    JsonElement decoded = gson.fromJson(json, JsonElement.class);
                    if (decoded != null) {
                        successCallback.accept(decoded);
                    }
                } else {
                    errorCallback.accept(result.getResponse().getStatus(), getContentAsString(StandardCharsets.UTF_8));
                }
            }
        });
    }
}
