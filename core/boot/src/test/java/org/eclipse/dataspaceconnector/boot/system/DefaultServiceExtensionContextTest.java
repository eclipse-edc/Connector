/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.boot.system;

import org.eclipse.dataspaceconnector.boot.util.CyclicDependencyException;
import org.eclipse.dataspaceconnector.core.BaseExtension;
import org.eclipse.dataspaceconnector.core.config.ConfigFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Config;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.EdcInjectionException;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultServiceExtensionContextTest {

    private final ServiceExtension coreExtension = new TestCoreExtension();
    private ServiceExtensionContext context;
    private ServiceLocator serviceLocatorMock;

    @BeforeEach
    void setUp() {
        TypeManager typeManager = new TypeManager();
        Monitor monitor = mock(Monitor.class);
        serviceLocatorMock = mock(ServiceLocator.class);
        context = new DefaultServiceExtensionContext(typeManager, monitor, serviceLocatorMock);
    }

    @Test
    void getConfig_onlyFromConfig() {
        var path = "edc.test";

        var configExtMock = mock(ConfigurationExtension.class);
        Config extensionConfig = ConfigFactory.fromMap(Map.of("edc.test.entry1", "value1", "edc.test.entry2", "value2"));
        when(configExtMock.getConfig()).thenReturn(extensionConfig);
        when(serviceLocatorMock.loadImplementors(eq(ConfigurationExtension.class), anyBoolean())).thenReturn(List.of(configExtMock));
        context.initialize();

        var config = context.getConfig(path);

        assertThat(config.getString("entry1")).isEqualTo("value1");
        assertThat(config.getString("entry2")).isEqualTo("value2");
    }

    @Test
    void getConfig_withOtherProperties() {
        var path = "edc.test";

        var configExtMock = mock(ConfigurationExtension.class);
        Config extensionConfig = ConfigFactory.fromMap(Map.of("edc.test.entry1", "value1", "edc.test.entry2", "value2"));
        when(configExtMock.getConfig()).thenReturn(extensionConfig);
        when(serviceLocatorMock.loadImplementors(eq(ConfigurationExtension.class), anyBoolean())).thenReturn(List.of(configExtMock));
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

        var configExtMock = mock(ConfigurationExtension.class);
        Config extensionConfig = ConfigFactory.fromMap(Map.of("edc.test.entry1", "value1", "edc.test.entry2", "value2"));
        when(configExtMock.getConfig()).thenReturn(extensionConfig);
        when(serviceLocatorMock.loadImplementors(eq(ConfigurationExtension.class), anyBoolean())).thenReturn(List.of(configExtMock));
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
    @DisplayName("No dependencies between service extensions")
    void loadServiceExtensions_noDependencies() {

        var service1 = new ServiceExtension() {
        };
        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(service1, coreExtension));

        var list = context.loadServiceExtensions();

        assertThat(list).hasSize(2);
        assertThat(list).extracting(InjectionContainer::getInjectionTarget).contains(service1);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Locating two service extensions for the same service class ")
    void loadServiceExtensions_whenMultipleServices() {
        var service1 = new ServiceExtension() {
        };
        var service2 = new ServiceExtension() {
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(service1, service2, coreExtension));

        var list = context.loadServiceExtensions();
        assertThat(list).hasSize(3);
        assertThat(list).extracting(InjectionContainer::getInjectionTarget).containsExactlyInAnyOrder(service1, service2, coreExtension);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A DEFAULT service extension depends on a PRIMORDIAL one")
    void loadServiceExtensions_withBackwardsDependency() {
        var depending = new DependingExtension();
        var someExtension = new SomeExtension();
        var providing = new ProvidingExtension();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(providing, depending, someExtension, coreExtension));

        var services = context.loadServiceExtensions();
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providing, depending, someExtension);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }


    @Test
    @DisplayName("A service extension has a dependency on another one of the same loading stage")
    void loadServiceExtensions_withEqualDependency() {
        var depending = new DependingExtension() {
        };
        var coreService = new SomeExtension() {
        };

        var thirdService = new ServiceExtension() {
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, thirdService, coreService, coreExtension));

        var services = context.loadServiceExtensions();
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactlyInAnyOrder(coreService, depending, thirdService, coreExtension);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Two service extensions have a circular dependency")
    void loadServiceExtensions_withCircularDependency() {
        var s1 = new TestProvidingExtension2();
        var s2 = new TestProvidingExtension();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(s1, s2, coreExtension));

        assertThatThrownBy(() -> context.loadServiceExtensions()).isInstanceOf(CyclicDependencyException.class);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A service extension has an unsatisfied dependency")
    void loadServiceExtensions_dependencyNotSatisfied() {
        var depending = new DependingExtension();
        var someExtension = new SomeExtension();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, someExtension, coreExtension));

        assertThatThrownBy(() -> context.loadServiceExtensions()).isInstanceOf(EdcException.class).hasMessageContaining("The following injected fields were not provided:\nField \"someService\" of type ");
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Services extensions are sorted by dependency order")
    void loadServiceExtensions_dependenciesAreSorted() {
        var depending = new DependingExtension();
        var providingExtension = new ProvidingExtension();


        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, providingExtension, coreExtension));

        var services = context.loadServiceExtensions();
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providingExtension, depending);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Should throw exception when no core dependency found")
    void noCoreDependency_shouldThrowException() {
        var depending = new DependingExtension();
        var coreService = new SomeExtension();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService));

        assertThatThrownBy(() -> context.loadServiceExtensions()).isInstanceOf(EdcException.class);

    }

    @Test
    @DisplayName("Requires annotation influences ordering")
    void loadServiceExtensions_withAnnotation() {
        var depending = new DependingExtension();
        var providingExtension = new ProvidingExtension();
        var annotatedExtension = new AnnotatedExtension();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, annotatedExtension, providingExtension, coreExtension));

        var services = context.loadServiceExtensions();
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providingExtension, depending, annotatedExtension);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Requires annotation not satisfied")
    void loadServiceExtensions_withAnnotation_notSatisfied() {
        var annotatedExtension = new AnnotatedExtension();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(annotatedExtension, coreExtension));

        assertThatThrownBy(() -> context.loadServiceExtensions()).isNotInstanceOf(EdcInjectionException.class).isInstanceOf(EdcException.class);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Mixed requirement features work")
    void loadServiceExtensions_withMixedInjectAndAnnotation() {
        var providingExtension = new ProvidingExtension(); // provides SomeObject
        var anotherProvidingExt = new AnotherProvidingExtension(); //provides AnotherObject
        var mixedAnnotation = new MixedAnnotation();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(mixedAnnotation, providingExtension, coreExtension, anotherProvidingExt));

        var services = context.loadServiceExtensions();
        assertThat(services).extracting(InjectionContainer::getInjectionTarget).containsExactly(coreExtension, providingExtension, anotherProvidingExt, mixedAnnotation);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Mixed requirement features introducing circular dependency")
    void loadServiceExtensions_withMixedInjectAndAnnotation_withCircDependency() {
        var s1 = new TestProvidingExtension3();
        var s2 = new TestProvidingExtension();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(s1, s2, coreExtension));

        assertThatThrownBy(() -> context.loadServiceExtensions()).isInstanceOf(CyclicDependencyException.class);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    void get_setting_returns_the_setting_from_the_configuration_extension() {
        var configuration = mock(ConfigurationExtension.class);
        when(configuration.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of("key", "value")));
        when(serviceLocatorMock.loadImplementors(ConfigurationExtension.class, false)).thenReturn(List.of(configuration));
        context.initialize();

        var setting = context.getSetting("key", "default");

        assertThat(setting).isEqualTo("value");
    }

    @Test
    void get_setting_returns_default_value_if_setting_is_not_found() {
        var configuration = mock(ConfigurationExtension.class);
        when(configuration.getConfig()).thenReturn(ConfigFactory.empty());
        when(serviceLocatorMock.loadImplementors(ConfigurationExtension.class, false)).thenReturn(List.of(configuration));
        context.initialize();

        var setting = context.getSetting("key", "default");

        assertThat(setting).isEqualTo("default");
    }

    @SafeVarargs
    private <T> List<T> mutableListOf(T... elements) {
        return new ArrayList<>(List.of(elements));
    }

    private static class DependingExtension implements ServiceExtension {
        @Inject
        private SomeObject someService;
    }

    private static class SomeExtension implements ServiceExtension {
    }

    @Provides({SomeObject.class})
    private static class ProvidingExtension implements ServiceExtension {
    }

    @Provides(AnotherObject.class)
    private static class AnotherProvidingExtension implements ServiceExtension {
    }

    @Provides({SomeObject.class})
    private static class TestProvidingExtension implements ServiceExtension {
        @Inject
        AnotherObject obj;
    }

    @Provides({AnotherObject.class})
    private static class TestProvidingExtension2 implements ServiceExtension {
        @Inject
        SomeObject obj;
    }

    @Provides({AnotherObject.class})
    @Requires({SomeObject.class})
    private static class TestProvidingExtension3 implements ServiceExtension {
    }

    @Requires(SomeObject.class)
    private static class AnnotatedExtension implements ServiceExtension {
    }

    @Requires(SomeObject.class)
    private static class MixedAnnotation implements ServiceExtension {
        @Inject
        private AnotherObject obj;
    }

    private static class SomeObject {
    }

    private static class AnotherObject {
    }

    @BaseExtension
    private static class TestCoreExtension implements ServiceExtension {

    }
}
