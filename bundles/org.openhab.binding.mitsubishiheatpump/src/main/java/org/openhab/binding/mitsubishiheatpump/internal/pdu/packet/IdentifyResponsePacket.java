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
package org.openhab.binding.mitsubishiheatpump.internal.pdu.packet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.MitsubishiHeatpumpPDU;

@NonNullByDefault
public class IdentifyResponsePacket extends AbstractPacket implements MitsubishiHeatpumpPDU {
    private static final byte PACKET_TYPE = 0x7b;

    public IdentifyResponsePacket(byte[] data) throws SerialProtocolException {
        super(data);
    }

    @Override
    public byte getId() {
        return PACKET_TYPE;
    }
}
