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

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.boot.system.TestFunctions.getDeclaredField;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurationInjectionPointTest {

    private final ConfigurationObjectFactory configurationObjectFactory = mock();

    private final TestExtension targetInstance = new TestExtension();
    private final Field field = getDeclaredField(TestExtension.class, "configuration");
    private final ConfigurationInjectionPoint<TestExtension> injectionPoint = new ConfigurationInjectionPoint<>(
            targetInstance, field, configurationObjectFactory);

    @Test
    void getTargetInstance() {
        assertThat(injectionPoint.getTargetInstance()).isEqualTo(targetInstance);
    }

    @Test
    void getType() {
        assertThat(injectionPoint.getType()).isEqualTo(TestExtension.ConfigurationRecord.class);
    }

    @Test
    void isRequired() {
        assertThat(injectionPoint.isRequired()).isTrue();
    }

    @Test
    void setTargetValue() {
        var result = injectionPoint.setTargetValue(new TestExtension.ConfigurationRecord("newValue"));

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void getDefaultValueProvider() {
        assertThat(injectionPoint.getDefaultValueProvider()).isNull();
    }

    @Test
    void getProviders() {
        var context = new TestExtensionContext();
        context.setConfig(ConfigFactory.fromMap(Map.of("required", "value")));

        var result = injectionPoint.getProviders(Map.of(), context);

        assertThat(result).isSucceeded();
    }

    @Test
    void getProviders_shouldReportViolations_whenRequiredSettingMissing() {
        var context = new TestExtensionContext();
        context.setConfig(ConfigFactory.empty());

        var result = injectionPoint.getProviders(Map.of(), context);

        assertThat(result).isFailed().detail()
                .contains("ConfigurationRecord")
                .contains("configuration")
                .contains("requiredValue");
    }

    @Nested
    class Resolve {

        @Test
        void shouldPassEmptyPrefixToFactory_whenNoSettingContext() {
            var instance = "configuration";
            when(configurationObjectFactory.instantiate(any(), any(), any())).thenReturn(instance);
            var field = getDeclaredField(TestExtension.class, "configuration");
            var injectionPoint = new ConfigurationInjectionPoint<>(mock(), field, configurationObjectFactory);

            var resolve = injectionPoint.resolve(mock(), mock());

            assertThat(resolve).isSameAs(instance);
            verify(configurationObjectFactory).instantiate(any(), isNull(String.class), eq(TestExtension.ConfigurationRecord.class));
        }

        @Test
        void shouldPassPrefixToFactory_whenSettingContextSet() {
            var instance = "configuration";
            when(configurationObjectFactory.instantiate(any(), any(), any())).thenReturn(instance);
            var field = getDeclaredField(TestExtension.class, "configurationWithPrefix");
            var injectionPoint = new ConfigurationInjectionPoint<>(mock(), field, configurationObjectFactory);

            var resolve = injectionPoint.resolve(mock(), mock());

            assertThat(resolve).isSameAs(instance);
            verify(configurationObjectFactory).instantiate(any(), eq("prefix"), eq(TestExtension.ConfigurationRecord.class));
        }

    }

    private static class TestExtension implements ServiceExtension {

        @Configuration
        private ConfigurationRecord configuration;

        @SettingContext("prefix")
        @Configuration
        private ConfigurationRecord configurationWithPrefix;

        @Settings
        record ConfigurationRecord(
                @Setting(key = "required")
                String requiredValue
        ) { }
    }

}
