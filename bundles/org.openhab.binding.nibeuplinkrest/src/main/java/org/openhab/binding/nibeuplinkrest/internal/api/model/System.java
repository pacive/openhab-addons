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

import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class System {
    private final int systemId;
    private String name;
    private String productName;
    private String securityLevel;
    private String serialNumber;
    private ZonedDateTime lastSeen;
    private ConnectionStatus connectionStatus;
    private String address;
    private boolean hasAlarmed;
    private @Nullable SystemConfig config;

    public System(int systemId, String name, String productName, String securityLevel, String serialNumber,
                  ZonedDateTime lastSeen, ConnectionStatus connectionStatus, String address, boolean hasAlarmed) {
        this.systemId = systemId;
        this.name = name;
        this.productName = productName;
        this.securityLevel = securityLevel;
        this.serialNumber = serialNumber;
        this.lastSeen = lastSeen;
        this.connectionStatus = connectionStatus;
        this.address = address;
        this.hasAlarmed = hasAlarmed;
    }

    public void setName(String name) { this.name = name; }

    public void setProductName(String productName) { this.productName = productName; }

    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }

    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public void setLastSeen(ZonedDateTime lastSeen) { this.lastSeen = lastSeen; }

    public void setConnectionStatus(ConnectionStatus connectionStatus) { this.connectionStatus = connectionStatus; }

    public void setAddress(String address) { this.address = address; }

    public void setHasAlarmed(boolean hasAlarmed) { this.hasAlarmed = hasAlarmed; }

    public void setConfig(SystemConfig config) { this.config = config; }

    public int getSystemId() { return systemId; }

    public String getName() { return name; }

    public String getProductName() { return productName; }

    public String getSecurityLevel() { return securityLevel; }

    public String getSerialNumber() { return serialNumber; }

    public ZonedDateTime getLastSeen() { return lastSeen; }

    public ConnectionStatus getConnectionStatus() { return connectionStatus; }

    public String getAddress() { return address; }

    public boolean hasAlarmed() { return hasAlarmed; }

    public boolean hasCooling() { return config != null ? config.hasCooling() : false; }

    public boolean hasHeating() { return config != null ? config.hasHeating() : false; }

    public boolean hasHotWater() { return config != null ? config.hasHotWater() : false; }

    public boolean hasVentilation() { return config != null ? config.hasVentilation() : false; }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        System system = (System) o;
        return systemId == system.systemId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemId);
    }
}
