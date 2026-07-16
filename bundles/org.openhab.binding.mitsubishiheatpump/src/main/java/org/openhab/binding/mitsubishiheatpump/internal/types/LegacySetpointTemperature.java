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
package org.openhab.binding.mitsubishiheatpump.internal.types;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.types.State;

@NonNullByDefault
public class LegacySetpointTemperature extends AbstractRangeType implements MitsubishiHeatpumpType {
    private static final int MIN = 0x00;
    private static final int MAX = 0x1f;

    public LegacySetpointTemperature(int rawValue) throws SerialProtocolException {
        super(rawValue);
    }

    public LegacySetpointTemperature(byte value) throws SerialProtocolException {
        super(value);
    }

    public LegacySetpointTemperature(float value) throws SerialProtocolException {
        super(convertToRaw(value));
    }

    protected int max() {
        return MAX;
    }

    protected int min() {
        return MIN;
    }

    @Override
    public Number getValue() {
        float value = 31f - (getRawValue() % 0x10);
        if (getRawValue() % 0x10 > 0) {
            value += 0.5f;
        }
        return value;
    }

    private static int convertToRaw(float value) {
        if (value < 16f) {
            value = 16f;
        } else if (value > 31.5f) {
            value = 31.5f;
        }

        int rawValue = (31 - (int) value) & 0x0f;

        if (value % 1 == 0.5f) {
            rawValue += 0x10;
        }

        return rawValue;
    }

    @Override
    public State asState() {
        return new QuantityType<>(getValue(), SIUnits.CELSIUS);
    }
}
