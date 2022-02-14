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

import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

import java.lang.reflect.Field;

import static java.lang.String.format;

/**
 * Represents one single auto-injectable field. More specific, it is a tuple consisting of a target, a field, the respective feature string and a flag whether the
 * dependency is required or not.
 * <p>
 * Each injectable field of a {@link ServiceExtension} is represented by one InjectionPoint
 */
public class FieldInjectionPoint<T> implements InjectionPoint<T> {
    private final T instance;
    private final Field injectedField;
    private final String featureString;
    private final boolean isRequired;

    public FieldInjectionPoint(T instance, Field injectedField, String featureString) {
        this(instance, injectedField, featureString, true);
    }

    public FieldInjectionPoint(T instance, Field injectedField, String featureString, boolean isRequired) {
        this.instance = instance;
        this.injectedField = injectedField;
        this.injectedField.setAccessible(true);
        this.featureString = featureString;
        this.isRequired = isRequired;
    }

    @Override
    public T getInstance() {
        return instance;
    }

    @Override
    public String getFeatureName() {
        return featureString;
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
    public void setTargetValue(Object service) throws IllegalAccessException {
        injectedField.set(instance, service);
    }

    @Override
    public String toString() {
        return format("Field \"%s\" of type [%s] required by %s", injectedField.getName(), getType(), instance.getClass().getName());
    }
}
