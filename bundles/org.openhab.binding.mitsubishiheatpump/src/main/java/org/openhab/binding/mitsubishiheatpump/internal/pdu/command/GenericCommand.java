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
package org.openhab.binding.mitsubishiheatpump.internal.pdu.command;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class GenericCommand implements MitsubishiHeatpumpCommand {
    private final byte[] data;

    public GenericCommand(byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] serialize() {
        return data;
    }

    @Override
    public byte getId() {
        return 0;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
