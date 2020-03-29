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

package org.openhab.binding.nibeuplinkrest.internal.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class StringConvert {

    public static String snakeCaseToCamelCase(String input) {
        Pattern wordSeparator = Pattern.compile("_([a-z0-9])");
        String lower = input.toLowerCase(Locale.ROOT);
        Matcher match = wordSeparator.matcher(lower);
        StringBuffer output = new StringBuffer();

        while (match.find()) {
            try {
                String firstLetter = match.group(1);
                match.appendReplacement(output, firstLetter.toUpperCase(Locale.ROOT));
            } catch (RuntimeException e) {
                throw new NibeUplinkRestException("Error converting string", e);
            }
        }
        match.appendTail(output);
        return output.toString();
    }

    public static <T> String toCommaList(Collection<T> input) {
        StringBuilder sb = new StringBuilder();
        input.forEach(e -> {
            sb.append(e).append(",");
        });
        sb.deleteCharAt(sb.lastIndexOf(","));
        return sb.toString();
    }
}
