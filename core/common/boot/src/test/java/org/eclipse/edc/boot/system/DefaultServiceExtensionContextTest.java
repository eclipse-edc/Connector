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
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.BootServicesExtension.COMPONENT_ID;
import static org.eclipse.edc.boot.BootServicesExtension.RUNTIME_ID;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultServiceExtensionContextTest {

    private final ConfigurationExtension configuration = mock();
    private final Monitor monitor = mock();
    private final Config config = mock();
    private DefaultServiceExtensionContext context;

    @BeforeEach
    void setUp() {
        context = new DefaultServiceExtensionContext(monitor, config);
    }

    @Test
    void get_setting_returns_the_setting_from_the_configuration_extension() {
        when(config.getConfig(any())).thenReturn(ConfigFactory.fromMap(Map.of("key", "value")));
        context.initialize();

        var setting = context.getSetting("key", "default");

        assertThat(setting).isEqualTo("value");
    }

    @Test
    void get_setting_returns_default_value_if_setting_is_not_found() {
        when(config.getConfig(any())).thenReturn(ConfigFactory.empty());
        context.initialize();

        var setting = context.getSetting("key", "default");

        assertThat(setting).isEqualTo("default");
    }

    @Test
    void registerService_throwsWhenFrozen() {
        when(config.getConfig(any())).thenReturn(ConfigFactory.empty());
        context.initialize();

        context.freeze();
        assertThatThrownBy(() -> context.registerService(Object.class, new Object() {
        })).isInstanceOf(EdcException.class).hasMessageStartingWith("Cannot register service");
    }

    @Nested
    class GetRuntimeId {
        @Test
        void shouldReturnRandomUuid_whenNotConfigured_andComponentIdNull() {
            when(config.getConfig(any())).thenReturn(ConfigFactory.empty());
            context.initialize();

            var runtimeId = context.getRuntimeId();
            assertThat(UUID.fromString(runtimeId)).isNotNull();

            verify(monitor, times(2)).warning(and(isA(String.class), argThat(message -> !message.contains(RUNTIME_ID))));
        }


        @Test
        void shouldReturnRandomId_whenComponentIdNotConfigured() {
            when(config.getConfig(any())).thenReturn(ConfigFactory.fromMap(Map.of(RUNTIME_ID, "runtime-id")));
            context.initialize();

            var runtimeId = context.getRuntimeId();

            assertThat(UUID.fromString(runtimeId)).isNotNull();
        }
    }

    @Nested
    class GetComponentId {
        @Test
        void shouldReturnRandomUuid_whenNotConfigured() {
            when(config.getConfig(any())).thenReturn(ConfigFactory.empty());
            context.initialize();

            var componentId = context.getComponentId();
            assertThat(UUID.fromString(componentId)).isNotNull();

            verify(monitor).warning(and(isA(String.class), argThat(message -> !message.contains(COMPONENT_ID))));
        }


        @Test
        void shouldUseRuntimeId_whenComponentIdNotConfigured() {
            when(config.getConfig(any())).thenReturn(ConfigFactory.fromMap(Map.of(RUNTIME_ID, "runtime-id")));
            context.initialize();
            var componentId = context.getComponentId();
            assertThat(componentId).isEqualTo("runtime-id");
        }


        @Test
        void shouldUseConfiguredValue_whenBothAreConfigured() {
            when(config.getConfig(any())).thenReturn(ConfigFactory.fromMap(Map.of(RUNTIME_ID, "runtime-id", COMPONENT_ID, "component-id")));

            context.initialize();
            var componentId = context.getComponentId();
            var runtimeId = context.getRuntimeId();

            assertThat(runtimeId).isEqualTo("runtime-id");
            assertThat(componentId).isEqualTo("component-id");
            verify(monitor).warning(and(isA(String.class), argThat(message -> !message.contains(RUNTIME_ID))));
        }
    }

}
