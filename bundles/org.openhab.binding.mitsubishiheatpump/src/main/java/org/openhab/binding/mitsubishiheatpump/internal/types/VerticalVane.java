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
public class VerticalVane extends AbstractType implements MitsubishiHeatpumpType {
    public static final VerticalVane AUTO;
    public static final VerticalVane POS_1;
    public static final VerticalVane POS_2;
    public static final VerticalVane POS_3;
    public static final VerticalVane POS_4;
    public static final VerticalVane POS_5;
    public static final VerticalVane SWING;

    static {
        try {
            AUTO = new VerticalVane(0);
            POS_1 = new VerticalVane(1);
            POS_2 = new VerticalVane(2);
            POS_3 = new VerticalVane(3);
            POS_4 = new VerticalVane(4);
            POS_5 = new VerticalVane(5);
            SWING = new VerticalVane(7);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public VerticalVane(int value) throws SerialProtocolException {
        super(value);
    }

    public VerticalVane(byte value) throws SerialProtocolException {
        super(value);
    }
}
