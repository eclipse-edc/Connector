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

package org.eclipse.edc.web.jersey.validation;

import org.eclipse.edc.web.spi.validation.InterceptorFunction;
import org.eclipse.edc.web.spi.validation.InterceptorFunctionRegistry;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Provides {@link InvocationHandler} objects for methods. That means, it offers a way to register an interceptor function
 * that gets called whenever a resource method is invoked. In most cases that is going to be method on a REST controller.
 */
public class ResourceInterceptorProvider implements ResourceMethodInvocationHandlerProvider, InterceptorFunctionRegistry {

    private final Map<Method, InterceptorFunction> methodBoundFunctions;
    private final Map<Class<?>, InterceptorFunction> typeBoundFunctions;
    private final List<InterceptorFunction> globallyBoundFunctions;

    /**
     * Do not modify this constructor. The Binder mechanism requires a public default CTor
     */
    public ResourceInterceptorProvider() {
        methodBoundFunctions = new HashMap<>();
        globallyBoundFunctions = new ArrayList<>();
        typeBoundFunctions = new HashMap<>();
    }

    @Override
    public InvocationHandler create(Invocable method) {
        var definitionMethod = method.getDefinitionMethod();
        var parameterTypes = definitionMethod.getParameterTypes();

        var methodBound = ofNullable(methodBoundFunctions.get(definitionMethod));

        var typeBound = Arrays.stream(parameterTypes)
                .map(typeBoundFunctions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        var functions = new ArrayList<InterceptorFunction>();

        // first method-bound, then type-bound, then global
        methodBound.ifPresent(functions::add);
        functions.addAll(typeBound);
        functions.addAll(globallyBoundFunctions);

        // no point in registering a noop-interceptor
        return functions.isEmpty() ? null : new ResourceInterceptor(functions);
    }

    /**
     * Registers a {@link InterceptorFunction} that is invoked whenever a resource method has one (or several) parameters that match the type of {@code type}.
     *
     * @param type     The type to match against a method's parameters
     * @param function The function
     * @param <T>      The argument type
     */
    @Override
    public <T> void addFunction(Class<T> type, InterceptorFunction function) {
        typeBoundFunctions.put(type, function);
    }

    /**
     * Registers a {@link InterceptorFunction} that is invoked on <em>every</em> resource method.
     *
     * @param function The function
     */
    @Override
    public void addFunction(InterceptorFunction function) {
        globallyBoundFunctions.add(function);
    }

    /**
     * Registers a {@link InterceptorFunction} that is invoked whenever a particular method is invoked on a resource. This could be a controller method.
     * Please note that the exact {@link Method} as obtained by Reflection must be passed here.
     *
     * @param method             The {@link Method} which to intercept
     * @param validationFunction The function
     */
    @Override
    public void addFunction(Method method, InterceptorFunction validationFunction) {
        methodBoundFunctions.put(method, validationFunction);
    }
}
