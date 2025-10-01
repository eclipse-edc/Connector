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

import org.eclipse.edc.boot.system.testextensions.ConfigurationObject;
import org.eclipse.edc.boot.system.testextensions.ExtensionWithConfigValue;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            var ip2 = createInjectionPoint(getTargetObject(), "optionalVal", Long.class);
            assertThat(ip2.isRequired()).isFalse();
        }


        @Test
        void getDefaultServiceProvider() {
            assertThat(getInjectionPoint().getDefaultValueProvider()).isNull();
        }

        @Test
        void setDefaultServiceProvider() {
            //noop
        }

        @Test
        void resolve_whenRequired_andNotFound_andNoDefault_expectException() {
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);

            assertThatThrownBy(() -> getInjectionPoint().resolve(contextMock, mock()))
                    .isInstanceOf(EdcInjectionException.class)
                    .hasMessageContaining("No config value and no default value found for injected field");
        }

        @Test
        void resolve_whenRequired_andNotFound_hasDefault() {
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);
            when(contextMock.getMonitor()).thenReturn(mock());
            var ip = createInjectionPoint(getInjectionPoint(), "requiredValWithDefault", ExtensionWithConfigValue.class);
            assertThat(ip.resolve(contextMock, new InjectionPointDefaultServiceSupplier())).isEqualTo(ExtensionWithConfigValue.DEFAULT_VALUE);
        }

        @Test
        void resolve_whenRequired_andNotFound_hasDefaultOfWrongType() {
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);
            when(contextMock.getMonitor()).thenReturn(mock());
            var ip = createInjectionPoint(getTargetObject(), "requiredDoubleVal", ExtensionWithConfigValue.class);
            assertThatThrownBy(() -> ip.resolve(contextMock, mock())).isInstanceOf(EdcInjectionException.class);
        }

        @Test
        void resolve_whenRequired_andHasConfig() {
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of("test.key", "test.value"));
            when(contextMock.getConfig()).thenReturn(config);
            assertThat(getInjectionPoint().resolve(contextMock, mock())).isEqualTo("test.value");
        }

        @Test
        void resolve_whenRequired_andHasConfigOfWrongType() {
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of("test.key3", "this-should-be-a-double"));
            when(contextMock.getConfig()).thenReturn(config);
            assertThatThrownBy(() -> getInjectionPoint().resolve(contextMock, mock())).isInstanceOf(EdcInjectionException.class);
        }

        @Test
        void resolve_whenNotRequired_notFound_shouldReturnNull() {
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);
            var ip = createInjectionPoint(getTargetObject(), "optionalVal", ExtensionWithConfigValue.class);
            assertThat(ip.resolve(contextMock, mock())).isNull();
        }

        @Test
        void isSatisfiedBy_whenOptional() {
            var ip = createInjectionPoint(getTargetObject(), "optionalVal", ExtensionWithConfigValue.class);
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);
            assertThat(ip.getProviders(Map.of(), contextMock).succeeded()).isTrue();
        }

        @Test
        void isSatisfiedBy_whenRequired_satisfiedByDefaultValue() {
            var ip = createInjectionPoint(getTargetObject(), "requiredValWithDefault", ExtensionWithConfigValue.class);
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);

            assertThat(ip.getProviders(Map.of(), contextMock).succeeded()).isTrue();
        }

        @Test
        void isSatisfiedBy_whenRequired_satisfiedByConfig() {
            var ip = createInjectionPoint(getTargetObject(), "requiredVal", ExtensionWithConfigValue.class);
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of("test.key", "test.value"));
            when(contextMock.getConfig()).thenReturn(config);

            assertThat(ip.getProviders(Map.of(), contextMock).succeeded()).isTrue();
        }

        @Test
        void isSatisfiedBy_whenRequired_notSatisfied() {
            var ip = createInjectionPoint(getTargetObject(), "requiredVal", ExtensionWithConfigValue.class);
            var contextMock = mock(ServiceExtensionContext.class);
            var config = ConfigFactory.fromMap(Map.of());
            when(contextMock.getConfig()).thenReturn(config);

            assertThat(ip.getProviders(Map.of(), contextMock).succeeded()).isFalse();
        }
    }

    @Nested
    class DeclaredOnExtension extends Tests {

        private final ExtensionWithConfigValue targetObject = new ExtensionWithConfigValue();
        private final SettingInjectionPoint<?> injectionPoint = createInjectionPoint(targetObject, "requiredVal", targetObject.getClass());

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
        private final SettingInjectionPoint<?> injectionPoint = createInjectionPoint(null, "requiredVal", ConfigurationObject.class);

        @Test
        void getTargetInstance() {
            assertThat(injectionPoint.getTargetInstance()).isNull();
        }

        @Test
        void setTargetValue() throws IllegalAccessException {
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


    private SettingInjectionPoint<?> createInjectionPoint(Object targetObject, String fieldName, Class<?> targetClass) {
        var field = getDeclaredField(ExtensionWithConfigValue.class, fieldName);
        return new SettingInjectionPoint<>(targetObject, field, field.getAnnotation(Setting.class), targetClass);
    }

}
