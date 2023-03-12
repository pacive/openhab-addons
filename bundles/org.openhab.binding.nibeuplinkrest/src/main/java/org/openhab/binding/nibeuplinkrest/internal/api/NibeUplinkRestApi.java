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
package org.openhab.binding.nibeuplinkrest.internal.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Category;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Mode;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.api.model.SystemConfig;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Thermostat;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;

/**
 * The interface between the handlers and the classes that handles the http requests to and responses
 * from Nibe uplink.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public interface NibeUplinkRestApi {

    /**
     * Get a list of systems connected to the users Nibe uplink account
     *
     * @return A list of {@link NibeSystem} objects
     */
    List<NibeSystem> getConnectedSystems();

    /**
     * Get info on the systems configuration - if it supports heating, cooling, hot water and ventilation
     *
     * @param systemId Id of the system
     * @return A {@link SystemConfig} object holding info on the system
     */
    SystemConfig getSystemConfig(int systemId) throws NibeUplinkRestException;

    /**
     * Get a list of categories that represents different components of the system
     *
     * @param systemId Id of the system
     * @param includeParameters Whether parameters related to the categories should be retrieved as well
     * @return A list of {@link Category} objects
     */
    List<Category> getCategories(int systemId, boolean includeParameters) throws NibeUplinkRestException;

    /**
     * Get info on a specific system. The request is placed at the head of the queue and the response
     * gets sent to the {@link NibeUplinkRestCallbackListener} registered for the systemId.
     *
     * @param systemId Id of the system to get
     */
    void requestSystem(int systemId);

    /**
     * Get info on a latest alarm on the system. The request is placed at the head of the queue and the response
     * gets sent to the {@link NibeUplinkRestCallbackListener} registered for the systemId.
     *
     * @param systemId Id of the system to get
     */
    void requestLatestAlarm(int systemId);

    /**
     * Get info on the software version installed on the system as well as any available upgrade. The request is placed
     * at the head of the queue and the response gets sent to the {@link NibeUplinkRestCallbackListener} registered for
     * the systemId.
     *
     * @param systemId Id of the system
     */
    void requestSoftwareInfo(int systemId);

    /**
     * Add a parameter to be tracked, to be included in requests
     *
     * @param systemId Id of the system
     * @param parameterId Id of the parameter
     */
    void addTrackedParameter(int systemId, int parameterId);

    /**
     * Remove a parameter from being tracked. It will no longer be included in requests.
     *
     * @param systemId Id of the system
     * @param parameterId Id of the parameter
     */
    void removeTrackedParameter(int systemId, int parameterId);

    /**
     * Get a list of parameter. The request is placed at the head of the queue and the response
     * gets sent to the {@link NibeUplinkRestCallbackListener} registered for the systemId.
     *
     * @param systemId Id of the system
     * @param parameterIds A {@link Set} of parameters to get
     */
    void requestParameters(int systemId, Set<Integer> parameterIds);

    /**
     * Set writeable parameters to a specified value
     *
     * @param systemId Id of the system
     * @param parameters A {@link Map} of parameters and corresponding values to be set
     */
    void setParameters(int systemId, Map<Integer, Integer> parameters) throws NibeUplinkRestException;

    /**
     * Gets the system operating mode - Default, away or vacation. The request is placed at the head of the queue and
     * the response gets sent to the {@link NibeUplinkRestCallbackListener} registered for the systemId.
     * 
     * @param systemId Id of the system
     */
    void requestMode(int systemId);

    /**
     * Set the system operating mode - Default, away or vacation
     *
     * @param systemId Id of the system
     * @param mode One of {@link Mode}'s enums
     */
    void setMode(int systemId, Mode mode);

    /**
     * Adds or updates a virtual thermostat connected to Nibe uplink that influences the system
     *
     * @param systemId Id of the system
     * @param thermostat A {@link Thermostat} object holding info to be sent to Nibe uplink
     */
    void setThermostat(int systemId, Thermostat thermostat);

    /**
     * Removes a thermostat, so it no longer affects the system.
     *
     * @param systemId Id of the system
     * @param thermostatId Id of the thermostat
     */
    void removeThermostat(int systemId, int thermostatId);

    /**
     * Add a callback listener that should receive updates for a specific system. There can only be one for
     * each system.
     *
     * @param systemId Id of the system
     * @param listener A {@link NibeUplinkRestCallbackListener} that should receive updates
     */
    void addCallbackListener(int systemId, NibeUplinkRestCallbackListener listener);

    /**
     * Removes the callback listener for the specified system
     * 
     * @param SystemId Id of the system
     */
    void removeCallbackListener(int SystemId);

    /**
     * Set the amount of seconds between updates to the parameters and system info
     * 
     * @param updateInterval Time in seconds
     */
    void setUpdateInterval(int updateInterval);

    /**
     * Set the number of days between checks for software updates
     * 
     * @param softwareUpdateCheckInterval Time in days
     */
    void setSoftwareUpdateCheckInterval(int softwareUpdateCheckInterval);
}