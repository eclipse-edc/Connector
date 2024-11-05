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

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.ValueProvider;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * Represents one single auto-injectable field. More specific, it is a tuple consisting of a target, a field, the respective feature string and a flag whether the
 * dependency is required or not.
 * <p>
 * Each injectable field of a {@link ServiceExtension} is represented by one InjectionPoint
 */
public class ServiceInjectionPoint<T> implements InjectionPoint<T> {
    private final T instance;
    private final Field injectedField;
    private final boolean isRequired;
    private ValueProvider defaultServiceProvider;

    public ServiceInjectionPoint(T instance, Field injectedField) {
        this(instance, injectedField, true);
    }

    public ServiceInjectionPoint(T instance, Field injectedField, boolean isRequired) {
        this.instance = instance;
        this.injectedField = injectedField;
        this.injectedField.setAccessible(true);
        this.isRequired = isRequired;
    }

    @Override
    public T getTargetInstance() {
        return instance;
    }

    @Override
    public Class<?> getType() {
        return injectedField.getType();
    }

    @Override
    public boolean isRequired() {
        return isRequired;
    }

    @Override
    public Result<Void> setTargetValue(Object value) {
        try {
            injectedField.set(instance, value);
        } catch (IllegalAccessException e) {
            return Result.failure("Could not assign value '%s' to field '%s'. Reason: %s".formatted(value, injectedField, e.getMessage()));
        }
        return Result.success();
    }

    @Override
    public @Nullable ValueProvider getDefaultValueProvider() {
        return defaultServiceProvider;
    }

    @Override
    public void setDefaultValueProvider(ValueProvider defaultValueProvider) {
        this.defaultServiceProvider = defaultValueProvider;

    }

    @Override
    public Object resolve(ServiceExtensionContext context, DefaultServiceSupplier defaultServiceSupplier) {
        var serviceClass = getType();
        if (context.hasService(serviceClass)) {
            return context.getService(serviceClass, !isRequired());
        } else {
            return defaultServiceSupplier.provideFor(this, context);
        }
    }

    @Override
    public Result<List<InjectionContainer<T>>> getProviders(Map<Class<?>, List<InjectionContainer<T>>> dependencyMap, ServiceExtensionContext context) {
        var serviceClass = getType();
        var providers = ofNullable(dependencyMap.get(serviceClass));
        if (providers.isPresent()) {
            return Result.success(providers.get());
        } else if (context.hasService(serviceClass)) {
            return Result.success(List.of());
        } else {
            // attempt to interpret the feature name as class name and see if the context has that service
            return Result.failure(injectedField.getName() + " of type " + serviceClass);
        }

    }

    @Override
    public String getTypeString() {
        return "Service";
    }

    @Override
    public String toString() {
        return format("Field \"%s\" of type [%s] required by %s", injectedField.getName(), getType(), instance.getClass().getName());
    }
}
