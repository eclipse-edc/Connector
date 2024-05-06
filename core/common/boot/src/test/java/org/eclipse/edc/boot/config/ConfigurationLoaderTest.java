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
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurationLoaderTest {

    private final ServiceLocator serviceLocator = mock();
    private final Monitor monitor = mock();
    private final ConfigurationExtension configurationExtension = mock();
    private final EnvironmentVariables environmentVariables = mock();
    private final SystemProperties systemProperties = mock();

    private final ConfigurationLoader loader = new ConfigurationLoader(serviceLocator, environmentVariables, systemProperties);

    @BeforeEach
    void setUp() {
        when(environmentVariables.get()).thenReturn(emptyMap());
        when(systemProperties.get()).thenReturn(new Properties());
        when(serviceLocator.loadImplementors(any(), anyBoolean())).thenReturn(List.of(configurationExtension));
    }

    @Test
    void shouldInitializeConfigurationExtensions() {
        loader.loadConfiguration(monitor);

        verify(configurationExtension).initialize(monitor);
    }

    @Test
    void shouldLoadEmptyConfiguration_whenNoConfigurationExtension() {
        when(serviceLocator.loadImplementors(any(), anyBoolean())).thenReturn(emptyList());

        var config = loader.loadConfiguration(monitor);

        assertThat(config).isNotNull();
        assertThat(config.getRelativeEntries()).isEmpty();
        verify(serviceLocator).loadImplementors(ConfigurationExtension.class, false);
    }

    @Test
    void shouldLoadConfigFromExtension() {
        var extensionConfig = ConfigFactory.fromMap(Map.of("edc.test.entry1", "value1", "edc.test.entry2", "value2"));
        when(configurationExtension.getConfig()).thenReturn(extensionConfig);

        var config = loader.loadConfiguration(monitor);

        assertThat(config.getString("edc.test.entry1")).isEqualTo("value1");
        assertThat(config.getString("edc.test.entry2")).isEqualTo("value2");
    }

    @Test
    void shouldLoadConfigFromSystemProperties() {
        var properties = new Properties();
        properties.put("edc.test.entry", "foo");
        when(systemProperties.get()).thenReturn(properties);

        var config = loader.loadConfiguration(monitor);

        assertThat(config.getString("edc.test.entry")).isEqualTo("foo");
    }

    @Test
    void shouldOverrideConfig_whenSystemPropertiesOverlap() {
        var extensionConfig = ConfigFactory.fromMap(Map.of("edc.test.entry1", "value1", "edc.test.entry2", "value2"));
        when(configurationExtension.getConfig()).thenReturn(extensionConfig);
        var properties = new Properties();
        properties.put("edc.test.entry2", "value override");
        when(systemProperties.get()).thenReturn(properties);

        var config = loader.loadConfiguration(monitor);

        assertThat(config.getString("edc.test.entry1")).isEqualTo("value1");
        assertThat(config.getString("edc.test.entry2")).isEqualTo("value override");
    }

    @Test
    void shouldConvertCase_whenEnvironmentVariable() {
        when(environmentVariables.get()).thenReturn(Map.of("EDC_TEST_ENTRY", "value"));

        var config = loader.loadConfiguration(monitor);

        assertThat(config.getString("edc.test.entry")).isEqualTo("value");
    }

    @Test
    void shouldOverrideConfig_whenEnvironmentVariablesOverlap() {
        var extensionConfig = ConfigFactory.fromMap(Map.of("edc.test.entry", "value"));
        when(configurationExtension.getConfig()).thenReturn(extensionConfig);
        when(environmentVariables.get()).thenReturn(Map.of("EDC_TEST_ENTRY", "value override"));

        var config = loader.loadConfiguration(monitor);

        assertThat(config.getString("edc.test.entry")).isEqualTo("value override");
    }

    @Test
    void shouldOverrideEnvironmentVariablesWithSystemProperties() {
        when(environmentVariables.get()).thenReturn(Map.of("EDC_TEST_ENTRY", "value"));
        var properties = new Properties();
        properties.put("edc.test.entry", "value override");
        when(systemProperties.get()).thenReturn(properties);

        var config = loader.loadConfiguration(monitor);

        assertThat(config.getString("edc.test.entry")).isEqualTo("value override");
    }

}
