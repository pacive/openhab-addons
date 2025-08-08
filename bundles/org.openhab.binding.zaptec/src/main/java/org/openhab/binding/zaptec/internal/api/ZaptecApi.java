/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.zaptec.internal.api;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * The {@link ZaptecApi} interface.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public interface ZaptecApi {

    void getChargers(Consumer<JsonElement> successCallback, BiConsumer<Integer, String> errorCallback);

    JsonArray getInstallations();
}
