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
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.util.Util;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.State;

@NonNullByDefault
public abstract class AbstractType {
    protected int rawValue;
    protected final int byteSize;

    protected AbstractType(int value) throws SerialProtocolException {
        this(value, 1);
    }

    protected AbstractType(int value, int byteSize) throws SerialProtocolException {
        if (!this.validate(value)) {
            throw new SerialProtocolException("Invalid input");
        }
        this.rawValue = value;
        this.byteSize = byteSize;
    }

    protected AbstractType(byte value) throws SerialProtocolException {
        this(((int) value) & 0xff);
    }

    protected AbstractType(byte[] data) throws SerialProtocolException {
        this(Util.bytesToInt(data), data.length);
    }

    public Number getValue() {
        return getRawValue();
    }

    public int getRawValue() {
        return this.rawValue;
    }

    public byte[] serialize() {
        return Util.intToBytes(this.rawValue, this.byteSize);
    }

    protected boolean validate(int value) {
        return true;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other instanceof MitsubishiHeatpumpType) {
            return getRawValue() == ((MitsubishiHeatpumpType) other).getRawValue();
        }
        return false;
    }

    public State asState() {
        return new DecimalType(getValue());
    }

    @Override
    public String toString() {
        return getValue().toString();
    }
}
