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
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.types.EnhancedTemperature;
import org.openhab.binding.mitsubishiheatpump.internal.types.LegacyRoomTemperature;
import org.openhab.binding.mitsubishiheatpump.internal.types.RemoteTempSetting;

@NonNullByDefault
public class SetRemoteTemperatureCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = 0x07;

    private RemoteTempSetting remoteTempSetting;
    private LegacyRoomTemperature legacyRoomTemperature;
    private EnhancedTemperature enhancedTemperature;

    public SetRemoteTemperatureCommand() {
        this.remoteTempSetting = RemoteTempSetting.INTERNAL;
        try {
            this.legacyRoomTemperature = new LegacyRoomTemperature(0);
            this.enhancedTemperature = new EnhancedTemperature(0);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public SetRemoteTemperatureCommand(byte[] data) throws SerialProtocolException {
        if (data.length != DEFAULT_LENGTH) {
            throw new SerialProtocolException("Data of unexpected length");
        }
        this.remoteTempSetting = new RemoteTempSetting(data[1]);
        this.legacyRoomTemperature = new LegacyRoomTemperature(data[2]);
        this.enhancedTemperature = new EnhancedTemperature(data[3]);
    }

    public byte getId() {
        return COMMAND_ID;
    }

    public byte[] serialize() {
        byte[] data = new byte[DEFAULT_LENGTH];
        data[0] = COMMAND_ID;
        data[1] = remoteTempSetting.serialize()[0];
        data[2] = legacyRoomTemperature.serialize()[0];
        data[3] = enhancedTemperature.serialize()[0];

        return data;
    }

    public void setLegacyRoomTemperature(LegacyRoomTemperature legacyRoomTemperature) {
        this.legacyRoomTemperature = legacyRoomTemperature;
        this.remoteTempSetting = RemoteTempSetting.EXTERNAL;
    }

    public void setEnhancedTemperature(EnhancedTemperature enhancedTemperature) {
        this.enhancedTemperature = enhancedTemperature;
        this.remoteTempSetting = RemoteTempSetting.EXTERNAL;
    }
}
