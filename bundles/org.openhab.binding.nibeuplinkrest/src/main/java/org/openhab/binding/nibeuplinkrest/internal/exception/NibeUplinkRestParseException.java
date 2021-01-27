/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

package org.openhab.binding.nibeuplinkrest.internal.exception;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Exception thrown to indicate error in the parsing of data returned from Nibe uplink
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestParseException extends NibeUplinkRestException {

    private static final long serialVersionUID = 4684643516566565L;

    public NibeUplinkRestParseException(String message) {
        super(message);
    }

    public NibeUplinkRestParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
