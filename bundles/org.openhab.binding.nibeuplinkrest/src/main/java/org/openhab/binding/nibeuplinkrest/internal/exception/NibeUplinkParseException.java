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

package org.openhab.binding.nibeuplinkrest.internal.exception;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkParseException extends NibeUplinkException {

    private static final long serialVersionUID = 4684643516566565L;

    public NibeUplinkParseException(String message) {
        super(message);
    }

    public NibeUplinkParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
