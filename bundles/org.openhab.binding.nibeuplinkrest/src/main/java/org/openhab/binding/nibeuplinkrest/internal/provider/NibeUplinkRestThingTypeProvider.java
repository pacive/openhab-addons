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
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Provides generated thing-types to the framework
 *
 * @author Anders Alfredsson - Initial contribution
 */

@Component(service = { ThingTypeProvider.class, NibeUplinkRestThingTypeProvider.class }, immediate = true)
@NonNullByDefault
public class NibeUplinkRestThingTypeProvider implements ThingTypeProvider {

    private Map<ThingTypeUID, ThingType> thingTypes = new ConcurrentHashMap<>();
    private @NonNullByDefault({}) BundleContext bundleContext;

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
     * @param thingTypeUID
     * @param thingType
     */
    public void addThingType(ThingTypeUID thingTypeUID, ThingType thingType) {
        thingTypes.putIfAbsent(thingTypeUID, thingType);
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        bundleContext = componentContext.getBundleContext();
    }

    @Deactivate
    protected void deactivate() {
        thingTypes.clear();
        bundleContext = null;
    }
}
