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

package org.eclipse.dataspaceconnector.system;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.util.CyclicDependencyException;
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
        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(service1));

        var list = context.loadServiceExtensions();

        assertThat(list).hasSize(1);
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

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(service1, service2));

        var list = context.loadServiceExtensions();
        assertThat(list).hasSize(2);
        assertThat(list).containsExactlyInAnyOrder(service1, service2);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A DEFAULT service extension depends on a PRIMORDIAL one")
    void loadServiceExtensions_withBackwardsDependency() {
        var depending = new DependingService() {
            @Override
            public LoadPhase phase() {
                return LoadPhase.DEFAULT;
            }
        };
        var coreService = new CoreService() {
            @Override
            public LoadPhase phase() {
                return LoadPhase.PRIMORDIAL;
            }
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService));

        var services = context.loadServiceExtensions();
        assertThat(services).containsExactly(coreService, depending);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A PRIMORDIAL service extension depends on a DEFAULT one - should fail")
    void loadServiceExtensions_withForwardsDependency() {
        var depending = new DependingService() {
            @Override
            public LoadPhase phase() {
                return LoadPhase.PRIMORDIAL;
            }
        };
        var coreService = new CoreService() {
            @Override
            public LoadPhase phase() {
                return LoadPhase.DEFAULT;
            }
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService));

        assertThatThrownBy(() -> context.loadServiceExtensions()).isInstanceOf(EdcException.class);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    @Test
    @DisplayName("A service extension has a dependency on another one of the same loading stage")
    void loadServiceExtensions_withEqualDependency() {
        var depending = new DependingService() {
            @Override
            public LoadPhase phase() {
                return LoadPhase.DEFAULT;
            }
        };
        var coreService = new CoreService() {
            @Override
            public LoadPhase phase() {
                return LoadPhase.DEFAULT;
            }
        };

        var thirdService = new ServiceExtension() {
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, thirdService, coreService));

        var services = context.loadServiceExtensions();
        assertThat(services).containsExactlyInAnyOrder(coreService, depending, thirdService);
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

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(s1, s2));

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
        var coreService = new CoreService() {
            @Override
            public Set<String> provides() {
                return Set.of("no-one-cares-about-this");
            }
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService));

        assertThatThrownBy(() -> context.loadServiceExtensions())
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("not found: no-one-provides-this");
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
        var coreService = new CoreService() {
            @Override
            public Set<String> provides() {
                return Set.of("the-other");
            }
        };

        when(serviceLocatorMock.loadImplementors(eq(ServiceExtension.class), anyBoolean())).thenReturn(mutableListOf(depending, coreService));

        var services = context.loadServiceExtensions();
        assertThat(services).containsExactly(coreService, depending);
        verify(serviceLocatorMock).loadImplementors(eq(ServiceExtension.class), anyBoolean());
    }

    private <T> List<T> mutableListOf(T... elements) {
        return new ArrayList<>(List.of(elements));
    }

    private abstract static class DependingService implements ServiceExtension {

        @Override
        public Set<String> requires() {
            return Set.of("core");
        }
    }

    private abstract static class CoreService implements ServiceExtension {
        @Override
        public Set<String> provides() {
            return Set.of("core");
        }
    }
}
