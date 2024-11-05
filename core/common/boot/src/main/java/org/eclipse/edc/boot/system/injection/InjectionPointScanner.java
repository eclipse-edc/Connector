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

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Scans a particular (partly constructed) object for fields that are annotated with {@link Inject} and returns them
 * in a {@link Set}
 */
public class InjectionPointScanner {

    public <T> Stream<InjectionPoint<T>> getInjectionPoints(T instance) {

        var targetClass = instance.getClass();

        // scan service injection points
        var fields = Arrays.stream(targetClass.getDeclaredFields())
                .filter(f -> f.getAnnotation(Inject.class) != null)
                .map(f -> {
                    var isRequired = f.getAnnotation(Inject.class).required();
                    return new ServiceInjectionPoint<>(instance, f, isRequired);
                });

        // scan value injection points
        var values = Arrays.stream(targetClass.getDeclaredFields())
                .filter(f -> f.getAnnotation(Setting.class) != null && !Setting.NULL.equals(f.getAnnotation(Setting.class).key()))
                .map(f -> {
                    var annotation = f.getAnnotation(Setting.class);
                    return new ValueInjectionPoint<>(instance, f, annotation, targetClass);
                });

        // scan configuration injection points
        var configObjects = Arrays.stream(targetClass.getDeclaredFields())
                .filter(f -> f.getAnnotation(Configuration.class) != null)
                .map(f -> new ConfigurationInjectionPoint<>(instance, f));

        return Stream.of(fields, values, configObjects).flatMap(Function.identity());
    }
}
