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
public class RemoteProhibitFlags extends AbstractBitMapType implements MitsubishiHeatpumpType {
    public static final RemoteProhibitFlags UNLOCKED;
    public static final RemoteProhibitFlags LOCK_POWER;
    public static final RemoteProhibitFlags LOCK_MODE;
    public static final RemoteProhibitFlags LOCK_TEMPERATURE;

    static {
        try {
            UNLOCKED = new RemoteProhibitFlags(0x00);
            LOCK_POWER = new RemoteProhibitFlags(0x01);
            LOCK_MODE = new RemoteProhibitFlags(0x02);
            LOCK_TEMPERATURE = new RemoteProhibitFlags(0x04);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public RemoteProhibitFlags(int value) throws SerialProtocolException {
        super(value);
    }

    public RemoteProhibitFlags(byte value) throws SerialProtocolException {
        super(value);
    }
}
