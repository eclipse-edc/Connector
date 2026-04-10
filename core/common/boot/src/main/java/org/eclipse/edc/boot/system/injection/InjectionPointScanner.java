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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Scans a particular (partly constructed) object for fields that are annotated with {@link Inject} and returns them
 * in a {@link Set}
 */
public class InjectionPointScanner {

    private final ConfigurationObjectFactory configurationObjectFactory = new ConfigurationObjectFactory();

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
                .map(f -> new SettingInjectionPoint<>(instance, f));

        // scan configuration injection points
        var configObjects = Arrays.stream(targetClass.getDeclaredFields())
                .filter(f -> f.getAnnotation(Configuration.class) != null)
                .map(f -> {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        return new ConfigurationMapInjectionPoint<>(instance, f, configurationObjectFactory);
                    }
                    return new ConfigurationInjectionPoint<>(instance, f, configurationObjectFactory);
                });

        return Stream.of(fields, values, configObjects).flatMap(Function.identity());
    }
}
