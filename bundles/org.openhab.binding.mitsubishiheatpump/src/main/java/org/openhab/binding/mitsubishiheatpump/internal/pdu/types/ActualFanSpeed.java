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
public class ActualFanSpeed extends AbstractType implements MitsubishiHeatpumpType {
    public static final ActualFanSpeed OFF;
    public static final ActualFanSpeed VERY_LOW;
    public static final ActualFanSpeed QUIET;
    public static final ActualFanSpeed LOW;
    public static final ActualFanSpeed POWERFUL;
    public static final ActualFanSpeed SUPER_POWERFUL;
    public static final ActualFanSpeed SUPER_QUIET;

    static {
        try {
            OFF = new ActualFanSpeed(0);
            VERY_LOW = new ActualFanSpeed(1);
            QUIET = new ActualFanSpeed(2);
            LOW = new ActualFanSpeed(3);
            POWERFUL = new ActualFanSpeed(4);
            SUPER_POWERFUL = new ActualFanSpeed(5);
            SUPER_QUIET = new ActualFanSpeed(6);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public ActualFanSpeed(int value) throws SerialProtocolException {
        super(value);
    }

    public ActualFanSpeed(byte value) throws SerialProtocolException {
        super(value);
    }
}
