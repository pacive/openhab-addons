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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.ActualFanSpeed;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.AutoMode;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.StatusFlags;

@NonNullByDefault
public class GetRunStatusCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = 0x09;

    private final StatusFlags statusFlags;
    private final ActualFanSpeed actualFanSpeed;
    private final AutoMode autoMode;

    public GetRunStatusCommand(byte[] data) throws SerialProtocolException {
        if (data.length != DEFAULT_LENGTH) {
            throw new SerialProtocolException("Data of unexpected length");
        }
        this.statusFlags = new StatusFlags(data[3]);
        this.actualFanSpeed = new ActualFanSpeed(data[4]);
        this.autoMode = new AutoMode(data[5]);
    }

    public byte getId() {
        return COMMAND_ID;
    }

    public byte[] serialize() {
        byte[] data = new byte[DEFAULT_LENGTH];
        data[0] = COMMAND_ID;
        data[3] = statusFlags.serialize()[0];
        data[4] = actualFanSpeed.serialize()[0];
        data[5] = autoMode.serialize()[0];

        return data;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public ActualFanSpeed getActualFanSpeed() {
        return actualFanSpeed;
    }

    public AutoMode getAutoMode() {
        return autoMode;
    }

    @Override
    public String toString() {
        return "GetRunStatusCommand{statusFlags=" + statusFlags + ", actualFanSpeed=" + actualFanSpeed + ", autoMode="
                + autoMode + '}';
    }
}
