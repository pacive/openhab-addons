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

import static org.openhab.binding.mitsubishiheatpump.internal.MitsubishiHeatpumpBindingConstants.PROTOCOL_IDENTIFIER;
import static org.openhab.binding.mitsubishiheatpump.internal.MitsubishiHeatpumpBindingConstants.SYNC;

import java.util.Arrays;
import java.util.HexFormat;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.util.Util;

@NonNullByDefault
public abstract class AbstractPacket {
    protected final byte[] rawData;
    protected final MitsubishiHeatpumpCommand command;

    protected AbstractPacket(byte[] data) throws SerialProtocolException {
        this.rawData = data;
        this.command = createCommand(Arrays.copyOfRange(data, 5, data.length - 1));
    }

    protected AbstractPacket(MitsubishiHeatpumpCommand command) {
        this.rawData = new byte[] {};
        this.command = command;
    }

    protected MitsubishiHeatpumpCommand createCommand(byte[] data) throws SerialProtocolException {
        return switch (data[0]) {
            case 0x02 -> new GetSettingsCommand(data);
            case 0x03 -> new GetTemperaturesCommand(data);
            case (byte) 0xc9 -> new BaseCapabilitiesCommand(data);
            default -> new GenericCommand(data);
        };
    }

    public byte[] serialize() {
        byte[] commandData = this.command.serialize();
        byte[] data = new byte[22];
        data[0] = SYNC;
        data[1] = getId();
        data[2] = PROTOCOL_IDENTIFIER[0];
        data[3] = PROTOCOL_IDENTIFIER[1];
        data[4] = (byte) commandData.length;

        System.arraycopy(commandData, 0, data, 5, commandData.length);
        data[21] = Util.calculateCheckSum(data);
        return data;
    }

    public String asHex() {
        if (rawData.length > 0) {
            return HexFormat.of().formatHex(rawData);
        } else {
            return HexFormat.of().formatHex(serialize());
        }
    }

    protected abstract byte getId();

    public MitsubishiHeatpumpCommand getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "->" + this.command + " (" + asHex() + ")";
    }
}
