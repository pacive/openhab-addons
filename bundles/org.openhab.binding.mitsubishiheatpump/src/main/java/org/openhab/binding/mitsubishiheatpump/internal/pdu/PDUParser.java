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

import static org.openhab.binding.mitsubishiheatpump.internal.MitsubishiHeatpumpBindingConstants.*;

import java.util.Arrays;
import java.util.HexFormat;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.util.Util;

@NonNullByDefault
public class PDUParser {
    public static MitsubishiHeatpumpPDU parse(String hex) throws SerialProtocolException {
        return parse(HexFormat.of().parseHex(hex));
    }

    public static MitsubishiHeatpumpPDU parse(byte[] data) throws SerialProtocolException {
        validate(data);
        return switch (data[1]) {
            case 0x41 -> new SetRequestPacket(data);
            case 0x61 -> new SetResponsePacket(data);
            case 0x42 -> new GetRequestPacket(data);
            case 0x62 -> new GetResponsePacket(data);
            default -> new GenericPacket(data);
        };
    }

    private static void validate(byte[] data) throws SerialProtocolException {
        if (data[0] != SYNC) {
            throw new SerialProtocolException("Invalid sync byte");
        }
        if (!Arrays.equals(Arrays.copyOfRange(data, 2, 4), PROTOCOL_IDENTIFIER)) {
            throw new SerialProtocolException("Invalid protocol identifier");
        }
        if (data[4] != data.length - 6) {
            throw new SerialProtocolException("Declared packet length does not match actual");
        }
        byte checkSum = Util.calculateCheckSum(Arrays.copyOf(data, data[4] + 5));
        if (data[data[4] + 5] != checkSum) {
            throw new SerialProtocolException("Invalid checksum");
        }
    }
}
