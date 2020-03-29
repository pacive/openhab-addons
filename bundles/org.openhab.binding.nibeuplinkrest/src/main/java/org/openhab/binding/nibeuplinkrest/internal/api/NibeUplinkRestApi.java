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
package org.openhab.binding.nibeuplinkrest.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;

import java.util.List;
import java.util.Map;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public interface NibeUplinkRestApi {

    List<NibeSystem> getConnectedSystems();

    NibeSystem getSystem(int systemId);

    List<Category> getCategories(int systemId, boolean includeParameters);

    SystemConfig getSystemConfig(int systemId);

    SoftwareInfo getSoftwareInfo(int systemId);

    void addTrackedParameter(int systemId, int parameterId);

    void removeTrackedParameter(int systemId, int parameterId);

    void setParameters(int systemId, Map<Integer, Integer> parameters);

    void setMode(int systemId, Mode mode);

    void setThermostat(int systemId, Thermostat thermostat);

    void removeThermostat(int systemId, int thermostatId);

    void addCallbackListener(int systemId, NibeUplinkRestCallbackListener listener);

    void removeCallbackListener(int SystemId);

    void setUpdateInterval(int updateInterval);

    void setSoftwareUpdateCheckInterval(int softwareUpdateCheckInterval);
}
