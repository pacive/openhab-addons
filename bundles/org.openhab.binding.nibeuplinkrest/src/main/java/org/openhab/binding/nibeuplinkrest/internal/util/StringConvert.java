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

package org.openhab.binding.nibeuplinkrest.internal.util;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class StringConvert {

    public static String snakeCaseToCamelCase(String input) {
        return toCamelCase(input.replace("_", " "));
    }

    public static String toCamelCase(String input) {
        String[] parts = input.toLowerCase(Locale.ROOT).split("\\s");
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = i > 0 ? parts[i].substring(0, 1).toUpperCase(Locale.ROOT) + parts[i].substring(1) : parts[i];
            output.append(clean(part));
        }
        return output.toString();
    }

    public static String clean(String input) {
        return input.replaceAll("\\W", "");
    }

    public static String capitalize(String input) {
        return Stream.of(input.split("\\s"))
                .map((str) -> str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1))
                .collect(Collectors.joining(" "));
    }
}
