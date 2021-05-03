/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nibeuplinkrest.internal.api.model.AlarmInfo;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Mode;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Parameter;
import org.openhab.binding.nibeuplinkrest.internal.api.model.SoftwareInfo;

/**
 * Interface that specifies callback methods to inform the handler of updates
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public interface NibeUplinkRestCallbackListener {

    /**
     * Callback method when a request for parameter values has been responded
     *
     * @param parameterValues A list of {@link Parameter}s
     */
    void parametersUpdated(List<Parameter> parameterValues);

    /**
     * Callback when a request for system info has a response
     *
     * @param system A {@link NibeSystem} object
     */
    void systemUpdated(NibeSystem system);

    /**
     * Callback when a request for software info has a response
     *
     * @param softwareInfo A {@link SoftwareInfo} object
     */
    void softwareUpdateAvailable(SoftwareInfo softwareInfo);

    /**
     * Callback when a request for the systems operating mode has a response
     *
     * @param mode A {@link Mode} enum value
     */
    void modeUpdated(Mode mode);

    /**
     * Callback when a request for the systems alarm info has a response
     *
     * @param alarmInfo A {@link Mode} enum value
     */
    void alarmInfoUpdated(AlarmInfo alarmInfo);

    /**
     * Callback when a request for the systems status has a response
     *
     * @param activeComponents A {@link Set} of the Ids of the active components
     */
    void statusUpdated(Set<String> activeComponents);
}
