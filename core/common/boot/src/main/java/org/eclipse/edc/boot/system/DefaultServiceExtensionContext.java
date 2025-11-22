/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.boot.system;

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.boot.BootServicesExtension.COMPONENT_ID;
import static org.eclipse.edc.boot.BootServicesExtension.RUNTIME_ID;

/**
 * Base service extension context.
 * <p>Prior to using, {@link #initialize()} must be called.</p>
 */
public class DefaultServiceExtensionContext implements ServiceExtensionContext {


    private final Map<Class<?>, Object> services = new HashMap<>();
    private final Config config;
    private boolean isReadOnly = false;
    private String runtimeId;
    private String componentId;

    public DefaultServiceExtensionContext(Monitor monitor, Config config) {
        this.config = config;
        // register as service
        registerService(Monitor.class, monitor);
    }

    @Override
    public Config getConfig(String path) {
        return config.getConfig(path);
    }

    @Override
    public void freeze() {
        isReadOnly = true;
    }

    @Override
    public String getRuntimeId() {
        return runtimeId;
    }

    @Override
    public String getComponentId() {
        return componentId;
    }

    @Override
    public <T> boolean hasService(Class<T> type) {
        return services.containsKey(type);
    }

    @Override
    public <T> T getService(Class<T> type) {
        T service = (T) services.get(type);
        if (service == null) {
            throw new EdcException("Service not found: " + type.getName());
        }
        return service;
    }

    @Override
    public <T> T getService(Class<T> type, boolean isOptional) {
        if (!isOptional) {
            return getService(type);
        }
        return (T) services.get(type);
    }

    @Override
    public <T> void registerService(Class<T> type, T service) {
        if (isReadOnly) {
            throw new EdcException("Cannot register service " + type.getName() + ", the ServiceExtensionContext is in read-only mode.");
        }
        if (hasService(type)) {
            getMonitor().warning("A service of the type " + type.getCanonicalName() + " was already registered and has now been replaced with a " + service.getClass().getSimpleName() + " instance.");
        }
        services.put(type, service);
    }

    @Override
    public void initialize() {
        runtimeId = getSetting(RUNTIME_ID, null);
        if (runtimeId != null) {
            getMonitor().warning("A configuration value for '%s' was found. Explicitly configuring this is not supported anymore and may get removed in the future. A random value will be assigned.".formatted(RUNTIME_ID));
        } else {
            // runtime-id should always be randomized to guarantee a working lease mechanism
            runtimeId = UUID.randomUUID().toString();
        }

        componentId = getSetting(COMPONENT_ID, null);
        if (componentId == null) {
            componentId = ofNullable(runtimeId)
                    .orElseGet(() -> {
                        getMonitor().warning("%s is not configured so a random UUID is used. It is recommended to provide a static one.".formatted(COMPONENT_ID));
                        return UUID.randomUUID().toString();
                    });
        }

    }

}
