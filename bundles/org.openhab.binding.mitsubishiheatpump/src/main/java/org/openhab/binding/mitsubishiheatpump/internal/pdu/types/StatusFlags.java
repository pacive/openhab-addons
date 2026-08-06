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
public class StatusFlags extends AbstractBitMapType implements MitsubishiHeatpumpType {
    public static final StatusFlags NORMAL;
    public static final StatusFlags FILTER;
    public static final StatusFlags DEFROST;
    public static final StatusFlags PREHEAT;
    public static final StatusFlags STANDBY;

    static {
        try {
            NORMAL = new StatusFlags(0x00);
            FILTER = new StatusFlags(0x01);
            DEFROST = new StatusFlags(0x02);
            PREHEAT = new StatusFlags(0x04);
            STANDBY = new StatusFlags(0x08);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public StatusFlags(int value) throws SerialProtocolException {
        super(value);
    }

    public StatusFlags(byte data) throws SerialProtocolException {
        super(data);
    }
}
