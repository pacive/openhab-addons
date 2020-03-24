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
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public enum Mode {
    DEFAULT_OPERATION,
    AWAY_FROM_HOME,
    VACATION;

    public Mode from(String mode) {
        switch (mode.toUpperCase()) {
            case "DEFAULT_OPERATION":
                return DEFAULT_OPERATION;
            case "AWAY_FROM_HOME":
                return AWAY_FROM_HOME;
            case "VACATION":
                return VACATION;
        }
        throw new IllegalArgumentException();
    }

    public Mode from(int mode) {
        switch (mode) {
            case 0:
                return DEFAULT_OPERATION;
            case 1:
                return AWAY_FROM_HOME;
            case 2:
                return VACATION;
        }
        throw new IllegalArgumentException();
    }
}
