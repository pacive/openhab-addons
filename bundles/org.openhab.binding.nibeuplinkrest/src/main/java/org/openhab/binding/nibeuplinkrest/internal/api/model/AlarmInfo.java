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

package org.openhab.binding.nibeuplinkrest.internal.api.model;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class AlarmInfo {
    private int alarmNumber;
    private String title;
    private String description;

    public AlarmInfo(int alarmNumber, String title, String description) {
        this.alarmNumber = alarmNumber;
        this.title = title;
        this.description = description;
    }

    public int getAlarmNumber() {
        return alarmNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setAlarmNumber(int alarmNumber) {
        this.alarmNumber = alarmNumber;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) %s", title, alarmNumber, description);
    }
}
