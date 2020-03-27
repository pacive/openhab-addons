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
public class QueuedUpdate {
    private enum QueueStatus {
        SENDING,
        VERIFYING,
        DONE,
        TIMEOUT;

        public static QueueStatus from(String status) {
            switch (status.toUpperCase()) {
                case "SENDING":
                    return SENDING;
                case "VERIFYING":
                    return VERIFYING;
                case "DONE":
                    return DONE;
                case "TIMEOUT":
                    return TIMEOUT;
            }
            throw new IllegalArgumentException();
        }

        public static QueueStatus from(int status) {
            switch (status) {
                case 0:
                    return SENDING;
                case 1:
                    return VERIFYING;
                case 2:
                    return DONE;
                case 3:
                    return TIMEOUT;
            }
            throw new IllegalArgumentException();
        }
    }

    private final Parameter parameter;
    private QueueStatus status;

    public QueuedUpdate(Parameter parameter, QueueStatus status) {
        this.parameter = parameter;
        this.status = status;
    }

    public QueuedUpdate(Parameter parameter, String status) {
        this.parameter = parameter;
        this.status = QueueStatus.from(status);
    }

    public QueuedUpdate(Parameter parameter, int status) {
        this.parameter = parameter;
        this.status = QueueStatus.from(status);
    }

    public Parameter getParameter() {
        return parameter;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public void setStatus(String status) {
        this.status = QueueStatus.from(status);
    }

    public void setStatus(int status) {
        this.status = QueueStatus.from(status);
    }
}
