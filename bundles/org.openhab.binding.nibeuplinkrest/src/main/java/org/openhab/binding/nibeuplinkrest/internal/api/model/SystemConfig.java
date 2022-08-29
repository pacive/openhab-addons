/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
 * Class for holding info on the system's capabilities
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class SystemConfig {
    private boolean hasCooling;
    private boolean hasHeating;
    private boolean hasHotWater;
    private boolean hasVentilation;

    public SystemConfig(boolean hasCooling, boolean hasHeating, boolean hasHotWater, boolean hasVentilation) {
        this.hasCooling = hasCooling;
        this.hasHeating = hasHeating;
        this.hasHotWater = hasHotWater;
        this.hasVentilation = hasVentilation;
    }

    public boolean hasCooling() {
        return hasCooling;
    }

    public void setHasCooling(boolean hasCooling) {
        this.hasCooling = hasCooling;
    }

    public boolean hasHeating() {
        return hasHeating;
    }

    public void setHasHeating(boolean hasHeating) {
        this.hasHeating = hasHeating;
    }

    public boolean hasHotWater() {
        return hasHotWater;
    }

    public void setHasHotWater(boolean hasHotWater) {
        this.hasHotWater = hasHotWater;
    }

    public boolean hasVentilation() {
        return hasVentilation;
    }

    public void setHasVentilation(boolean hasVentilation) {
        this.hasVentilation = hasVentilation;
    }
}
