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
import org.openhab.core.types.State;

@NonNullByDefault
public interface MitsubishiHeatpumpType {

    /**
     * Get the numeric representation of the data type, after conversion from raw form
     *
     * @return The human-readable numeric representation
     */
    Number getValue();

    /**
     * Get the raw value, as represented in the serial protocol
     *
     * @return The raw value
     */
    int getRawValue();

    /**
     * Serializes the data into a byte array, in the representation of the serial protocol
     *
     * @return A byte array representation of the data
     */
    byte[] serialize();

    /**
     * Convert the data to an openHAB State representation
     *
     * @return The data represented as a State
     */
    State asState();
}
