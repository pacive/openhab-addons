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
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.TimeType;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.TimerMode;

@NonNullByDefault
public class GetTimerModeCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = 0x05;

    private final TimerMode timerMode;
    private final TimeType onMinutesSet;
    private final TimeType offMinutesSet;
    private final TimeType onMinutesRemaining;
    private final TimeType ofMinutesRemaining;

    public GetTimerModeCommand(byte[] data) throws SerialProtocolException {
        if (data.length != DEFAULT_LENGTH) {
            throw new SerialProtocolException("Data of unexpected length");
        }
        this.timerMode = new TimerMode(data[3]);
        this.onMinutesSet = new TimeType(data[4]);
        this.offMinutesSet = new TimeType(data[5]);
        this.onMinutesRemaining = new TimeType(data[6]);
        this.ofMinutesRemaining = new TimeType(data[7]);
    }

    public byte getId() {
        return COMMAND_ID;
    }

    public byte[] serialize() {
        byte[] data = new byte[DEFAULT_LENGTH];
        data[0] = COMMAND_ID;
        data[3] = timerMode.serialize()[0];
        data[4] = onMinutesSet.serialize()[0];
        data[5] = offMinutesSet.serialize()[0];
        data[6] = onMinutesRemaining.serialize()[0];
        data[7] = ofMinutesRemaining.serialize()[0];

        return data;
    }

    public TimerMode getTimerMode() {
        return timerMode;
    }

    public TimeType getOnMinutesSet() {
        return onMinutesSet;
    }

    public TimeType getOffMinutesSet() {
        return offMinutesSet;
    }

    public TimeType getOnMinutesRemaining() {
        return onMinutesRemaining;
    }

    public TimeType getOfMinutesRemaining() {
        return ofMinutesRemaining;
    }

    @Override
    public String toString() {
        return "GetTimerModeCommand{timerMode=" + timerMode + ", onMinutesSet=" + onMinutesSet + ", offMinutesSet="
                + offMinutesSet + ", onMinutesRemaining=" + onMinutesRemaining + ", ofMinutesRemaining="
                + ofMinutesRemaining + '}';
    }
}
