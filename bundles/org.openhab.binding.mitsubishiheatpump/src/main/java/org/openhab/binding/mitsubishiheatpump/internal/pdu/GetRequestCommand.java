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
package org.openhab.binding.mitsubishiheatpump.internal.pdu;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class GetRequestCommand implements MitsubishiHeatpumpCommand {
    public static final GetRequestCommand GET_SETTINGS = new GetRequestCommand((byte) 0x02);
    public static final GetRequestCommand GET_TEMPERATURES = new GetRequestCommand((byte) 0x03);
    public static final GetRequestCommand GET_ERROR_STATE = new GetRequestCommand((byte) 0x04);
    public static final GetRequestCommand GET_TIMER_INFO = new GetRequestCommand((byte) 0x05);
    public static final GetRequestCommand GET_OPERATION_STATE = new GetRequestCommand((byte) 0x06);
    public static final GetRequestCommand GET_RUN_STATUS = new GetRequestCommand((byte) 0x09);

    private final byte commandId;

    public GetRequestCommand(byte commandId) {
        this.commandId = commandId;
    }

    @Override
    public byte[] serialize() {
        return new byte[] { this.commandId };
    }

    @Override
    public byte getId() {
        return commandId;
    }
}
