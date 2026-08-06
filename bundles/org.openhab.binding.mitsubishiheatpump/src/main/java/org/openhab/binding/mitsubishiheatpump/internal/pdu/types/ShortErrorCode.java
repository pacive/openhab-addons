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
public class ShortErrorCode extends AbstractType implements MitsubishiHeatpumpType {
    public static final ShortErrorCode NO_ERROR;

    static {
        try {
            NO_ERROR = new ShortErrorCode(0x00);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public ShortErrorCode(int value) throws SerialProtocolException {
        super(value);
    }

    public ShortErrorCode(byte value) throws SerialProtocolException {
        super(value);
    }
}
