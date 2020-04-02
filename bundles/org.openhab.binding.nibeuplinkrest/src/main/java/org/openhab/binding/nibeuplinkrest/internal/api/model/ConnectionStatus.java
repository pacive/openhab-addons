/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.nibeuplinkrest.internal.api.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enum corresponding to different sonnection status of a system
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public enum ConnectionStatus {
    ONLINE,
    PENDING,
    OFFLINE;

    public static ConnectionStatus from(String status) {
        switch (status.toUpperCase()) {
            case "ONLINE":
                return ONLINE;
            case "PENDING":
                return PENDING;
            case "OFFLINE":
                return OFFLINE;
        }
        throw new IllegalArgumentException();
    }

    public static ConnectionStatus from(int status) {
        switch (status) {
            case 0:
                return ONLINE;
            case 1:
                return PENDING;
            case 2:
                return OFFLINE;
        }
        throw new IllegalArgumentException();
    }
}
