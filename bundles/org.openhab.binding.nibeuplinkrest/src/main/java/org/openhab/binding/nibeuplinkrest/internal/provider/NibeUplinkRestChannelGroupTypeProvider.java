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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.type.*;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Provides generated channel-group-types to the framework
 *
 * @author Anders Alfredsson - Initial contribution
 */
@Component(service = { ChannelGroupTypeProvider.class, NibeUplinkRestChannelGroupTypeProvider.class }, immediate = true)
@NonNullByDefault
public class NibeUplinkRestChannelGroupTypeProvider implements ChannelGroupTypeProvider {

    private final Map<ChannelGroupTypeUID, ChannelGroupType> channelGroupTypes = new ConcurrentHashMap<>();

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return channelGroupTypes.values();
    }

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        return channelGroupTypes.get(channelGroupTypeUID);
    }

    /**
     * Adnna channel-group-type
     * 
     * @param type
     */
    public void add(ChannelGroupType type) {
        channelGroupTypes.putIfAbsent(type.getUID(), type);
    }

    /**
     * Add a list of channel-group-types
     *
     * @param types
     */
    public void addAll(List<ChannelGroupType> types) {
        types.forEach(t -> {
            add(t);
        });
    }

    /**
     * Convenience method to get a channels full id (groupId#channelId)
     * based on it's channel id
     *
     * @param channelId
     * @return
     */
    public String getChannelFromID(String channelId) {
        for (ChannelGroupType groupType : channelGroupTypes.values()) {
            for (ChannelDefinition channel : groupType.getChannelDefinitions()) {
                if (channel.getId().equals(channelId))
                    return groupType.getUID().getId() + "#" + channelId;
            }
        }
        return "";
    }

    public void remove(ChannelGroupTypeUID uid) {
        channelGroupTypes.remove(uid);
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
    }

    @Deactivate
    protected void deactivate() {
        channelGroupTypes.clear();
    }
}
