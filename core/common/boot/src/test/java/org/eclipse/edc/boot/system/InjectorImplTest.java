/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.boot.system;

import org.eclipse.edc.boot.system.injection.DefaultServiceSupplier;
import org.eclipse.edc.boot.system.injection.EdcInjectionException;
import org.eclipse.edc.boot.system.injection.FieldInjectionPoint;
import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.boot.system.injection.InjectorImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class InjectorImplTest {

    private InjectorImpl injector;
    private Monitor monitor;
    private ServiceExtensionContext context;
    private final DefaultServiceSupplier defaultServiceSupplier = mock(DefaultServiceSupplier.class);

    @BeforeEach
    void setup() {
        injector = new InjectorImpl(defaultServiceSupplier);
        monitor = mock(Monitor.class);
        context = mock(ServiceExtensionContext.class);
        when(context.getMonitor()).thenReturn(monitor);
    }

    @AfterEach
    void teardown() {
        verify(context).getMonitor();
        verifyNoMoreInteractions(context, monitor);
    }

    @Test
    @DisplayName("Testing ServiceExtension with no injection points")
    void templateWithNoInjectionPoints() {
        var serviceExtension = new EmptyTestExtension();
        var template = new InjectionContainer<>(serviceExtension, Collections.emptySet(), Collections.emptyList());

        injector.inject(template, context);

        verify(context, never()).getService(any(), anyBoolean());
    }

    @Test
    @DisplayName("All injection points of a service are satisfied")
    void allInjectionPointsSatisfied() throws NoSuchFieldException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var template = new InjectionContainer<>(serviceExtension, Set.of(new FieldInjectionPoint<>(serviceExtension, field)), Collections.emptyList());
        when(context.hasService(eq(SomeObject.class))).thenReturn(true);
        when(context.getService(eq(SomeObject.class), anyBoolean())).thenReturn(new SomeObject());

        injector.inject(template, context);

        assertThat(serviceExtension.someObject).isNotNull();
        verify(context).hasService(eq(SomeObject.class));
        verify(context).getService(eq(SomeObject.class), anyBoolean());
    }

    @Test
    @DisplayName("Injection point of a service is not satisfied but a default is provided")
    void defaultInjectionPoint() throws NoSuchFieldException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var template = new InjectionContainer<>(serviceExtension, Set.of(new FieldInjectionPoint<>(serviceExtension, field)), Collections.emptyList());
        when(context.hasService(SomeObject.class)).thenReturn(false);
        when(context.getService(SomeObject.class, false)).thenThrow(new EdcException("Service not found"));
        when(defaultServiceSupplier.provideFor(any(), any())).thenReturn(new SomeObject());

        injector.inject(template, context);

        assertThat(serviceExtension.someObject).isNotNull();
        verify(context).hasService(eq(SomeObject.class));
    }

    @Test
    @DisplayName("Injection point of a service is not satisfied")
    void notAllInjectionPointsSatisfied_shouldThrowException() throws NoSuchFieldException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var template = new InjectionContainer<>(serviceExtension, Set.of(new FieldInjectionPoint<>(serviceExtension, field)), Collections.emptyList());
        var rootCauseException = new EdcInjectionException("Service not found");
        when(context.hasService(SomeObject.class)).thenReturn(false);
        when(defaultServiceSupplier.provideFor(any(), any())).thenThrow(rootCauseException);

        assertThatThrownBy(() -> injector.inject(template, context)).isInstanceOf(EdcInjectionException.class)
                .hasMessageStartingWith("Service not found");
        assertThat(serviceExtension.someObject).isNull();

        verify(context).hasService(SomeObject.class);
        verify(context, never()).getService(SomeObject.class, false);
    }

    @Test
    @DisplayName("Cannot set value of the injected field")
    void cannotSetInjectionPoint_shouldThrowException() throws NoSuchFieldException, IllegalAccessException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var injectionPoint = spy(new FieldInjectionPoint<>(serviceExtension, field));
        var template = new InjectionContainer<>(serviceExtension, Set.of(injectionPoint), Collections.emptyList());

        var value = new SomeObject();
        when(context.hasService(eq(SomeObject.class))).thenReturn(true);
        when(context.getService(eq(SomeObject.class), anyBoolean())).thenReturn(value);

        doThrow(new IllegalAccessException("test")).when(injectionPoint).setTargetValue(value);

        assertThatThrownBy(() -> injector.inject(template, context)).isInstanceOf(EdcInjectionException.class).hasCauseInstanceOf(IllegalAccessException.class);
        assertThat(serviceExtension.someObject).isNull();
        verify(context).hasService(eq(SomeObject.class));
        verify(context).getService(eq(SomeObject.class), anyBoolean());
        verify(monitor).warning(anyString(), any());
    }

    @Test
    @DisplayName("Injection point of an optional service that's provided")
    void optionalService_provided() throws NoSuchFieldException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var template = new InjectionContainer<>(serviceExtension, Set.of(new FieldInjectionPoint<>(serviceExtension, field, false)), Collections.emptyList());
        when(context.hasService(eq(SomeObject.class))).thenReturn(true);
        when(context.getService(any(), anyBoolean())).thenReturn(new SomeObject());

        injector.inject(template, context);

        assertThat(serviceExtension.someObject).isNotNull();
        verify(context).hasService(eq(SomeObject.class));
        verify(context).getService(eq(SomeObject.class), eq(true));
        verifyNoInteractions(defaultServiceSupplier);
    }

    @Test
    @DisplayName("Injection point of an optional service is not satisfied and a default is provided")
    void optionalService_defaultProvided() throws NoSuchFieldException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var template = new InjectionContainer<>(serviceExtension, Set.of(new FieldInjectionPoint<>(serviceExtension, field, false)), Collections.emptyList());
        when(context.hasService(eq(SomeObject.class))).thenReturn(false);
        when(defaultServiceSupplier.provideFor(any(), any())).thenReturn(new SomeObject());

        injector.inject(template, context);

        assertThat(serviceExtension.someObject).isNotNull();
        verify(context).hasService(eq(SomeObject.class));
        verify(defaultServiceSupplier).provideFor(any(), any());
    }

    @Test
    @DisplayName("Injection point of an optional service is not satisfied and a default is not provided either")
    void optionalService_defaultNotProvided() throws NoSuchFieldException {
        var serviceExtension = new TestServiceExtension();
        var field = serviceExtension.getClass().getDeclaredField("someObject");
        var template = new InjectionContainer<>(serviceExtension, Set.of(new FieldInjectionPoint<>(serviceExtension, field, false)), Collections.emptyList());
        when(context.hasService(eq(SomeObject.class))).thenReturn(false);
        when(defaultServiceSupplier.provideFor(any(), any())).thenReturn(null);

        injector.inject(template, context);

        assertThat(serviceExtension.someObject).isNull();
        verify(context).hasService(eq(SomeObject.class));
        verify(defaultServiceSupplier).provideFor(any(), any());
    }

    private static class TestServiceExtension implements ServiceExtension {
        @Inject
        private SomeObject someObject;
    }

    private static class SomeObject {
    }

    private static class EmptyTestExtension implements ServiceExtension {
    }
}
