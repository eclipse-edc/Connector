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

package org.eclipse.dataspaceconnector.boot.system;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Base service extension context.
 * <p>Prior to using, {@link #initialize()} must be called.</p>
 */
public class DefaultServiceExtensionContext implements ServiceExtensionContext {
    private final Monitor monitor;
    private final Telemetry telemetry;
    private final TypeManager typeManager;

    private final Map<Class<?>, Object> services = new HashMap<>();
    private final List<ConfigurationExtension> configurationExtensions;
    private String connectorId;
    private Config config;

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry, List<ConfigurationExtension> configurationExtensions) {
        this.typeManager = typeManager;
        this.monitor = monitor;
        this.telemetry = telemetry;
        this.configurationExtensions = configurationExtensions;
        // register as services
        registerService(TypeManager.class, typeManager);
        registerService(Monitor.class, monitor);
    }

    @Override
    public String getConnectorId() {
        return connectorId;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public Telemetry getTelemetry() {
        return telemetry;
    }

    @Override
    public TypeManager getTypeManager() {
        return typeManager;
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
        if (hasService(type)) {
            monitor.warning("A service of the type " + type.getCanonicalName() + " was already registered and has now been replaced");
        }
        services.put(type, service);
    }

    @Override
    public void initialize() {
        configurationExtensions.forEach(ext -> {
            ext.initialize(monitor);
            monitor.info("Initialized " + ext.name());
        });
        config = loadConfig();
        connectorId = getSetting("edc.connector.name", "edc-" + UUID.randomUUID());
    }

    @Override
    public Config getConfig(String path) {
        return config.getConfig(path);
    }

    // this method exists so that getting env vars can be mocked during testing
    protected Map<String, String> getEnvironmentVariables() {
        return System.getenv();
    }

    private Config loadConfig() {
        var config = configurationExtensions.stream()
                .map(ConfigurationExtension::getConfig)
                .filter(Objects::nonNull)
                .reduce(Config::merge)
                .orElse(ConfigFactory.empty());

        var environmentConfig = ConfigFactory.fromMap(getEnvironmentVariables());
        var systemPropertyConfig = ConfigFactory.fromProperties(System.getProperties());

        return config.merge(environmentConfig).merge(systemPropertyConfig);
    }

}
