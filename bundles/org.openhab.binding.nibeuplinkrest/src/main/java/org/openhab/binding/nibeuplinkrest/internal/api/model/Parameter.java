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
import org.eclipse.jdt.annotation.Nullable;

import java.util.Objects;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class Parameter {
    private final int parameterId;
    private @Nullable String name;
    private @Nullable String title;
    private @Nullable String designation;
    private @Nullable String unit;
    private @Nullable String displayValue;
    private int rawValue;

    public Parameter(int parameterId) {
        this.parameterId = parameterId;
    }

    public Parameter(int parameterId, String name, String title, String designation, String unit,
                     String displayValue, int rawValue) {
        this.parameterId = parameterId;
        this.name = name;
        this.title = title;
        this.designation = designation;
        this.unit = unit;
        this.displayValue = displayValue;
        this.rawValue = rawValue;
    }

    public int getParameterId() { return parameterId; }

    public @Nullable String getName() { return name; }

    public @Nullable String getTitle() { return title; }

    public @Nullable String getDesignation() { return designation; }

    public @Nullable String getUnit() { return unit; }

    public @Nullable String getDisplayValue() { return displayValue; }

    public int getRawValue() { return rawValue; }

    public void setName(String name) { this.name = name; }

    public void setTitle(String title) { this.title = title; }

    public void setDesignation(String designation) { this.designation = designation; }

    public void setUnit(String unit) { this.unit = unit; }

    public void setDisplayValue(String displayValue) { this.displayValue = displayValue; }

    public void setRawValue(int rawValue) { this.rawValue = rawValue; }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter parameter = (Parameter) o;
        return parameterId == parameter.parameterId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterId);
    }
}
