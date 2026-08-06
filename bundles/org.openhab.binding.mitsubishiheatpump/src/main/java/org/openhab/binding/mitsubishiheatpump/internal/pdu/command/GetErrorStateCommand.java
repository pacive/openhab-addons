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
package org.openhab.binding.mitsubishiheatpump.internal.pdu.command;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.ErrorCode;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.ShortErrorCode;

@NonNullByDefault
public class GetErrorStateCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = 0x04;

    private final ErrorCode errorCode;
    private final ShortErrorCode shortErrorCode;

    public GetErrorStateCommand(byte[] data) throws SerialProtocolException {
        if (data.length != DEFAULT_LENGTH) {
            throw new SerialProtocolException("Data of unexpected length");
        }
        this.errorCode = new ErrorCode(Arrays.copyOfRange(data, 4, 6));
        this.shortErrorCode = new ShortErrorCode(data[6]);
    }

    public byte getId() {
        return COMMAND_ID;
    }

    public byte[] serialize() {
        byte[] data = new byte[DEFAULT_LENGTH];
        data[0] = COMMAND_ID;
        System.arraycopy(errorCode.serialize(), 0, data, 4, 2);
        data[6] = shortErrorCode.serialize()[0];

        return data;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public ShortErrorCode getShortErrorCode() {
        return shortErrorCode;
    }

    @Override
    public String toString() {
        return "GetErrorStateCommand{errorCode=" + errorCode + ", shortErrorCode=" + shortErrorCode + '}';
    }
}
