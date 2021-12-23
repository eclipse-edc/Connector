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
import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        assertThat(list).contains(service1);
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
        assertThat(list).containsExactlyInAnyOrder(service1, service2, coreExtension);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A DEFAULT service extension depends on a PRIMORDIAL one")
    void loadServiceExtensions_withBackwardsDependency() {
        var depending = new DependingService();
        var coreService = new SomeService();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService, coreExtension));

        var services = context.loadServiceExtensions();
        assertThat(services).containsExactly(coreExtension, coreService, depending);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }


    @Test
    @DisplayName("A service extension has a dependency on another one of the same loading stage")
    void loadServiceExtensions_withEqualDependency() {
        var depending = new DependingService() {
        };
        var coreService = new SomeService() {
        };

        var thirdService = new ServiceExtension() {
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, thirdService, coreService, coreExtension));

        var services = context.loadServiceExtensions();
        assertThat(services).containsExactlyInAnyOrder(coreService, depending, thirdService, coreExtension);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Two service extensions have a circular dependency")
    void loadServiceExtensions_withCircularDependency() {
        var s1 = new ServiceExtension() {
            @Override
            public Set<String> provides() {
                return Set.of("providedFeature");
            }

            @Override
            public Set<String> requires() {
                return Set.of("requiredFeature");
            }
        };
        var s2 = new ServiceExtension() {
            @Override
            public Set<String> provides() {
                return Set.of("requiredFeature");
            }

            @Override
            public Set<String> requires() {
                return Set.of("providedFeature");
            }
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(s1, s2, coreExtension));

        assertThatThrownBy(() -> context.loadServiceExtensions()).isInstanceOf(CyclicDependencyException.class);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A service extension has an unsatisfied dependency")
    void loadServiceExtensions_dependencyNotSatisfied() {
        var depending = new DependingService() {
            @Override
            public Set<String> requires() {
                return Set.of("no-one-provides-this");
            }
        };
        var coreService = new SomeService() {
            @Override
            public Set<String> provides() {
                return Set.of("no-one-cares-about-this");
            }
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService, coreExtension));

        assertThatThrownBy(() -> context.loadServiceExtensions())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Extension feature \"no-one-provides-this\" required by")
                .hasMessageEndingWith("not found");
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Services extensions are sorted by dependency order")
    void loadServiceExtensions_dependenciesAreSorted() {
        var depending = new DependingService() {
            @Override
            public Set<String> requires() {
                return Set.of("the-other");
            }
        };
        var testService = new SomeService() {
            @Override
            public Set<String> provides() {
                return Set.of("the-other");
            }
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, testService, coreExtension));

        var services = context.loadServiceExtensions();
        assertThat(services).containsExactly(coreExtension, testService, depending);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("Should throw exception when no core dependency found")
    void noCoreDependency_shouldThrowException() {
        var depending = new DependingService();
        var coreService = new SomeService();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService));

        assertThatThrownBy(() -> context.loadServiceExtensions()).isInstanceOf(EdcException.class);

    }

    @Test
    @DisplayName("Expands into child features if only a parent feature is required")
    void verifyExpandParentFeature() {
        var childService = new SomeChildService();
        var childService2 = new SomeOtherChildService();
        //the depending service requires the "core" feature, but the child services provide a
        //nested feature, "core:child:service" and "core:child"service:2"

        var dependingService = new DependingService();

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(childService, coreExtension, childService2, dependingService));

        var services = context.loadServiceExtensions();

        assertThat(services).containsExactly(coreExtension, childService, childService2, dependingService);

        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @SafeVarargs
    private <T> List<T> mutableListOf(T... elements) {
        return new ArrayList<>(List.of(elements));
    }

    @Feature("core")
    private interface CoreFeature {
    }

    @Feature("core:child:service")
    private interface ChildFeature {
    }

    @Feature("core:child:service:2")
    private interface ChildFeature2 {
    }

    @Requires(CoreFeature.class)
    private static class DependingService implements ServiceExtension {

    }

    @Provides(CoreFeature.class)
    private static class SomeService implements ServiceExtension {
    }

    @Provides(ChildFeature.class)
    private static class SomeChildService implements ServiceExtension {
    }

    @Provides(ChildFeature2.class)
    private static class SomeOtherChildService implements ServiceExtension {
    }

    @BaseExtension
    private static class TestCoreExtension implements ServiceExtension {

    }
}
