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
package org.openhab.binding.nibeuplinkrest.internal.auth;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestOAuthServletTemplate  {

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestOAuthServletTemplate.class);

    private final Pattern EXPANSION_PATTERN = Pattern.compile("\\$\\{([^\\}]+)\\}");

    private final BundleContext bundleContext;
    private String template = "";
    private Map<String, String> replaceMap = new HashMap<>();

    public NibeUplinkRestOAuthServletTemplate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public NibeUplinkRestOAuthServletTemplate(BundleContext bundleContext, String fileName) throws IOException {
        this(bundleContext);
        loadTemplateFile(fileName);
    }

    public NibeUplinkRestOAuthServletTemplate(BundleContext bundleContext, String filename,
                                              Map<String, String> replaceMap) throws IOException {
        this(bundleContext, filename);
        this.replaceMap = replaceMap;
    }

    public void loadTemplateFile(String fileName) throws IOException {
        final URL file = bundleContext.getBundle().getEntry(fileName);

        if (file == null) {
            throw new FileNotFoundException("File not found: " + fileName);
        }
        try (InputStream reader = file.openStream()) {
            this.template = IOUtils.toString(reader);
        }
    }

    public void addReplacement(String from, @Nullable String to) {
        if (to == null) {
            to = "";
        }
        replaceMap.put(from, to);
    }

    public void removeReplacement(String from) {
        replaceMap.remove(from);
    }

    public String replaceAll() {
        Matcher match = EXPANSION_PATTERN.matcher(this.template);
        StringBuffer output = new StringBuffer();

        while (match.find()) {
            try {
                String key = match.group(1);
                match.appendReplacement(output, Matcher.quoteReplacement(replaceMap.getOrDefault(key, "")));
            } catch (RuntimeException e) {
                logger.warn("Error running replacements: {}", e.getMessage());
            }
        }
        match.appendTail(output);
        return output.toString();
    }
}
