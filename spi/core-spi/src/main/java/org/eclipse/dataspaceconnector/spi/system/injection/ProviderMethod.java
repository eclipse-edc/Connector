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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * Represents a {@link Method} that is annotated with the {@link Provider} annotation.
 */
public class ProviderMethod {
    private final Method method;
    private final boolean isDefault;

    public ProviderMethod(Method method) {
        this.method = method;
        isDefault = ofNullable(method.getAnnotation(Provider.class)).map(Provider::isDefault)
                .orElseThrow(() -> new IllegalArgumentException(format("Method %s is not annotated with @Provider!", method)));
    }

    public Method getMethod() {
        return method;
    }

    /**
     * Whether {@link Provider#isDefault()} is {@code true} or {@code false}
     */
    public boolean isDefault() {
        return isDefault;
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getReturnType() {
        return (Class<T>) method.getReturnType();
    }

    public Object invoke(Object target, Object... params) {
        try {
            if (method.getParameterTypes().length == 0) {
                return method.invoke(target);
            }
            if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == ServiceExtensionContext.class) {
                return method.invoke(target, params);
            } else {
                throw new IllegalArgumentException("Provider methods can only have 0..1 arguments, and only accept a ServiceExtensionContext!");
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new EdcInjectionException(e);
        }
    }
}
