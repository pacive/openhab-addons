/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ThingType;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Provides generated thing-types to the framework
 *
 * @author Anders Alfredsson - Initial contribution
 */

@Component(service = { ThingTypeProvider.class, NibeUplinkRestThingTypeProvider.class }, immediate = true)
@NonNullByDefault
public class NibeUplinkRestThingTypeProvider implements ThingTypeProvider {

    private final Map<ThingTypeUID, ThingType> thingTypes = new ConcurrentHashMap<>();

    @Override
    public Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        return thingTypes.values();
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        return thingTypes.get(thingTypeUID);
    }

    /**
     * Add a thing type to the provider
     * 
     * @param thingTypeUID
     * @param thingType
     */
    public void addThingType(ThingTypeUID thingTypeUID, ThingType thingType) {
        thingTypes.putIfAbsent(thingTypeUID, thingType);
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
    }

    @Deactivate
    protected void deactivate() {
        thingTypes.clear();
    }
}
