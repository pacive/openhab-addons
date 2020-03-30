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

import java.util.Map;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class SoftwareInfo {
    private String currentVersion;
    private @Nullable Map<String, String> upgrade;

    public SoftwareInfo(String currentVersion, @Nullable Map<String, String> upgrade) {
        this.currentVersion = currentVersion;
        this.upgrade = upgrade;
    }

    public void setCurrentVersion(String currentVersion) { this.currentVersion = currentVersion; }

    public void setUpgradeAvailable(Map<String, String> upgrade) { this.upgrade = upgrade; }

    public String getCurrentVersion() { return currentVersion; }

    public @Nullable Map<String, String> getUpgradeAvailable() { return upgrade; }

    public boolean isUpgradeAvailable() { return upgrade != null; }
}
