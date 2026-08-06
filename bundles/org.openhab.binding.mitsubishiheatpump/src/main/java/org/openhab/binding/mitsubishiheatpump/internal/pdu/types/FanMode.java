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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;

@NonNullByDefault
public class FanMode extends AbstractType implements MitsubishiHeatpumpType {
    public static final FanMode AUTO;
    public static final FanMode QUIET;
    public static final FanMode LOW;
    public static final FanMode MEDIUM;
    public static final FanMode HIGH;
    public static final FanMode VERY_HIGH;

    static {
        try {
            AUTO = new FanMode(0);
            QUIET = new FanMode(1);
            LOW = new FanMode(2);
            MEDIUM = new FanMode(3);
            HIGH = new FanMode(5);
            VERY_HIGH = new FanMode(6);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public FanMode(int value) throws SerialProtocolException {
        super(value);
    }

    public FanMode(byte value) throws SerialProtocolException {
        super(value);
    }
}
