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
public class EnhancedTemperature extends AbstractRangeType implements MitsubishiHeatpumpType {
    public EnhancedTemperature(int rawValue) throws SerialProtocolException {
        super(rawValue);
    }

    public EnhancedTemperature(byte value) throws SerialProtocolException {
        super(value);
    }

    public EnhancedTemperature(float value) throws SerialProtocolException {
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
        return ((float) (getRawValue() - 128)) / 2;
    }

    private static int convertToRaw(float value) {
        return (int) (value * 2 + 128);
    }

    @Override
    public State asState() {
        return new QuantityType<>(getValue(), SIUnits.CELSIUS);
    }
}
