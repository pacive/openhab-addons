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

package org.openhab.binding.nibeuplinkrest.internal.provider;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.type.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@Component(service = { ChannelGroupTypeProvider.class, NibeUplinkRestChannelGroupTypeProvider.class }, immediate = true)
@NonNullByDefault
public class NibeUplinkRestChannelGroupTypeProvider implements ChannelGroupTypeProvider {

    private final Map<ChannelGroupTypeUID, ChannelGroupType> channelGroupTypes = new ConcurrentHashMap<>();
    private @NonNullByDefault({}) BundleContext bundleContext;

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return channelGroupTypes.values();
    }

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, @Nullable Locale locale) {
        return channelGroupTypes.get(channelGroupTypeUID);
    }

    public void add(ChannelGroupType type) {
        channelGroupTypes.putIfAbsent(type.getUID(), type);
    }

    public void addAll(List<ChannelGroupType> types) {
        types.forEach(t -> {
            add(t);
        });
    }

    public String getGroupFromID(String channelId) {
        for (ChannelGroupType groupType : channelGroupTypes.values()) {
            for (ChannelDefinition channel : groupType.getChannelDefinitions()) {
                if (channel.getId().equals(channelId))
                    return groupType.getUID().getId();
            }
        }
        return "";
    }

    public void remove(ChannelGroupTypeUID uid) {
        channelGroupTypes.remove(uid);
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        bundleContext = componentContext.getBundleContext();
    }

    @Deactivate
    protected void deactivate() {
        channelGroupTypes.clear();
        bundleContext = null;
    }
}
