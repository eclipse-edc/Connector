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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.extension.jersey.validation;

import org.eclipse.dataspaceconnector.spi.exception.InvalidRequestException;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.validation.InterceptorFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.extension.jersey.validation.TestObject.getAnswerMethod;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceInterceptorTest {

    private final TestObject proxy = new TestObject();
    private ResourceInterceptor interceptor;

    @BeforeEach
    void setup() {
    }

    @Test
    void invoke_whenNoFunctions() throws Throwable {
        interceptor = new ResourceInterceptor(List.of());
        var result = interceptor.invoke(proxy, getAnswerMethod(), new Object[0]);
        assertThat(result).isEqualTo(42);
    }

    @Test
    void invoke_whenFunctionReturnsSuccess() throws Throwable {
        var functionMock = mock(InterceptorFunction.class);
        when(functionMock.apply(any())).thenReturn(Result.success());
        interceptor = new ResourceInterceptor(List.of(functionMock));

        var result = interceptor.invoke(proxy, getAnswerMethod(), new Object[0]);
        assertThat(result).isEqualTo(42);
        verify(functionMock).apply(argThat(argument -> argument.length == 0));
    }

    @Test
    void invoke_whenFunctionReturnsFailure() {
        var functionMock = mock(InterceptorFunction.class);
        when(functionMock.apply(any())).thenReturn(Result.failure("test failure"));
        interceptor = new ResourceInterceptor(List.of(functionMock));

        assertThatThrownBy(() -> interceptor.invoke(proxy, getAnswerMethod(), new Object[0]))
                .isInstanceOf(InvocationTargetException.class)
                .hasRootCauseInstanceOf(InvalidRequestException.class)
                .hasRootCauseMessage("test failure");
        verify(functionMock).apply(argThat(argument -> argument.length == 0));
    }

    @Test
    void invoke_whenFunctionsReturnMixedResults() {
        var successFunctionMock = mock(InterceptorFunction.class);
        when(successFunctionMock.apply(any())).thenReturn(Result.failure("test failure"));
        var failedFunctionMock = mock(InterceptorFunction.class);
        when(failedFunctionMock.apply(any())).thenReturn(Result.success());

        interceptor = new ResourceInterceptor(List.of(successFunctionMock, failedFunctionMock));

        assertThatThrownBy(() -> interceptor.invoke(proxy, getAnswerMethod(), new Object[0]))
                .isInstanceOf(InvocationTargetException.class)
                .hasRootCauseInstanceOf(InvalidRequestException.class)
                .hasRootCauseMessage("test failure");
        verify(successFunctionMock).apply(argThat(argument -> argument.length == 0));
    }

    @Test
    void invoke_verifyFunctionSequence() throws Throwable {
        var fct1 = mock(InterceptorFunction.class);
        when(fct1.apply(any())).thenReturn(Result.success());
        var fct2 = mock(InterceptorFunction.class);
        when(fct2.apply(any())).thenReturn(Result.success());

        interceptor = new ResourceInterceptor(List.of(fct1, fct2));
        interceptor.invoke(proxy, getAnswerMethod(), new Object[0]);

        var functionOrder = inOrder(fct1, fct2);
        functionOrder.verify(fct1).apply(any());
        functionOrder.verify(fct2).apply(any());

    }
}
