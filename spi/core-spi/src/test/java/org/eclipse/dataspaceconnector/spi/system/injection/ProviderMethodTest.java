/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.spi.system.injection;

import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderMethodTest {


    @Test
    void isDefault_noAnnotation_throwsException() {
        var m = mock(Method.class);
        assertThatThrownBy(() -> new ProviderMethod(m)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageEndingWith("is not annotated with @Provider!");
    }

    @Test
    void isDefault_withAnnotation() {
        var m = mock(Method.class);
        when(m.getAnnotation(Provider.class)).thenReturn(provider(true));
        assertThat(new ProviderMethod(m).isDefault()).isTrue();

    }

    @Test
    void isDefault_withAnnotation_noDefault() {
        var m = mock(Method.class);
        when(m.getAnnotation(Provider.class)).thenReturn(provider(false));
        assertThat(new ProviderMethod(m).isDefault()).isFalse();
    }

    @Test
    void verifyReturnType() {
        var m = mock(Method.class);
        when(m.getAnnotation(Provider.class)).thenReturn(provider(false));

        when(m.getReturnType()).thenAnswer(invocation -> String.class); //thenReturn does not work here due to the "capture<>"
        assertThat(new ProviderMethod(m).getReturnType()).isEqualTo(String.class);
    }

    @Test
    void invoke_withZeroArgs() throws InvocationTargetException, IllegalAccessException {
        var m = mock(Method.class);
        when(m.getAnnotation(Provider.class)).thenReturn(provider(false));
        when(m.getParameterTypes()).thenReturn(new Class[]{});
        var obj = new Object();
        when(m.invoke(any())).thenReturn(obj);

        assertThat(new ProviderMethod(m).invoke(obj)).isEqualTo(obj);
    }

    @Test
    void invoke_withOneArg() throws InvocationTargetException, IllegalAccessException {
        var m = mock(Method.class);
        when(m.getAnnotation(Provider.class)).thenReturn(provider(false));
        when(m.getParameterTypes()).thenReturn(new Class[]{ ServiceExtensionContext.class });
        var obj = new Object();
        when(m.invoke(any())).thenReturn(obj);

        assertThat(new ProviderMethod(m).invoke(obj)).isEqualTo(obj);
    }

    @Test
    void invoke_withMultipleArgs() throws InvocationTargetException, IllegalAccessException {
        var m = mock(Method.class);
        when(m.getParameterTypes()).thenReturn(new Class[]{ ServiceExtensionContext.class, String.class });
        var obj = new Object();
        when(m.invoke(any())).thenReturn(obj);

        assertThatThrownBy(() -> new ProviderMethod(m).invoke(obj)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invoke_withWrongSingleArg() throws InvocationTargetException, IllegalAccessException {
        var m = mock(Method.class);
        when(m.getParameterTypes()).thenReturn(new Class[]{ String.class });
        var obj = new Object();
        when(m.invoke(any())).thenReturn(obj);

        assertThatThrownBy(() -> new ProviderMethod(m).invoke(obj)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invoke_withVoidReturn() {
        var m = mock(Method.class);
        when(m.getParameterTypes()).thenReturn(new Class[]{ Void.class });

        assertThatThrownBy(() -> new ProviderMethod(m).invoke(new Object())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invoke_throwsException() throws InvocationTargetException, IllegalAccessException {
        var m = mock(Method.class);
        when(m.getAnnotation(Provider.class)).thenReturn(provider(false));
        when(m.getParameterTypes()).thenReturn(new Class[]{ ServiceExtensionContext.class });
        var obj = new Object();
        when(m.invoke(any())).thenThrow(new IllegalAccessException());

        assertThatThrownBy(() -> new ProviderMethod(m).invoke(obj)).isInstanceOf(EdcInjectionException.class).hasRootCauseInstanceOf(IllegalAccessException.class);
    }

    @NotNull
    private Provider provider(boolean isDefault) {
        return new Provider() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Provider.class;
            }

            @Override
            public boolean isDefault() {
                return isDefault;
            }
        };
    }
}