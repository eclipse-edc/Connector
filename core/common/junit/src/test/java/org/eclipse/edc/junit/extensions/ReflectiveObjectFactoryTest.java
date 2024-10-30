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

package org.eclipse.edc.junit.extensions;

import org.eclipse.edc.boot.system.injection.InjectionPointScanner;
import org.eclipse.edc.boot.system.injection.InjectorImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReflectiveObjectFactoryTest {

    private ReflectiveObjectFactory factory;

    @BeforeEach
    void setUp() {
        var injector = new InjectorImpl((t, c) -> null);
        var mockedContext = mock(ServiceExtensionContext.class);
        when(mockedContext.hasService(eq(SomeService.class))).thenReturn(true);
        when(mockedContext.getService(eq(SomeService.class), anyBoolean())).thenReturn(new SomeService());

        factory = new ReflectiveObjectFactory(injector, new InjectionPointScanner(), mockedContext);
    }

    @Test
    void constructInstance() {
        var handler = factory.constructInstance(TestTargetObject.class);
        assertThat(handler).isNotNull()
                .extracting(TestTargetObject::getObject).isNotNull();
    }

    @Test
    void constructInstance_noDefaultCtor() {
        assertThatThrownBy(() -> factory.constructInstance(NoDefaultCtor.class)).isInstanceOf(EdcException.class).hasRootCauseInstanceOf(NoSuchMethodException.class);
    }

    @Test
    void constructInstance_nothingToInject() {
        var instance = factory.constructInstance(NoInjectionPoints.class);
        assertThat(instance).isNotNull();
    }

    public static class TestTargetObject {
        @Inject
        private SomeService obj;

        public SomeService getObject() {
            return obj;
        }
    }

    public static class NoDefaultCtor {
        private final String id;
        @Inject
        private SomeService obj;

        NoDefaultCtor(String id) {
            this.id = id;
        }

        public SomeService getObject() {
            return obj;
        }
    }

    public static class NoInjectionPoints {

    }

    public static class SomeService {
    }
}
