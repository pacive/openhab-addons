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

import java.time.ZonedDateTime;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Class for holding general info on the system
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeSystem {
    private final int systemId;
    private String name;
    private String productName;
    private String securityLevel;
    private String serialNumber;
    private ZonedDateTime lastActivityDate;
    private ConnectionStatus connectionStatus;
    private boolean hasAlarmed;
    private @Nullable SystemConfig config;

    public NibeSystem(int systemId, String name, String productName, String securityLevel, String serialNumber,
            ZonedDateTime lastActivityDate, ConnectionStatus connectionStatus, boolean hasAlarmed) {
        this.systemId = systemId;
        this.name = name;
        this.productName = productName;
        this.securityLevel = securityLevel;
        this.serialNumber = serialNumber;
        this.lastActivityDate = lastActivityDate;
        this.connectionStatus = connectionStatus;
        this.hasAlarmed = hasAlarmed;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public void setLastActivityDate(ZonedDateTime lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void setHasAlarmed(boolean hasAlarmed) {
        this.hasAlarmed = hasAlarmed;
    }

    public void setConfig(@Nullable SystemConfig config) {
        this.config = config;
    }

    public int getSystemId() {
        return systemId;
    }

    public String getName() {
        return name;
    }

    public String getProductName() {
        return productName;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public ZonedDateTime getLastActivityDate() {
        return lastActivityDate;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public boolean hasAlarmed() {
        return hasAlarmed;
    }

    public @Nullable SystemConfig getConfig() {
        return config;
    }

    public boolean isConfigSet() {
        return config != null;
    }

    public boolean hasCooling() {
        SystemConfig localRef = config;
        return localRef != null && localRef.hasCooling();
    }

    public boolean hasHeating() {
        SystemConfig localRef = config;
        return localRef != null && localRef.hasHeating();
    }

    public boolean hasHotWater() {
        SystemConfig localRef = config;
        return localRef != null && localRef.hasHotWater();
    }

    public boolean hasVentilation() {
        SystemConfig localRef = config;
        return localRef != null && localRef.hasVentilation();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NibeSystem nibeSystem = (NibeSystem) o;
        return systemId == nibeSystem.systemId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemId);
    }
}
