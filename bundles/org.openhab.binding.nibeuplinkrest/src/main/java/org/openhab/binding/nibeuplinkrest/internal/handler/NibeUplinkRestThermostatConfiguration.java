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
package org.openhab.binding.nibeuplinkrest.internal.handler;

import org.eclipse.jdt.annotation.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@link NibeUplinkRestThermostatConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Anders Alfredsson - Initial contribution
 */

public class NibeUplinkRestThermostatConfiguration {
    public int id;
    public int systemId;
    public String name;
    private @Nullable String climateSystems;

    public @Nullable Set<Integer> getClimateSystems() {
        if (climateSystems == null) { return null; }
        return Arrays.asList(climateSystems.split(",")).stream()
                .map(Integer::parseInt).collect(Collectors.toSet());
    }
}
