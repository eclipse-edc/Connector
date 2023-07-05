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

package org.eclipse.edc.junit.extensions;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ConfigurationExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.mockito.Mockito.mock;

/**
 * This variant of the {@link DefaultServiceExtensionContext} maintains a list of service mocks, so that they will be used for dependency resolution
 * during the extension loading phase.
 * If a mock exists for any particular service, the following behavioural changes occur:
 * <ul>
 *     <li>{@link ServiceExtensionContext#registerService(Class, Object)} will only register the service, if no mock exists for the same type</li>
 *     <li>{@link ServiceExtensionContext#getService(Class)} will always return the service mock, if one exists </li>
 *     <li>{@link ServiceExtensionContext#hasService(Class)} will check first if a mock exists for any service, and then forward the call to the superclass</li>
 * </ul>
 *
 * <strong>This is only to be used in test situations!</strong>
 */
public class TestServiceExtensionContext extends DefaultServiceExtensionContext {
    private final LinkedHashMap<Class<?>, Object> serviceMocks;

    public static ServiceExtensionContext testServiceExtensionContext() {
        var context = new TestServiceExtensionContext(mock(), Collections.emptyList(), new LinkedHashMap<>());
        context.initialize();
        return context;
    }

    public TestServiceExtensionContext(Monitor monitor, List<ConfigurationExtension> configurationExtensions, LinkedHashMap<Class<?>, Object> serviceMocks) {
        super(monitor, configurationExtensions);
        this.serviceMocks = serviceMocks;
    }

    @Override
    public <T> boolean hasService(Class<T> type) {
        return (serviceMocks != null && serviceMocks.containsKey(type)) || super.hasService(type);
    }

    @Override
    public <T> T getService(Class<T> type) {
        return (T) ofNullable(serviceMocks.get(type)).orElseGet(() -> super.getService(type));
    }

    @Override
    public <T> void registerService(Class<T> type, T service) {
        if (serviceMocks != null && serviceMocks.containsKey(type)) {
            getMonitor().warning("TestServiceExtensionContext: A service mock was registered for type " + type.getCanonicalName() + " - will NOT register a " + service.getClass().getCanonicalName());
        } else {
            super.registerService(type, service);
        }
    }
}
