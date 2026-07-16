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
package org.openhab.binding.mitsubishiheatpump.internal.util;

import static org.openhab.binding.mitsubishiheatpump.internal.MitsubishiHeatpumpBindingConstants.SYNC;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class Util {
    public static int bytesToInt(byte[] data) {
        int value = 0;
        for (int i = 0; i < data.length; i++) {
            value |= (((int) data[i]) & 0xff) << (8 * (data.length - 1 - i));
        }
        return value;
    }

    public static byte[] intToBytes(int value, int byteSize) {
        byte[] output = new byte[byteSize];
        for (int i = 0; i < byteSize; i++) {
            byte b = (byte) ((value >> (8 * (byteSize - 1 - i))) & 0xff);
            output[i] = b;
        }
        return output;
    }

    public static byte[] concatenate(byte[] a, byte[] b) {
        byte[] output = new byte[a.length + b.length];
        System.arraycopy(a, 0, output, 0, a.length);
        System.arraycopy(b, 0, output, a.length, b.length);
        return output;
    }

    public static byte calculateCheckSum(byte[] data) {
        byte checksum = SYNC;
        for (byte b : data) {
            checksum -= b;
        }
        return checksum;
    }
}
