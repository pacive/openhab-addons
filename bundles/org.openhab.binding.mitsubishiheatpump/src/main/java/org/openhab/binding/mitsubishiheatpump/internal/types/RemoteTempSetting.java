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
public class RemoteTempSetting extends AbstractType implements MitsubishiHeatpumpType {
    public static final RemoteTempSetting INTERNAL;
    public static final RemoteTempSetting EXTERNAL;

    static {
        try {
            INTERNAL = new RemoteTempSetting(0);
            EXTERNAL = new RemoteTempSetting(1);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public RemoteTempSetting(int value) throws SerialProtocolException {
        super(value);
    }

    public RemoteTempSetting(byte value) throws SerialProtocolException {
        super(value);
    }
}
