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
package org.openhab.binding.mitsubishiheatpump.internal.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.MitsubishiHeatpumpPDU;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.command.GetRequestCommand;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.command.SetRemoteTemperatureCommand;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.command.SetSettingsCommand;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.packet.GetRequestPacket;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.packet.SetRequestPacket;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.EnhancedTemperature;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.FanMode;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.HorizontalVane;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.OperatingMode;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.Power;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.VerticalVane;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.types.Command;

@NonNullByDefault
public class ThingHandlerHelper {
    public static MitsubishiHeatpumpPDU createGetSettingsPDU() {
        GetRequestCommand grc = GetRequestCommand.GET_SETTINGS;
        return new GetRequestPacket(grc);
    }

    public static MitsubishiHeatpumpPDU createSetPowerPDU(Command command) throws SerialProtocolException {
        if (!(command instanceof OnOffType onOffType)) {
            throw new SerialProtocolException("Invalid command");
        }
        SetSettingsCommand mc = new SetSettingsCommand();
        if (onOffType == OnOffType.ON) {
            mc.setPower(Power.ON);
        } else {
            mc.setPower(Power.OFF);
        }
        return new SetRequestPacket(mc);
    }

    public static MitsubishiHeatpumpPDU createSetModePDU(Command command) throws SerialProtocolException {
        if (!(command instanceof DecimalType decimalType)) {
            throw new SerialProtocolException("Invalid command");
        }
        SetSettingsCommand mc = new SetSettingsCommand();
        mc.setOperatingMode(new OperatingMode(decimalType.intValue()));
        return new SetRequestPacket(mc);
    }

    public static MitsubishiHeatpumpPDU createSetpointPDU(Command command) throws SerialProtocolException {
        QuantityType<?> qt = validateTemperatureType(command);
        SetSettingsCommand mc = new SetSettingsCommand();
        mc.setTargetTemperature(new EnhancedTemperature(qt.floatValue()));
        return new SetRequestPacket(mc);
    }

    public static MitsubishiHeatpumpPDU createSetFanPDU(Command command) throws SerialProtocolException {
        if (!(command instanceof DecimalType decimalType)) {
            throw new SerialProtocolException("Invalid command");
        }
        SetSettingsCommand mc = new SetSettingsCommand();
        mc.setFanMode(new FanMode(decimalType.intValue()));
        return new SetRequestPacket(mc);
    }

    public static MitsubishiHeatpumpPDU createSetVerticalVanePDU(Command command) throws SerialProtocolException {
        if (!(command instanceof DecimalType decimalType)) {
            throw new SerialProtocolException("Invalid command");
        }
        SetSettingsCommand mc = new SetSettingsCommand();
        mc.setVerticalVane(new VerticalVane(decimalType.intValue()));
        return new SetRequestPacket(mc);
    }

    public static MitsubishiHeatpumpPDU createSetHorizontalVanePDU(Command command) throws SerialProtocolException {
        if (!(command instanceof DecimalType decimalType)) {
            throw new SerialProtocolException("Invalid command");
        }
        SetSettingsCommand mc = new SetSettingsCommand();
        mc.setHorizontalVane(new HorizontalVane(decimalType.intValue()));
        return new SetRequestPacket(mc);
    }

    public static MitsubishiHeatpumpPDU createSetRemoteTempPDU(Command command) throws SerialProtocolException {
        QuantityType<?> qt = validateTemperatureType(command);
        SetRemoteTemperatureCommand mc = new SetRemoteTemperatureCommand();
        mc.setEnhancedTemperature(new EnhancedTemperature(qt.floatValue()));
        return new SetRequestPacket(mc);
    }

    private static QuantityType<?> validateTemperatureType(Command command) throws SerialProtocolException {
        if (!(command instanceof QuantityType<?> qt)) {
            throw new SerialProtocolException("Invalid command");
        }
        if (!qt.getDimension().equals(SIUnits.CELSIUS.getDimension()) || !qt.getUnit().isCompatible(SIUnits.CELSIUS)) {
            throw new SerialProtocolException("Invalid QuantityType, must be Temperature");
        }
        qt = qt.toUnit(SIUnits.CELSIUS);
        if (qt == null) {
            throw new SerialProtocolException("Error converting unit");
        }
        return qt;
    }
}
