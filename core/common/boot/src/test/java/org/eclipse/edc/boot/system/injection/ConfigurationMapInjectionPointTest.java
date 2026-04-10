/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.eclipse.edc.boot.system.TestFunctions.getDeclaredField;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigurationMapInjectionPointTest {

    private final ConfigurationObjectFactory configurationObjectFactory = mock();
    private final TestExtension targetInstance = new TestExtension();
    private final Field field = getDeclaredField(TestExtension.class, "configurations");
    private final ConfigurationMapInjectionPoint<TestExtension> injectionPoint =
            new ConfigurationMapInjectionPoint<>(targetInstance, field, configurationObjectFactory);

    @Test
    void getType_isMap() {
        assertThat(injectionPoint.getType()).isEqualTo(Map.class);
    }

    @Test
    void isRequired_isFalse() {
        assertThat(injectionPoint.isRequired()).isFalse();
    }

    @Test
    void getDefaultValueProvider_isNull() {
        assertThat(injectionPoint.getDefaultValueProvider()).isNull();
    }

    @Test
    void getProviders_alwaysSuccess() {
        var context = mock(ServiceExtensionContext.class);
        doReturn(ConfigFactory.empty()).when(context).getConfig("prefix");

        var result = injectionPoint.getProviders(Map.of(), context);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void setTargetValue() {
        var map = Map.of("key1", new TestExtension.ConfigurationRecord("n"));

        var result = injectionPoint.setTargetValue(map);

        assertThat(result).isSucceeded();
        assertThat(targetInstance.getConfigurations()).isSameAs(map);
    }

    @Nested
    class Resolve {

        private final TestExtensionContext context = new TestExtensionContext();

        @Test
        void shouldReturnEmptyMap_whenNoSettings() {
            context.setConfig(ConfigFactory.empty());

            var result = injectionPoint.resolve(context, mock());

            assertThat(result).isInstanceOfSatisfying(Map.class, map -> assertThat(map).isEmpty());
        }

        @Test
        void resolve_singleEntry() {
            var key1 = new TestExtension.ConfigurationRecord("any");
            when(configurationObjectFactory.instantiate(any(), any(), any())).thenReturn(key1);
            context.setConfig(ConfigFactory.fromMap(Map.of("prefix.key1.required", "any")));

            var result = injectionPoint.resolve(context, mock());

            assertThat(result).isInstanceOf(Map.class)
                    .asInstanceOf(map(String.class, TestExtension.ConfigurationRecord.class))
                    .satisfies(configurations -> {
                        assertThat(configurations).hasSize(1).extractingByKey("key1")
                                .isInstanceOfSatisfying(TestExtension.ConfigurationRecord.class, entry -> {
                                    assertThat(entry).isSameAs(key1);
                                });
                    });
        }

        @Test
        void resolve_multipleEntries() {
            when(configurationObjectFactory.instantiate(any(), any(), any())).thenReturn(new TestExtension.ConfigurationRecord("any"));
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "prefix.key1.required", "value1",
                    "prefix.key2.required", "value2"
            )));

            var result = injectionPoint.resolve(context, mock());

            assertThat(result).isInstanceOf(Map.class)
                    .asInstanceOf(map(String.class, TestExtension.ConfigurationRecord.class))
                    .hasSize(2);
            verify(configurationObjectFactory, times(1)).instantiate(context, "prefix.key1", TestExtension.ConfigurationRecord.class);
            verify(configurationObjectFactory, times(1)).instantiate(context, "prefix.key2", TestExtension.ConfigurationRecord.class);
        }
    }

    private static class TestExtension implements ServiceExtension {

        @SettingContext("prefix")
        @Configuration
        private Map<String, ConfigurationRecord> configurations;

        public Map<String, ConfigurationRecord> getConfigurations() {
            return configurations;
        }

        @Settings
        public record ConfigurationRecord(
                @Setting(key = "required") String required) {
        }
    }

}
