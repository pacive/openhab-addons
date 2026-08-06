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
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.EnhancedTemperature;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.FanMode;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.HorizontalVane;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.LegacySetpointTemperature;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.OperatingMode;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.Power;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.RemoteProhibitFlags;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.VerticalVane;

@NonNullByDefault
public class GetSettingsCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = 0x02;

    private final Power power;
    private final OperatingMode operatingMode;
    private final LegacySetpointTemperature legacySetpointTemperature;
    private final FanMode fanMode;
    private final VerticalVane verticalVane;
    private final RemoteProhibitFlags remoteProhibitFlags;
    private final HorizontalVane horizontalVane;
    private final EnhancedTemperature targetTemperature;

    public GetSettingsCommand(byte[] data) throws SerialProtocolException {
        if (data.length != DEFAULT_LENGTH) {
            throw new SerialProtocolException("Data of unexpected length");
        }
        this.power = new Power(data[3]);
        this.operatingMode = new OperatingMode(data[4]);
        this.legacySetpointTemperature = new LegacySetpointTemperature(data[5]);
        this.fanMode = new FanMode(data[6]);
        this.verticalVane = new VerticalVane(data[7]);
        this.remoteProhibitFlags = new RemoteProhibitFlags(data[8]);
        this.horizontalVane = new HorizontalVane(data[10]);
        this.targetTemperature = new EnhancedTemperature(data[11]);
    }

    public byte getId() {
        return COMMAND_ID;
    }

    public byte[] serialize() {
        byte[] data = new byte[DEFAULT_LENGTH];
        data[0] = COMMAND_ID;
        data[3] = power.serialize()[0];
        data[4] = operatingMode.serialize()[0];
        data[5] = legacySetpointTemperature.serialize()[0];
        data[6] = fanMode.serialize()[0];
        data[7] = verticalVane.serialize()[0];
        data[8] = remoteProhibitFlags.serialize()[0];
        data[10] = horizontalVane.serialize()[0];
        data[11] = targetTemperature.serialize()[0];

        return data;
    }

    public Power getPower() {
        return power;
    }

    public OperatingMode getOperatingMode() {
        return operatingMode;
    }

    public LegacySetpointTemperature getLegacySetpointTemperature() {
        return legacySetpointTemperature;
    }

    public FanMode getFanMode() {
        return fanMode;
    }

    public VerticalVane getVerticalVane() {
        return verticalVane;
    }

    public RemoteProhibitFlags getRemoteProhibitFlags() {
        return remoteProhibitFlags;
    }

    public HorizontalVane getHorizontalVane() {
        return horizontalVane;
    }

    public EnhancedTemperature getTargetTemperature() {
        return targetTemperature;
    }

    @Override
    public String toString() {
        return "GetSettingsCommand{power=" + power + ", operatingMode=" + operatingMode + ", legacySetpointTemperature="
                + legacySetpointTemperature + ", fanMode=" + fanMode + ", verticalVane=" + verticalVane
                + ", remoteProhibitFlags=" + remoteProhibitFlags + ", horizontalVane=" + horizontalVane
                + ", targetTemperature=" + targetTemperature + '}';
    }
}
