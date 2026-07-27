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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.openhab.binding.mitsubishiheatpump.internal.MitsubishiHeatpumpHandler;
import org.openhab.binding.mitsubishiheatpump.internal.exception.HttpProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.exception.MitsibishiHeatpumpException;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.MitsubishiHeatpumpPDU;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.PDUParser;
import org.openhab.binding.mitsubishiheatpump.internal.util.CryptoHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@NonNullByDefault
public class ResponseHandler extends BufferingResponseListener {

    private final Logger logger = LoggerFactory.getLogger(ResponseHandler.class);

    private final CryptoHelper cryptoHelper;
    private final MitsubishiHeatpumpHandler thingHandler;
    private final DocumentBuilder documentBuilder;
    private final XPath xPath;

    public ResponseHandler(CryptoHelper cryptoHelper, MitsubishiHeatpumpHandler thingHandler)
            throws HttpProtocolException {
        super();
        this.cryptoHelper = cryptoHelper;
        this.thingHandler = thingHandler;
        this.xPath = XPathFactory.newInstance().newXPath();
        try {
            this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new HttpProtocolException("Error creating response handler", e);
        }
    }

    @Override
    public synchronized void onComplete(Result result) {
        try {
            logger.trace("HTTP response: {} {}", result.getResponse().getStatus(), result.getResponse().getReason());
            if (result.isSucceeded()) {
                handleResponse(getInnerContent());
            } else {
                logger.warn("HTTP request failed");
            }
        } catch (MitsibishiHeatpumpException e) {
            logger.warn("Error during handling of response: {}", e.getMessage());
            logger.debug("", e);
        }
    }

    private Document getInnerContent() throws HttpProtocolException {
        try {
            Document response = this.documentBuilder.parse(getContentAsInputStream());
            String content = xPath.evaluate("/ESV", response);
            byte[] decrypted = cryptoHelper.decryptPayload(content);
            return this.documentBuilder.parse(new ByteArrayInputStream(decrypted));
        } catch (XPathExpressionException | IOException | SAXException e) {
            throw new HttpProtocolException("Error parsing XML data", e);
        }
    }

    private void handleResponse(Document content) throws HttpProtocolException {
        getDeviceInfo(content);
        extractPDUs(content);
    }

    private void getDeviceInfo(Document content) {
        Map<String, String> deviceInfo = new HashMap<>();
        try {
            deviceInfo.put("macAddress", xPath.evaluate("/LSV/MAC", content));
            deviceInfo.put("serialNo", xPath.evaluate("/LSV/SERIAL", content));
            deviceInfo.put("appVersion", xPath.evaluate("/LSV/APP_VER", content));
            deviceInfo.put("rssi", xPath.evaluate("/LSV/RSSI", content));
        } catch (XPathExpressionException e) {
            logger.warn("Unable to parse device info: {}", e.getMessage());
            logger.debug("", e);
        }

        thingHandler.onDeviceInfoResponse(deviceInfo);
    }

    private void extractPDUs(Document content) {
        NodeList pdus = content.getElementsByTagName("VALUE");
        for (int i = 0; i < pdus.getLength(); i++) {
            Node node = pdus.item(i);
            try {
                MitsubishiHeatpumpPDU pdu = PDUParser.parse(node.getTextContent());
                thingHandler.onPDU(pdu);
            } catch (SerialProtocolException e) {
                logger.warn("Error parsing PDU: {}", e.getMessage());
                logger.debug("", e);
            }
        }
    }
}
