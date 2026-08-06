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
package org.openhab.binding.mitsubishiheatpump.internal.pdu.types;

import java.util.HexFormat;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;

@NonNullByDefault
public abstract class AbstractBitMapType extends AbstractType {

    protected AbstractBitMapType(int value, int byteLength) throws SerialProtocolException {
        super(value, byteLength);
    }

    protected AbstractBitMapType(int value) throws SerialProtocolException {
        super(value);
    }

    protected AbstractBitMapType(byte value) throws SerialProtocolException {
        super(value);
    }

    protected AbstractBitMapType(byte[] data) throws SerialProtocolException {
        super(data);
    }

    public void or(AbstractBitMapType other) {
        this.rawValue = this.getRawValue() | other.getRawValue();
    }

    public void and(AbstractBitMapType other) {
        this.rawValue = this.getRawValue() & other.getRawValue();
    }

    public boolean isSet(AbstractBitMapType other) {
        return (this.getRawValue() & other.getRawValue()) == other.getRawValue();
    }

    public void withFlags(AbstractBitMapType other) {
        or(other);
    }

    @Override
    public String toString() {
        return HexFormat.of().formatHex(serialize());
    }
}
