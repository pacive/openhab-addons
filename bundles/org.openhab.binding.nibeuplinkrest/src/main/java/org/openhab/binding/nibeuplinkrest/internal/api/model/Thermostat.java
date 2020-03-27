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

import java.util.HashSet;
import java.util.Set;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class Thermostat {
    private final int id;
    private final String thermostatName;
    private Set<Integer> climateSystems = new HashSet<>();
    private int currentTemperature;
    private int targetTemperature;

    public Thermostat(int id, String systemName, String thermostatName, Set<Integer> climateSystems) {
        this.id = id;
        this.thermostatName = thermostatName;
        this.climateSystems.addAll(climateSystems);
        currentTemperature = 0;
        targetTemperature = 0;
    }

    public int getId() { return id; }

    public String getThermostatName() { return thermostatName; }

    public Set<Integer> getClimateSystems() { return climateSystems; }

    public int getCurrentTemperature() { return currentTemperature; }

    public int getTargetTemperature() { return targetTemperature; }

    public void setCurrentTemperature(int currentTemperature) { this.currentTemperature = currentTemperature; }

    public void setTargetTemperature(int targetTemperature) { this.targetTemperature = targetTemperature; }
}
