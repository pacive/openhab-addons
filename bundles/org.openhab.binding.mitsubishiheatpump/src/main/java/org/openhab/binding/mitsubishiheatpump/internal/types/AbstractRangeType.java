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

@NonNullByDefault
public abstract class AbstractRangeType extends AbstractType {
    protected static final int MIN = 0x00;
    protected static final int MAX = 0xff;

    protected AbstractRangeType(byte value) throws SerialProtocolException {
        super(value);
    }

    protected AbstractRangeType(int value) throws SerialProtocolException {
        super(value);
    }

    @Override
    protected boolean validate(int value) {
        return value <= max() && value >= min();
    }

    protected abstract int max();

    protected abstract int min();
}
