/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.system.injection;

import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InjectionContainerTest {

    private InjectionContainer<ServiceExtension> container;
    private ServiceExtensionContext contextMock;
    private TestExtension testExtension;

    @BeforeEach
    void setup() {
        contextMock = mock(ServiceExtensionContext.class);
        testExtension = new TestExtension();
    }

    @Test
    void validate_allSatisfied() {
        when(contextMock.hasService(FooService.class)).thenReturn(true);
        when(contextMock.hasService(BarService.class)).thenReturn(true);

        container = new InjectionContainer<>(testExtension, Collections.emptySet());

        assertThat(container.validate(contextMock).succeeded()).isTrue();
    }

    @Test
    void validate_notAllSatisfied() {
        when(contextMock.hasService(FooService.class)).thenReturn(true);
        when(contextMock.hasService(BarService.class)).thenReturn(false);

        container = new InjectionContainer<>(testExtension, Collections.emptySet());

        var result = container.validate(contextMock);
        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).containsExactly(BarService.class.getName());
    }

    @Test
    @Disabled
    void validate_notSatisfiedByTarget() {
        // this test will be used to differentiate whether a service was registered by _this_ injection target or by another which
        // would indicate an implementation error too.
    }

    @Test
    void validate_noProvidesAnnotation() {
        when(contextMock.hasService(FooService.class)).thenReturn(true);
        when(contextMock.hasService(BarService.class)).thenReturn(true);

        container = new InjectionContainer<>(new ServiceExtension() {
        }, Collections.emptySet());

        assertThat(container.validate(contextMock).succeeded()).isTrue();
    }

    private interface FooService {
    }

    private interface BarService {
    }

    @Provides({FooService.class, BarService.class})
    private static class TestExtension implements ServiceExtension {
    }
}
