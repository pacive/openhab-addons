/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.mitsubishiheatpump.internal.http;

import static org.openhab.binding.mitsubishiheatpump.internal.MitsubishiHeatpumpBindingConstants.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.openhab.binding.mitsubishiheatpump.internal.MitsubishiHeatpumpHandler;
import org.openhab.binding.mitsubishiheatpump.internal.exception.ConfigurationException;
import org.openhab.binding.mitsubishiheatpump.internal.exception.HttpProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.MitsubishiHeatpumpPDU;
import org.openhab.binding.mitsubishiheatpump.internal.util.CryptoHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class HttpAPI {

    private final Logger logger = LoggerFactory.getLogger(HttpAPI.class);

    private final HttpClient httpClient;
    private final CryptoHelper cryptoHelper;
    private final MitsubishiHeatpumpHandler thingHandler;
    private @Nullable URI uri;

    public HttpAPI(HttpClient httpClient, CryptoHelper cryptoHelper, MitsubishiHeatpumpHandler thingHandler) {
        this.httpClient = httpClient;
        this.cryptoHelper = cryptoHelper;
        this.thingHandler = thingHandler;
    }

    public void setHost(String host) throws URISyntaxException {
        this.uri = new URI("http", host, "/smart", "");
    }

    public void requestStatus() throws ConfigurationException {
        try {
            String xml = XML_HEADER
                    + cryptoHelper.encryptPayload(XML_COMMAND_HEADER + XML_CONNECT_TAG + XML_COMMAND_FOOTER)
                    + XML_FOOTER;
            sendRequest(xml);
        } catch (HttpProtocolException e) {
            logger.warn("Error creating request");
            logger.debug("", e);
        }
    }

    public void sendPacket(MitsubishiHeatpumpPDU pdu) throws ConfigurationException {
        try {
            String xml = XML_HEADER
                    + cryptoHelper.encryptPayload(
                            XML_COMMAND_HEADER + XML_CODE_HEADER + pdu.asHex() + XML_CODE_FOOTER + XML_COMMAND_FOOTER)
                    + XML_FOOTER;
            sendRequest(xml);
        } catch (HttpProtocolException e) {
            logger.warn("Error creating request");
            logger.debug("", e);
        }
    }

    private void sendRequest(String xml) throws ConfigurationException, HttpProtocolException {
        if (this.uri == null) {
            throw new ConfigurationException("Host not set");
        }
        Request req = httpClient.POST(uri);
        ContentProvider body = new StringContentProvider(xml);
        req.content(body);
        req.send(new ResponseHandler(this.cryptoHelper, this.thingHandler));
    }
}
