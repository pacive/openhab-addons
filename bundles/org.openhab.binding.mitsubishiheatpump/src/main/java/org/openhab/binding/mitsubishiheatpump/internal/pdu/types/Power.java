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
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

@NonNullByDefault
public class Power extends AbstractType implements MitsubishiHeatpumpType {
    public static final Power OFF;
    public static final Power ON;
    public static final Power TEST_MODE;

    static {
        try {
            OFF = new Power(0);
            ON = new Power(1);
            TEST_MODE = new Power(2);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public Power(int value) throws SerialProtocolException {
        super(value);
    }

    public Power(byte value) throws SerialProtocolException {
        super(value);
    }

    @Override
    public State asState() {
        if (this.equals(Power.OFF)) {
            return OnOffType.OFF;
        } else if (this.equals(Power.ON)) {
            return OnOffType.ON;
        } else {
            return UnDefType.UNDEF;
        }
    }
}
