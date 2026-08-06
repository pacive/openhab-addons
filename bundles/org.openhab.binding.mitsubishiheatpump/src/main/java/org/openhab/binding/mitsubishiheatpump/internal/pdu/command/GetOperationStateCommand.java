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
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.AccumulatedPower;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.CompressorFrequency;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.InputPower;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.Power;

@NonNullByDefault
public class GetOperationStateCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = 0x06;

    private final CompressorFrequency compressorFrequency;
    private final Power operating;
    private final InputPower inputPower;
    private final AccumulatedPower accumulatedPower;

    public GetOperationStateCommand(byte[] data) throws SerialProtocolException {
        if (data.length != DEFAULT_LENGTH) {
            throw new SerialProtocolException("Data of unexpected length");
        }
        this.compressorFrequency = new CompressorFrequency(data[3]);
        this.operating = new Power(data[4]);
        this.inputPower = new InputPower(Arrays.copyOfRange(data, 5, 7));
        this.accumulatedPower = new AccumulatedPower(Arrays.copyOfRange(data, 7, 9));
    }

    public byte getId() {
        return COMMAND_ID;
    }

    public byte[] serialize() {
        byte[] data = new byte[DEFAULT_LENGTH];
        data[0] = COMMAND_ID;
        data[3] = compressorFrequency.serialize()[0];
        data[4] = operating.serialize()[0];
        System.arraycopy(inputPower.serialize(), 0, data, 5, 2);
        System.arraycopy(accumulatedPower.serialize(), 0, data, 7, 2);

        return data;
    }

    public CompressorFrequency getCompressorFrequency() {
        return compressorFrequency;
    }

    public Power getOperating() {
        return operating;
    }

    public InputPower getInputPower() {
        return inputPower;
    }

    public AccumulatedPower getAccumulatedPower() {
        return accumulatedPower;
    }

    @Override
    public String toString() {
        return "GetOperationStateCommand{compressorFrequency=" + compressorFrequency + ", operating=" + operating
                + ", inputPower=" + inputPower + ", accumulatedPower=" + accumulatedPower + '}';
    }
}
