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

package org.eclipse.dataspaceconnector.boot.system.injection;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.EdcInjectionException;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.system.injection.Injector;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

public final class InjectorImpl implements Injector {

    private final Map<Class<?>, Supplier<Object>> defaults;

    /**
     * Constructs a new Injector instance, which can either resolve services from the {@link ServiceExtensionContext}, or -
     * if the required service is not present - use the default implementations provided in the map.
     *
     * @param defaultSuppliers A map that contains dependency types as key, and default service objects as value.
     */
    public InjectorImpl(Map<Class<?>, Supplier<Object>> defaultSuppliers) {
        defaults = defaultSuppliers;
    }

    public InjectorImpl() {
        this(Map.of());
    }

    @Override
    public <T> T inject(InjectionContainer<T> container, ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        container.getInjectionPoints().forEach(ip -> {
            try {
                Object service = resolveService(context, ip.getType(), ip.isRequired());
                if (service != null) { //can only be if not required
                    ip.setTargetValue(service);
                }
            } catch (EdcInjectionException ex) {
                throw ex; //simply rethrow, do not wrap in another one
            } catch (EdcException ex) { //thrown e.g. if the service is not present and is not optional
                monitor.warning("Error during injection", ex);
                throw new EdcInjectionException(ex);
            } catch (IllegalAccessException e) { //e.g. when the field is marked "final"
                monitor.warning("Could not set injection target", e);
                throw new EdcInjectionException(e);
            }
        });

        return container.getInjectionTarget();
    }

    private Object resolveService(ServiceExtensionContext context, Class<?> serviceClass, boolean isRequired) {
        if (isRequired) {
            if (context.hasService(serviceClass)) {
                return context.getService(serviceClass, false);
            } else {
                return ofNullable(defaults.get(serviceClass)).map(Supplier::get).orElseThrow(() -> new EdcInjectionException("No default provider for required service " + serviceClass));
            }
        } else {
            var service = context.getService(serviceClass, true);
            if (service == null) {
                var supplier = defaults.get(serviceClass);
                if (supplier != null) {
                    service = supplier.get();
                }

            }
            return service;
        }
    }
}
