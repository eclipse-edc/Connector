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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * This {@link InvocationHandler} acts as interceptor whenever methods get called on a proxy object, and invokes
 * a list of {@link InterceptorFunction} objects before calling the actual proxy.
 * <p>
 * Note that any exception thrown by a {@link InterceptorFunction} may get swallowed or cause the proxy not to be invoked at all.
 */
class ResourceInterceptor implements InvocationHandler {

    private final List<InterceptorFunction> interceptorFunctions;

    ResourceInterceptor(List<InterceptorFunction> functions) {
        interceptorFunctions = List.copyOf(functions);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        var results = interceptorFunctions.stream()
                .map(f -> f.apply(args))
                .reduce(Result::merge);

        if (results.isPresent()) {
            if (results.get().failed()) {
                throwException(results.get());
            }
        }

        return method.invoke(proxy, args);
    }

    private void throwException(Result<Void> result) throws InvocationTargetException {
        var cause = new InvalidRequestException(result.getFailureMessages());

        // must be wrapped in an InvocationTargetException, so that the message dispatcher picks it up and forwards
        // it to the exception mapper(s)
        throw new InvocationTargetException(cause);
    }
}
