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

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.Arrays;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.isPublic;

/**
 * Scans a given object for methods annotated with the {@link Provider} annotation.
 */
public class ProviderMethodScanner {
    private final Object target;

    public ProviderMethodScanner(ServiceExtension target) {
        this.target = target;
    }

    /**
     * Returns all methods annotated with {@link Provider}.
     */
    public Stream<ProviderMethod> allProviders() {
        return getProviderMethods(target);
    }

    /**
     * Returns all methods annotated with {@link Provider}, where {@link Provider#isDefault()} is {@code false}
     */
    public Stream<ProviderMethod> nonDefaultProviders() {
        return getProviderMethods(target).filter(pm -> !pm.isDefault());
    }

    /**
     * Returns all methods annotated with {@link Provider}, where {@link Provider#isDefault()} is {@code true}
     */
    public Stream<ProviderMethod> defaultProviders() {
        return getProviderMethods(target).filter(ProviderMethod::isDefault);
    }

    private Stream<ProviderMethod> getProviderMethods(Object extension) {
        return Arrays.stream(extension.getClass().getDeclaredMethods())
                .filter(m -> m.getAnnotation(Provider.class) != null)
                .map(ProviderMethod::new)
                .peek(method -> {
                    if (method.getReturnType().equals(Void.TYPE)) {
                        throw new EdcInjectionException("Methods annotated with @Provider must have a non-void return type!");
                    }
                    if (!isPublic(method.getMethod().getModifiers())) {
                        throw new EdcInjectionException("Methods annotated with @Provider must be public!");
                    }
                });
    }
}
