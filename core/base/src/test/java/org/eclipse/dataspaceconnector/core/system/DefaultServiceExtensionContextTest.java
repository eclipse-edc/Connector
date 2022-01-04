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

package org.eclipse.dataspaceconnector.core.system;

import org.eclipse.dataspaceconnector.core.BaseExtension;
import org.eclipse.dataspaceconnector.core.util.CyclicDependencyException;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Provides({ SomeObject.class })
    private static class ProvidingExtension implements ServiceExtension {
    }

    @Provides({ SomeObject.class })
    private static class TestProvidingExtension implements ServiceExtension {
        @Inject
        AnotherObject obj;
    }

    @Provides({ AnotherObject.class })
    private static class TestProvidingExtension2 implements ServiceExtension {
        @Inject
        SomeObject obj;
    }


    private static class SomeObject {
    }

    private static class AnotherObject {
    }

    @BaseExtension
    private static class TestCoreExtension implements ServiceExtension {

    }
}
