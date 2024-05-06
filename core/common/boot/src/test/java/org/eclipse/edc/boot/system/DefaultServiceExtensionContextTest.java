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
import org.eclipse.edc.spi.system.ConfigurationExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.BootServicesExtension.RUNTIME_ID;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultServiceExtensionContextTest {

    private final ConfigurationExtension configuration = mock();
    private final Monitor monitor = mock();
    private DefaultServiceExtensionContext context;

    @BeforeEach
    void setUp() {
        context = new DefaultServiceExtensionContext(monitor, List.of(configuration));
    }

    @Test
    void getConfig_onlyFromConfig() {
        var path = "edc.test";

        var extensionConfig = ConfigFactory.fromMap(Map.of("edc.test.entry1", "value1", "edc.test.entry2", "value2"));
        when(configuration.getConfig()).thenReturn(extensionConfig);
        context.initialize();

        var config = context.getConfig(path);

        assertThat(config.getString("entry1")).isEqualTo("value1");
        assertThat(config.getString("entry2")).isEqualTo("value2");
    }

    @Test
    void getConfig_withOtherProperties() {
        var path = "edc.test";

        var extensionConfig = ConfigFactory.fromMap(Map.of("edc.test.entry1", "value1", "edc.test.entry2", "value2"));
        when(configuration.getConfig()).thenReturn(extensionConfig);
        System.setProperty("edc.test.entry3", "foo");

        context.initialize();

        var config = context.getConfig(path);
        try {
            assertThat(config.getString("entry1")).isEqualTo("value1");
            assertThat(config.getString("entry2")).isEqualTo("value2");
            assertThat(config.getString("entry3")).isEqualTo("foo");
        } finally {
            System.clearProperty("edc.test.entry3");
        }
    }

    @Test
    void getConfig_withOtherPropertiesOverlapping() {
        var path = "edc.test";

        var extensionConfig = ConfigFactory.fromMap(Map.of("edc.test.entry1", "value1", "edc.test.entry2", "value2"));
        when(configuration.getConfig()).thenReturn(extensionConfig);
        System.setProperty("edc.test.entry2", "foo");

        context.initialize();

        var config = context.getConfig(path);

        try {
            assertThat(config.getString("entry1")).isEqualTo("value1");
            assertThat(config.getString("entry2")).isEqualTo("foo");
        } finally {
            System.clearProperty("edc.test.entry2");
        }
    }

    @Test
    void get_setting_returns_the_setting_from_the_configuration_extension() {
        when(configuration.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("key", "value")));
        context.initialize();

        var setting = context.getSetting("key", "default");

        assertThat(setting).isEqualTo("value");
    }

    @Test
    void get_setting_returns_default_value_if_setting_is_not_found() {
        when(configuration.getConfig()).thenReturn(ConfigFactory.empty());
        context.initialize();

        var setting = context.getSetting("key", "default");

        assertThat(setting).isEqualTo("default");
    }

    @Test
    @DisplayName("An environment variable in UPPER_SNAKE_CASE gets converted to dot-notation")
    void loadConfig_mapsSystemPropertyToJavaPropertyFormat() {
        when(configuration.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("key", "value")));
        context = Mockito.spy(context);
        when(context.getEnvironmentVariables()).thenReturn(Map.of("SOME_KEY", "env-val"));
        context.initialize();

        var setting = context.getSetting("key", "default");
        var setting2 = context.getSetting("some.key", null);

        assertThat(setting).isEqualTo("value");
        assertThat(setting2).isEqualTo("env-val");
    }

    @Test
    @DisplayName("An environment variable in UPPER_SNAKE_CASE should overwrite app config in dot-notation")
    void loadConfig_envOverwritesAppConfig() {
        when(configuration.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("some.key", "value1")));
        context = Mockito.spy(context);
        when(context.getEnvironmentVariables()).thenReturn(Map.of("SOME_KEY", "value2"));
        context.initialize();

        var setting = context.getSetting("some.key", null);

        assertThat(setting).isEqualTo("value2");
    }

    @Test
    @DisplayName("An environment variable in UPPER_SNAKE_CASE should get overwritten by system config in dot-notation")
    void loadConfig_systemPropOverwritesEnvVar() {

        System.setProperty("some.key", "value3");
        when(configuration.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("some.key", "value1")));
        context = Mockito.spy(context);
        when(context.getEnvironmentVariables()).thenReturn(Map.of("SOME_KEY", "value2"));
        context.initialize();

        var setting = context.getSetting("some.key", null);

        try {
            assertThat(setting).isEqualTo("value3");
        } finally {
            System.clearProperty("some.key");
        }
    }

    @Test
    void registerService_throwsWhenFrozen() {
        when(configuration.getConfig()).thenReturn(ConfigFactory.empty());
        context.initialize();

        context.freeze();
        assertThatThrownBy(() -> context.registerService(Object.class, new Object() {
        })).isInstanceOf(EdcException.class).hasMessageStartingWith("Cannot register service");
    }

    @Nested
    class GetRuntimeId {
        @Test
        void shouldReturnRandomUuid_whenNotConfigured() {
            context.initialize();

            var runtimeId = context.getRuntimeId();

            assertThat(UUID.fromString(runtimeId)).isNotNull();
            verify(monitor).warning(and(isA(String.class), argThat(message -> message.startsWith(RUNTIME_ID))));
        }

        @Test
        void shouldReturnConfiguredId_whenConfigured() {
            when(configuration.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(RUNTIME_ID, "runtime-id")));
            context.initialize();

            var runtimeId = context.getRuntimeId();

            assertThat(runtimeId).isEqualTo("runtime-id");
        }
    }

}
