/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.spi.system.ServiceExtensionContext;

public final class InjectorImpl implements Injector {

    private final DefaultServiceSupplier defaultServiceSupplier;

    /**
     * Constructs a new Injector instance, which can either resolve services from the {@link ServiceExtensionContext}, or -
     * if the required service is not present - use the default implementations provided in the map.
     *
     * @param defaultServiceSupplier A function that maps a type to its default service instance, that could be null.
     */
    public InjectorImpl(DefaultServiceSupplier defaultServiceSupplier) {
        this.defaultServiceSupplier = defaultServiceSupplier;
    }

    @Override
    public <T> T inject(InjectionContainer<T> container, ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        container.getInjectionPoints().forEach(ip -> {
            try {
                var service = ip.resolve(context, defaultServiceSupplier);
                if (service != null) { //can only be if not required
                    ip.setTargetValue(service);
                }
            } catch (EdcInjectionException ex) {
                throw ex; //simply rethrow, do not wrap in another one
            } catch (Exception ex) { //thrown e.g. if the service is not present and is not optional
                monitor.warning("Error during injection", ex);
                throw new EdcInjectionException(ex);
            }
        });

        return container.getInjectionTarget();
    }
}
