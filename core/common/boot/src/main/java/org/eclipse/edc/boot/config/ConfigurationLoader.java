/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.boot.config;

import org.eclipse.edc.boot.system.ServiceLocator;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ConfigurationExtension;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.util.Objects;

/**
 * Load configuration from configuration extensions, environment variables and system properties.
 */
public class ConfigurationLoader {
    private final ServiceLocator serviceLocator;
    private final EnvironmentVariables environmentVariables;
    private final SystemProperties systemProperties;

    public ConfigurationLoader(ServiceLocator serviceLocator, EnvironmentVariables environmentVariables, SystemProperties systemProperties) {
        this.serviceLocator = serviceLocator;
        this.environmentVariables = environmentVariables;
        this.systemProperties = systemProperties;
    }

    /**
     * Load configuration.
     * Please note that Environment variables keys will be converted from the ENVIRONMENT_NOTATION to the dot.notation.
     *
     * @param monitor the monitor.
     * @return the Config instance.
     */
    public Config loadConfiguration(Monitor monitor) {
        var config = serviceLocator.loadImplementors(ConfigurationExtension.class, false)
                .stream().peek(extension -> {
                    extension.initialize(monitor);
                    monitor.debug("ConfigurationExtension Initialized: " + extension.name());
                })
                .map(ConfigurationExtension::getConfig)
                .filter(Objects::nonNull)
                .reduce(Config::merge)
                .orElse(ConfigFactory.empty());

        var environmentConfig = ConfigFactory.fromEnvironment(environmentVariables.get());
        var systemPropertyConfig = ConfigFactory.fromProperties(systemProperties.get());

        return config.merge(environmentConfig).merge(systemPropertyConfig);
    }

}
