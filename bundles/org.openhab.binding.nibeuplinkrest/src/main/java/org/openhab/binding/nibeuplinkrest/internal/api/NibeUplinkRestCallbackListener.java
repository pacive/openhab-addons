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
import org.openhab.binding.nibeuplinkrest.internal.api.model.Mode;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.api.model.SoftwareInfo;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Thermostat;

import java.util.Map;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public interface NibeUplinkRestCallbackListener {

    void parametersUpdated(Map<Integer, Integer> parameterValues);

    void systemUpdated(NibeSystem system);

    void softWareUpdateAvailable(SoftwareInfo softwareInfo);

    void modeUpdated(Mode mode);
}
