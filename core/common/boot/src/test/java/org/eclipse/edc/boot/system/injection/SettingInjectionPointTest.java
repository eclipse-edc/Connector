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
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.system.TestFunctions.getDeclaredField;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettingInjectionPointTest {

    abstract class Tests {
        protected abstract SettingInjectionPoint<?> getInjectionPoint();

        protected abstract Object getTargetObject();

        @Test
        void getType() {
            assertThat(getInjectionPoint().getType()).isEqualTo(String.class);
        }

        @Test
        void isRequired() {
            assertThat(getInjectionPoint().isRequired()).isTrue();
            var ip2 = createInjectionPoint(getTargetObject(), "optionalVal");
            assertThat(ip2.isRequired()).isFalse();
        }

        @Test
        void getDefaultServiceProvider() {
            assertThat(getInjectionPoint().getDefaultValueProvider()).isNull();
        }

        @Nested
        class Resolve {

            private final TestExtensionContext context = new TestExtensionContext();

            @Test
            void shouldThrowException_whenNoValueForRequiredWithoutDefault() {
                context.setConfig(ConfigFactory.empty());
                var injectionPoint = createInjectionPoint(getTargetObject(), "requiredVal");

                assertThatThrownBy(() -> injectionPoint.resolve(context, mock()))
                        .isInstanceOf(EdcInjectionException.class)
                        .hasMessageContaining("No config value and no default value found for injected field");
            }

            @Test
            void shouldResolve_whenNoValueForRequiredWithDefault() {
                context.setConfig(ConfigFactory.empty());
                var injectionPoint = createInjectionPoint(getTargetObject(), "requiredValWithDefault");

                var resolved = injectionPoint.resolve(context, new InjectionPointDefaultServiceSupplier());

                assertThat(resolved).isEqualTo(ExtensionWithConfigValue.DEFAULT_VALUE);
            }

            @Test
            void shouldThrowException_whenDefaultWrongType() {
                context.setConfig(ConfigFactory.empty());

                var injectionPoint = createInjectionPoint(getTargetObject(), "requiredDoubleValue");

                assertThatThrownBy(() -> injectionPoint.resolve(context, mock())).isInstanceOf(EdcInjectionException.class);
            }

            @Test
            void shouldResolve() {
                var config = ConfigFactory.fromMap(Map.of("test.key", "test.value"));
                context.setConfig(config);
                var injectionPoint = createInjectionPoint(getTargetObject(), "requiredVal");

                var resolved = injectionPoint.resolve(context, mock());

                assertThat(resolved).isEqualTo("test.value");
            }

            @Test
            void shouldThrowException_whenWrongType() {
                var config = ConfigFactory.fromMap(Map.of("test.key3", "this-should-be-a-double"));
                context.setConfig(config);
                var injectionPoint = createInjectionPoint(getTargetObject(), "requiredDoubleValue");

                assertThatThrownBy(() -> injectionPoint.resolve(context, mock())).isInstanceOf(EdcInjectionException.class);
            }

            @Test
            void shouldResolveNull_whenNotRequiredWithoutSetting() {
                context.setConfig(ConfigFactory.empty());
                var injectionPoint = createInjectionPoint(getTargetObject(), "optionalVal");

                var resolved = injectionPoint.resolve(context, mock());

                assertThat(resolved).isNull();
            }

            @Test
            void shouldResolveConfigObject() {
                context.setConfig(ConfigFactory.fromMap(Map.of(
                        "subconfig.key", "value",
                        "other.settings", "any"
                )));
                var injectionPoint = createInjectionPoint(getTargetObject(), "subconfig");

                var resolved = injectionPoint.resolve(context, mock());

                assertThat(resolved).isInstanceOfSatisfying(Config.class, subconfig -> {
                    assertThat(subconfig.getEntries()).hasSize(1);
                    assertThat(subconfig.currentNode()).isEqualTo("subconfig");
                    assertThat(subconfig.getString("key")).isEqualTo("value");
                });
            }
        }

        @Test
        void isSatisfiedBy_whenOptional() {
            var ip = createInjectionPoint(getTargetObject(), "optionalVal");
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);
            assertThat(ip.getProviders(Map.of(), contextMock).succeeded()).isTrue();
        }

        @Test
        void isSatisfiedBy_whenRequired_satisfiedByDefaultValue() {
            var ip = createInjectionPoint(getTargetObject(), "requiredValWithDefault");
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);

            assertThat(ip.getProviders(Map.of(), contextMock).succeeded()).isTrue();
        }

        @Test
        void isSatisfiedBy_whenRequired_satisfiedByConfig() {
            var ip = createInjectionPoint(getTargetObject(), "requiredVal");
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of("test.key", "test.value"));
            when(contextMock.getConfig()).thenReturn(config);

            assertThat(ip.getProviders(Map.of(), contextMock).succeeded()).isTrue();
        }

        @Test
        void isSatisfiedBy_whenRequired_notSatisfied() {
            var ip = createInjectionPoint(getTargetObject(), "requiredVal");
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);

            assertThat(ip.getProviders(Map.of(), contextMock).succeeded()).isFalse();
        }

        protected SettingInjectionPoint<?> createInjectionPoint(Object targetObject, String fieldName) {
            var field = getDeclaredField(ExtensionWithConfigValue.class, fieldName);
            return new SettingInjectionPoint<>(targetObject, field);
        }
    }

    @Nested
    class DeclaredOnExtension extends Tests {

        private final ExtensionWithConfigValue targetObject = new ExtensionWithConfigValue();
        private final SettingInjectionPoint<?> injectionPoint = createInjectionPoint(targetObject, "requiredVal");

        @Test
        void getTargetInstance() {
            assertThat(injectionPoint.getTargetInstance()).isEqualTo(targetObject);
        }

        @Test
        void setTargetValue() throws IllegalAccessException, NoSuchFieldException {
            assertThat(getInjectionPoint().setTargetValue("test").succeeded()).isTrue();

            var field = getTargetObject().getClass().getDeclaredField("requiredVal"); // yes, it's a bit dirty...
            field.setAccessible(true);
            assertThat(field.get(getTargetObject()))
                    .isEqualTo("test");
        }

        @Override
        protected SettingInjectionPoint<?> getInjectionPoint() {
            return injectionPoint;
        }

        @Override
        protected Object getTargetObject() {
            return targetObject;
        }
    }

    @Nested
    class DeclaredOnConfigObject extends Tests {
        private final SettingInjectionPoint<?> injectionPoint = createInjectionPoint(null, "requiredVal");

        @Test
        void getTargetInstance() {
            assertThat(injectionPoint.getTargetInstance()).isNull();
        }

        @Test
        void setTargetValue() {
            assertThat(getInjectionPoint().setTargetValue("test").succeeded()).isFalse();
        }

        @Override
        protected SettingInjectionPoint<?> getInjectionPoint() {
            return injectionPoint;
        }

        @Override
        protected Object getTargetObject() {
            return null;
        }
    }

    public static class ExtensionWithConfigValue implements ServiceExtension {

        public static final String DEFAULT_VALUE = "default-value";

        @Setting(key = "test.key")
        private String requiredVal;

        @Setting(key = "test.optional.value", required = false)
        private Long optionalVal;

        @Setting(key = "test.key2", defaultValue = DEFAULT_VALUE)
        private String requiredValWithDefault;

        @Setting(key = "test.key3", defaultValue = DEFAULT_VALUE)
        private Double requiredDoubleValue;

        @Setting(key = "test.duration", defaultValue = "PT1H")
        private Duration duration;

        @Setting(key = "subconfig")
        private Config subconfig;
    }
}
