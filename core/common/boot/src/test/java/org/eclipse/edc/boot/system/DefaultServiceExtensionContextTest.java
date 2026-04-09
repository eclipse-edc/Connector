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
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.BootServicesExtension.COMPONENT_ID;
import static org.eclipse.edc.boot.BootServicesExtension.RUNTIME_ID;
import static org.mockito.Mockito.mock;

class DefaultServiceExtensionContextTest {

    private final ConfigurationExtension configuration = mock();
    private final Monitor monitor = mock();
    private final Config config = mock();

    @Test
    void get_setting_returns_the_setting_from_the_configuration_extension() {
        var config = ConfigFactory.fromMap(Map.of("key", "value"));
        var context = createContext(config);

        var setting = context.getSetting("key", "default");

        assertThat(setting).isEqualTo("value");
    }

    @Test
    void get_setting_returns_default_value_if_setting_is_not_found() {
        var context = createContext(ConfigFactory.empty());

        var setting = context.getSetting("key", "default");

        assertThat(setting).isEqualTo("default");
    }

    @Test
    void registerService_throwsWhenFrozen() {
        var context = createContext(ConfigFactory.empty());

        context.freeze();
        assertThatThrownBy(() -> context.registerService(Object.class, new Object() {
        })).isInstanceOf(EdcException.class).hasMessageStartingWith("Cannot register service");
    }

    @Nested
    class GetRuntimeId {

        @Test
        void shouldReturnRandomUuid_whenNotConfigured() {
            var context = createContext(ConfigFactory.empty());

            var runtimeId = context.getRuntimeId();
            assertThat(UUID.fromString(runtimeId)).isNotNull();
        }

        @Test
        void shouldReturnConfiguredRuntimeId() {
            var context = createContext(ConfigFactory.fromMap(Map.of(RUNTIME_ID, "runtime-id")));

            var runtimeId = context.getRuntimeId();

            assertThat(runtimeId).isEqualTo("runtime-id");
        }

    }

    @Nested
    class GetComponentId {

        @Test
        void shouldReturnRandomUuid_whenNotConfigured() {
            var context = createContext(ConfigFactory.empty());

            var componentId = context.getComponentId();
            assertThat(UUID.fromString(componentId)).isNotNull();

        }

        @Test
        void shouldUseRuntimeId_whenComponentIdNotConfigured() {
            var context = createContext(ConfigFactory.fromMap(Map.of(RUNTIME_ID, "runtime-id")));

            var componentId = context.getComponentId();

            assertThat(componentId).isEqualTo("runtime-id");
        }

        @Test
        void shouldUseConfiguredValue_whenBothAreConfigured() {
            var context = createContext(ConfigFactory.fromMap(Map.of(RUNTIME_ID, "runtime-id", COMPONENT_ID, "component-id")));

            var componentId = context.getComponentId();
            var runtimeId = context.getRuntimeId();

            assertThat(runtimeId).isEqualTo("runtime-id");
            assertThat(componentId).isEqualTo("component-id");
        }

    }

    private ServiceExtensionContext createContext(Config config) {
        var context = new DefaultServiceExtensionContext(monitor, config);
        context.initialize();
        return context;
    }
}
