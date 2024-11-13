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
import org.eclipse.edc.boot.system.testextensions.ExtensionWithConfigObject;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.system.TestFunctions.getDeclaredField;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurationInjectionPointTest {

    private final ExtensionWithConfigObject targetInstance = new ExtensionWithConfigObject();
    private final ConfigurationInjectionPoint<ExtensionWithConfigObject> injectionPoint = new ConfigurationInjectionPoint<>(targetInstance, getDeclaredField(ExtensionWithConfigObject.class, "configurationObject"));

    @Test
    void getTargetInstance() {
        assertThat(injectionPoint.getTargetInstance()).isEqualTo(targetInstance);
    }

    @Test
    void getType() {
        assertThat(injectionPoint.getType()).isEqualTo(ConfigurationObject.class);
    }

    @Test
    void isRequired() {
        assertThat(injectionPoint.isRequired()).isTrue();
        var ip = new ConfigurationInjectionPoint<>(targetInstance, getDeclaredField(ExtensionWithConfigObject.class, "optionalConfigurationObject"));
        assertThat(ip.isRequired()).isFalse();
    }

    @Test
    void setTargetValue() throws IllegalAccessException {
        assertThat(injectionPoint.setTargetValue(new ConfigurationObject("foo", 42L, null, 3.14159)).succeeded())
                .isTrue();
    }

    @Test
    void getDefaultValueProvider() {
        assertThat(injectionPoint.getDefaultValueProvider()).isNull();
    }

    @Test
    void setDefaultValueProvider() {
        //noop
    }

    @Test
    void resolve_record_isRequired_notResolved() {
        var context = mock(ServiceExtensionContext.class);
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of()));
        assertThatThrownBy(() -> injectionPoint.resolve(context, mock())).isInstanceOf(EdcInjectionException.class);
    }

    @Test
    void resolve_objectIsRecord() {
        var context = mock(ServiceExtensionContext.class);
        when(context.getMonitor()).thenReturn(mock());
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("foo.bar.baz", "asdf",
                "test.key3", "42")));

        var res = injectionPoint.resolve(context, mock());
        assertThat(res).isInstanceOf(ConfigurationObject.class);
    }

    @Test
    void resolve_objectIsClass() {
        var ip = new ConfigurationInjectionPoint<>(targetInstance, getDeclaredField(ExtensionWithConfigObject.class, "optionalConfigurationObject"));
        var context = mock(ServiceExtensionContext.class);
        when(context.getMonitor()).thenReturn(mock());
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("optional.key", "asdf")));

        var result = ip.resolve(context, mock());

        assertThat(result).isInstanceOf(ExtensionWithConfigObject.OptionalConfigurationObject.class);
        assertThat(((ExtensionWithConfigObject.OptionalConfigurationObject) result).getVal()).isEqualTo("asdf");
    }

    @Test
    void getProviders() {
        var context = mock(ServiceExtensionContext.class);
        when(context.getMonitor()).thenReturn(mock());
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("foo.bar.baz", "asdf",
                "test.key3", "42")));

        assertThat(injectionPoint.getProviders(Map.of(), context).succeeded()).isTrue();
    }

    @Test
    void getProviders_hasViolations() {
        var context = mock(ServiceExtensionContext.class);
        when(context.getMonitor()).thenReturn(mock());
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of()));

        var result = injectionPoint.getProviders(Map.of(), context);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureDetail()).isEqualTo("Configuration object \"configurationObject\" of type [class org.eclipse.edc.boot.system.testextensions.ConfigurationObject], " +
                                                        "through nested settings [Configuration value \"requiredVal\" of type [class java.lang.String] (property 'foo.bar.baz')]");
    }
}