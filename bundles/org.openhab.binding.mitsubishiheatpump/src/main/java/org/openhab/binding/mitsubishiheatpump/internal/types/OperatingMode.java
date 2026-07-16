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
public class OperatingMode extends AbstractType implements MitsubishiHeatpumpType {
    public static final OperatingMode HEAT;
    public static final OperatingMode DEHUMIDIFY;
    public static final OperatingMode COOL;
    public static final OperatingMode FAN;
    public static final OperatingMode AUTO;
    public static final OperatingMode I_SEE_HEAT;
    public static final OperatingMode I_SEE_DRY;
    public static final OperatingMode I_SEE_COOL;
    public static final OperatingMode AUTO_HEAT;
    public static final OperatingMode AUTO_COOL;

    static {
        try {
            HEAT = new OperatingMode(1);
            DEHUMIDIFY = new OperatingMode(2);
            COOL = new OperatingMode(3);
            FAN = new OperatingMode(7);
            AUTO = new OperatingMode(8);
            I_SEE_HEAT = new OperatingMode(9);
            I_SEE_DRY = new OperatingMode(10);
            I_SEE_COOL = new OperatingMode(11);
            AUTO_HEAT = new OperatingMode(33);
            AUTO_COOL = new OperatingMode(35);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public OperatingMode(int value) throws SerialProtocolException {
        super(value);
    }

    public OperatingMode(byte value) throws SerialProtocolException {
        super(value);
    }
}
