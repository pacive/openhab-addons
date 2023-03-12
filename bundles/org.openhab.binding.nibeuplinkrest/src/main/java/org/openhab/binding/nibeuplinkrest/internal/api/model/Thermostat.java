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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Class representing a virtual thermostat that can affect the system
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class Thermostat {
    @SerializedName("externalId")
    private final int id;
    private final String name;
    private @Nullable List<Integer> climateSystems;
    private @Nullable Integer actualTemp;
    private @Nullable Integer targetTemp;

    public Thermostat(int id, String name, @Nullable List<Integer> climateSystems, @Nullable Double actualTemp,
            @Nullable Double targetTemp) {
        this.id = id;
        this.name = name;
        this.climateSystems = climateSystems;
        this.actualTemp = actualTemp == null ? null : (int) (actualTemp * 10);
        this.targetTemp = targetTemp == null ? null : (int) (targetTemp * 10);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public @Nullable List<Integer> getClimateSystems() {
        return climateSystems;
    }

    public @Nullable Double getActualTemp() {
        Integer localRef = actualTemp;
        return localRef == null ? null : (double) localRef / 10;
    }

    public @Nullable Double getTargetTemp() {
        Integer localRef = targetTemp;
        return localRef == null ? null : (double) localRef / 10;
    }

    public void setActualTemp(int actualTemp) {
        this.actualTemp = actualTemp;
    }

    public void setTargetTemp(int targetTemp) {
        this.targetTemp = targetTemp;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Thermostat that = (Thermostat) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
