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
import org.openhab.binding.mitsubishiheatpump.internal.types.FanMode;
import org.openhab.binding.mitsubishiheatpump.internal.types.HorizontalVane;
import org.openhab.binding.mitsubishiheatpump.internal.types.LegacySetpointTemperature;
import org.openhab.binding.mitsubishiheatpump.internal.types.OperatingMode;
import org.openhab.binding.mitsubishiheatpump.internal.types.Power;
import org.openhab.binding.mitsubishiheatpump.internal.types.RemoteProhibitFlags;
import org.openhab.binding.mitsubishiheatpump.internal.types.UpdateFlags;
import org.openhab.binding.mitsubishiheatpump.internal.types.VerticalVane;

@NonNullByDefault
public class SetSettingsCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = 0x01;

    private final UpdateFlags updateFlags;
    private Power power;
    private OperatingMode operatingMode;
    private LegacySetpointTemperature legacySetpointTemperature;
    private FanMode fanMode;
    private VerticalVane verticalVane;
    private RemoteProhibitFlags remoteProhibitFlags;
    private HorizontalVane horizontalVane;
    private EnhancedTemperature targetTemperature;

    public SetSettingsCommand() {
        try {
            this.updateFlags = new UpdateFlags(0, 2);
            this.power = Power.OFF;
            this.operatingMode = OperatingMode.HEAT;
            this.legacySetpointTemperature = new LegacySetpointTemperature(0);
            this.fanMode = FanMode.AUTO;
            this.verticalVane = VerticalVane.AUTO;
            this.remoteProhibitFlags = RemoteProhibitFlags.UNLOCKED;
            this.horizontalVane = HorizontalVane.AUTO;
            this.targetTemperature = new EnhancedTemperature(0);
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
        System.arraycopy(updateFlags.serialize(), 0, data, 1, 2);
        data[3] = power.serialize()[0];
        data[4] = operatingMode.serialize()[0];
        data[5] = legacySetpointTemperature.serialize()[0];
        data[6] = fanMode.serialize()[0];
        data[7] = verticalVane.serialize()[0];
        data[11] = remoteProhibitFlags.serialize()[0];
        data[13] = horizontalVane.serialize()[0];
        data[14] = targetTemperature.serialize()[0];

        return data;
    }

    public void setPower(Power power) {
        this.power = power;
        this.updateFlags.withFlags(UpdateFlags.UPDATE_POWER);
    }

    public void setOperatingMode(OperatingMode operatingMode) {
        this.operatingMode = operatingMode;
        this.updateFlags.withFlags(UpdateFlags.UPDATE_MODE);
    }

    public void setLegacySetpointTemperature(LegacySetpointTemperature legacySetpointTemperature) {
        this.legacySetpointTemperature = legacySetpointTemperature;
        this.updateFlags.withFlags(UpdateFlags.UPDATE_TEMP);
    }

    public void setFanMode(FanMode fanMode) {
        this.fanMode = fanMode;
        this.updateFlags.withFlags(UpdateFlags.UPDATE_FAN_MODE);
    }

    public void setVerticalVane(VerticalVane verticalVane) {
        this.verticalVane = verticalVane;
        this.updateFlags.withFlags(UpdateFlags.UPDATE_VERTICAL_VANE);
    }

    public void setRemoteProhibitFlags(RemoteProhibitFlags remoteProhibitFlags) {
        this.remoteProhibitFlags = remoteProhibitFlags;
        this.updateFlags.withFlags(UpdateFlags.UPDATE_REMOTE_PROHIBIT_FLAGS);
    }

    public void setHorizontalVane(HorizontalVane horizontalVane) {
        this.horizontalVane = horizontalVane;
        this.updateFlags.withFlags(UpdateFlags.UPDATE_HORIZONTAL_VANE);
    }

    public void setTargetTemperature(EnhancedTemperature targetTemperature) {
        this.targetTemperature = targetTemperature;
        this.updateFlags.withFlags(UpdateFlags.UPDATE_TEMP);
    }

    @Override
    public String toString() {
        return "SetSettingsCommand{updateFlags=" + updateFlags + ", power=" + power + ", operatingMode=" + operatingMode
                + ", legacySetpointTemperature=" + legacySetpointTemperature + ", fanMode=" + fanMode
                + ", verticalVane=" + verticalVane + ", remoteProhibitFlags=" + remoteProhibitFlags
                + ", horizontalVane=" + horizontalVane + ", targetTemperature=" + targetTemperature + '}';
    }
}
