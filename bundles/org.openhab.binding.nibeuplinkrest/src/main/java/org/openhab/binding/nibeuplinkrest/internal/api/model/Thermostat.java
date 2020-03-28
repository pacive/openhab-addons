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

import com.google.gson.annotations.SerializedName;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Set;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class Thermostat {
    @SerializedName("externalId")
    private final int id;
    private final String name;
    private @Nullable Set<Integer> climateSystems;
    private @Nullable Integer currentTemperature;
    private @Nullable Integer targetTemperature;

    public Thermostat(int id, String name, @Nullable Set<Integer> climateSystems, @Nullable Double currentTemperature,
                      @Nullable Double targetTemperature) {
        this.id = id;
        this.name = name;
        this.climateSystems = climateSystems;
        this.currentTemperature = currentTemperature == null ? null : (int) (currentTemperature * 10);
        this.targetTemperature = targetTemperature == null ? null : (int) (targetTemperature * 10);
    }

    public int getId() { return id; }

    public String getName() { return name; }

    public @Nullable Set<Integer> getClimateSystems() { return climateSystems; }

    public @Nullable Double getCurrentTemperature() { return currentTemperature == null ? null : (double) currentTemperature / 10; }

    public @Nullable Double getTargetTemperature() { return targetTemperature == null ? null : (double) targetTemperature / 10; }

    public void setCurrentTemperature(int currentTemperature) { this.currentTemperature = currentTemperature; }

    public void setTargetTemperature(int targetTemperature) { this.targetTemperature = targetTemperature; }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Thermostat that = (Thermostat) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
