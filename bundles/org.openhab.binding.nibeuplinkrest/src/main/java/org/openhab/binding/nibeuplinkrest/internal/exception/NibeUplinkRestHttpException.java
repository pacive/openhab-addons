/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
 * Exception class used do handle http errors
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestHttpException extends NibeUplinkRestException {

    private static final long serialVersionUID = 2688764916566565L;

    private final int responseCode;

    public NibeUplinkRestHttpException(String message, int responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    public NibeUplinkRestHttpException(String message, int responseCode, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }
}