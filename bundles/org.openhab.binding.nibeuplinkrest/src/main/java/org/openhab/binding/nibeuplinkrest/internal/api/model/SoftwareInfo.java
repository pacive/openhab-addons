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

package org.openhab.binding.nibeuplinkrest.internal.api.model;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Class for holding info on the systems software, as well as available updates
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class SoftwareInfo {
    private String currentVersion;
    private @Nullable String upgrade;

    public SoftwareInfo(String currentVersion, @Nullable String upgrade) {
        this.currentVersion = currentVersion;
        this.upgrade = upgrade;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public void setUpgradeAvailable(String upgrade) {
        this.upgrade = upgrade;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public @Nullable String getUpgradeAvailable() {
        return upgrade;
    }

    public boolean isUpgradeAvailable() {
        return upgrade != null;
    }
}
