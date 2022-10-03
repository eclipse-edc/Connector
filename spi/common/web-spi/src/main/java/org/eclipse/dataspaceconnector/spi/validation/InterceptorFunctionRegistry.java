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

package org.eclipse.dataspaceconnector.spi.validation;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.ExtensionPoint;

import java.lang.reflect.Method;

/**
 * Provides an interface where customers can register functions to intercept Jersey resource methods, for example for performing their own validation logic.
 */
@ExtensionPoint
public interface InterceptorFunctionRegistry {

    /**
     * Registers an interceptor function for a particular type (e.g. a DTO). The interceptor function gets applied to
     * all resource methods that have a T object in their signature
     *
     * @param type               The class of the object for which to register the function
     * @param validationFunction A function that processes the object and returns a Result
     */
    <T> void addFunction(Class<T> type, InterceptorFunction validationFunction);

    /**
     * Registers an interceptor function for all resource methods. Conditional evaluation must be done in the
     * interceptor function itself
     *
     * @param validationFunction Receives the list of arguments of the resource method, returns a Result
     */
    void addFunction(InterceptorFunction validationFunction);

    /**
     * Registers an interceptor function for a particular resource method (= Controller method). The interceptor
     * function only gets applied to that particular method.
     *
     * @param method             The {@link java.lang.reflect.Method} (of a controller) for which to register the function
     * @param validationFunction Receives the list of arguments of the resource method, returns a Result
     */
    void addFunction(Method method, InterceptorFunction validationFunction);
}
