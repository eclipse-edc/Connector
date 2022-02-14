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
package org.eclipse.dataspaceconnector.spi.system.injection;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents one {@link ServiceExtension} with a description of all its auto-injectable fields, which in turn are
 * represented by {@link FieldInjectionPoint}s.
 */
public class InjectionContainer<T> {
    private final T injectionTarget;
    private final Set<InjectionPoint<T>> injectionPoint;

    public InjectionContainer(T target, Set<InjectionPoint<T>> injectionPoint) {
        injectionTarget = target;
        if (injectionPoint.stream().anyMatch(ip -> ip.getInstance() != target)) {
            throw new EdcInjectionException("Injection target must match all InjectionPoints!");
        }
        this.injectionPoint = injectionPoint;

    }

    public T getInjectionTarget() {
        return injectionTarget;
    }

    public Set<InjectionPoint<T>> getInjectionPoints() {
        return injectionPoint;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "injectionTarget=" + injectionTarget +
                '}';
    }

    /**
     * checks that all there is a corresponding service instance for every entry in the @Provides annotation list.
     */
    public Result<Void> validate(ServiceExtensionContext context) {

        var providesAnnotation = injectionTarget.getClass().getAnnotation(Provides.class);
        if (providesAnnotation == null) {
            return Result.success();
        }

        var providedClasses = Stream.of(providesAnnotation.value());

        var errors = providedClasses
                .map(clazz -> context.hasService(clazz) ? null : clazz.getName())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return errors.isEmpty() ? Result.success() : Result.failure(errors);
    }
}
