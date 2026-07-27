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
import org.openhab.binding.mitsubishiheatpump.internal.types.LegacyTSRoomTemperature;
import org.openhab.binding.mitsubishiheatpump.internal.types.RemoteTempSetting;

@NonNullByDefault
public class SetRemoteTemperatureCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = 0x07;

    private RemoteTempSetting remoteTempSetting;
    private LegacyTSRoomTemperature legacyTSRoomTemperature;
    private EnhancedTemperature enhancedTemperature;

    public SetRemoteTemperatureCommand() {
        this.remoteTempSetting = RemoteTempSetting.INTERNAL;
        try {
            this.legacyTSRoomTemperature = new LegacyTSRoomTemperature(0);
            this.enhancedTemperature = new EnhancedTemperature(0);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public byte getId() {
        return COMMAND_ID;
    }

    public byte[] serialize() {
        byte[] data = new byte[DEFAULT_LENGTH];
        data[0] = COMMAND_ID;
        data[1] = remoteTempSetting.serialize()[0];
        data[2] = legacyTSRoomTemperature.serialize()[0];
        data[3] = enhancedTemperature.serialize()[0];

        return data;
    }

    public void setLegacyRoomTemperature(LegacyTSRoomTemperature legacyTSRoomTemperature) {
        this.legacyTSRoomTemperature = legacyTSRoomTemperature;
        this.remoteTempSetting = RemoteTempSetting.EXTERNAL;
    }

    public void setEnhancedTemperature(EnhancedTemperature enhancedTemperature) {
        this.enhancedTemperature = enhancedTemperature;
        this.remoteTempSetting = RemoteTempSetting.EXTERNAL;
    }

    @Override
    public String toString() {
        return "SetRemoteTemperatureCommand{remoteTempSetting=" + remoteTempSetting + ", legacyRoomTemperature="
                + legacyTSRoomTemperature + ", enhancedTemperature=" + enhancedTemperature + '}';
    }
}
