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

import org.eclipse.edc.boot.system.TestFunctions;
import org.eclipse.edc.boot.system.TestObject;
import org.eclipse.edc.boot.system.testextensions.DependentExtension;
import org.eclipse.edc.boot.system.testextensions.RequiredDependentExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ServiceInjectionPointTest {

    @Test
    void getInstance() {
        var ip = new ServiceInjectionPoint<>(new RequiredDependentExtension(), TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        assertThat(ip.getTargetInstance()).isInstanceOf(RequiredDependentExtension.class);
    }

    @Test
    void getType() {
        var ip = new ServiceInjectionPoint<>(new RequiredDependentExtension(), TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        assertThat(ip.getType()).isEqualTo(TestObject.class);
    }

    @Test
    void isRequired() {
        var ip = new ServiceInjectionPoint<>(new RequiredDependentExtension(), TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        assertThat(ip.isRequired()).isTrue();

        var ip2 = new ServiceInjectionPoint<>(new DependentExtension(), TestFunctions.getDeclaredField(DependentExtension.class, "testObject"), false);
        assertThat(ip2.isRequired()).isFalse();
    }

    @Test
    void setTargetValue() throws IllegalAccessException {
        var requiredDependentExtension = new RequiredDependentExtension();
        var ip = new ServiceInjectionPoint<>(requiredDependentExtension, TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        var newObject = new TestObject("new object");
        ip.setTargetValue(newObject);
        assertThat(requiredDependentExtension.getTestObject()).isEqualTo(newObject);
    }


    @Test
    void resolve_providerFoundInContext() {
        var ip = new ServiceInjectionPoint<>(new RequiredDependentExtension(), TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        var context = mock(ServiceExtensionContext.class);
        var defaultServiceSupplier = mock(DefaultServiceSupplier.class);

        var testObject = new TestObject("foo");
        when(context.hasService(eq(TestObject.class))).thenReturn(true);
        when(context.getService(eq(TestObject.class), anyBoolean())).thenReturn(testObject);
        var result = ip.resolve(context, defaultServiceSupplier);

        assertThat(result).isEqualTo(testObject);
        verifyNoInteractions(defaultServiceSupplier);

    }

    @Test
    void resolve_defaultProvider() {
        var ip = new ServiceInjectionPoint<>(new RequiredDependentExtension(), TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        var context = mock(ServiceExtensionContext.class);
        var defaultServiceSupplier = mock(DefaultServiceSupplier.class);

        var testObject = new TestObject("foo");
        when(context.hasService(eq(TestObject.class))).thenReturn(false);
        when(defaultServiceSupplier.provideFor(eq(ip), eq(context))).thenReturn(testObject);

        var result = ip.resolve(context, defaultServiceSupplier);

        assertThat(result).isEqualTo(testObject);
        verify(context).hasService(eq(TestObject.class));
        verify(defaultServiceSupplier).provideFor(eq(ip), eq(context));
        verifyNoMoreInteractions(context, defaultServiceSupplier);
    }

    @Test
    void resolve_noDefaultProvider() {
        var ip = new ServiceInjectionPoint<>(new RequiredDependentExtension(), TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        var context = mock(ServiceExtensionContext.class);
        var defaultServiceSupplier = mock(DefaultServiceSupplier.class);

        when(context.hasService(eq(TestObject.class))).thenReturn(false);
        when(defaultServiceSupplier.provideFor(eq(ip), eq(context))).thenThrow(new EdcInjectionException("foo"));

        assertThatThrownBy(() -> ip.resolve(context, defaultServiceSupplier)).isInstanceOf(EdcInjectionException.class);

        verify(context).hasService(eq(TestObject.class));
        verify(defaultServiceSupplier).provideFor(eq(ip), eq(context));
        verifyNoMoreInteractions(context, defaultServiceSupplier);

    }

    @Test
    void getProviders_fromMap() {
        var ip = new ServiceInjectionPoint<>(new RequiredDependentExtension(), TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        var context = mock(ServiceExtensionContext.class);

        var ic = new InjectionContainer<>(new RequiredDependentExtension(), Set.of(ip), List.of());

        var result = ip.getProviders(Map.of(TestObject.class, List.of(ic)), context);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void getProviders_fromContext() {
        var ip = new ServiceInjectionPoint<>(new RequiredDependentExtension(), TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        var context = mock(ServiceExtensionContext.class);

        when(context.hasService(eq(TestObject.class))).thenReturn(true);

        var result = ip.getProviders(Map.of(), context);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void isSatisfiedBy_notSatisfied() {
        var ip = new ServiceInjectionPoint<>(new RequiredDependentExtension(), TestFunctions.getDeclaredField(RequiredDependentExtension.class, "testObject"));
        var context = mock(ServiceExtensionContext.class);

        when(context.hasService(eq(TestObject.class))).thenReturn(false);

        var result = ip.getProviders(Map.of(), context);
        assertThat(result.succeeded()).isFalse();
    }

}