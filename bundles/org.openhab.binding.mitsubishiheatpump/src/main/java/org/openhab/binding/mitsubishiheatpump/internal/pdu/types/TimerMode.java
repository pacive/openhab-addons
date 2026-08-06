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
public class TimerMode extends AbstractType implements MitsubishiHeatpumpType {
    public static final TimerMode NONE;
    public static final TimerMode OFF_ONLY;
    public static final TimerMode ON_ONLY;
    public static final TimerMode BOTH;

    static {
        try {
            NONE = new TimerMode(0);
            OFF_ONLY = new TimerMode(1);
            ON_ONLY = new TimerMode(2);
            BOTH = new TimerMode(3);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public TimerMode(int value) throws SerialProtocolException {
        super(value);
    }

    public TimerMode(byte value) throws SerialProtocolException {
        super(value);
    }
}
