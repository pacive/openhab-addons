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
public class AutoMode extends AbstractType implements MitsubishiHeatpumpType {
    public static final AutoMode DIRECT;
    public static final AutoMode AUTO_FAN;
    public static final AutoMode AUTO_HEAT;
    public static final AutoMode AUTO_COOL;
    public static final AutoMode AUTO_LEADER;

    static {
        try {
            DIRECT = new AutoMode(0x00);
            AUTO_FAN = new AutoMode(0x01);
            AUTO_HEAT = new AutoMode(0x02);
            AUTO_COOL = new AutoMode(0x03);
            AUTO_LEADER = new AutoMode(0x40);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public AutoMode(int value) throws SerialProtocolException {
        super(value);
    }

    public AutoMode(byte value) throws SerialProtocolException {
        super(value);
    }
}
