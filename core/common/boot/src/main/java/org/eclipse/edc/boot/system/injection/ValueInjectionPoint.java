/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.boot.system.injection.lifecycle.ServiceProvider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Injection point for configuration values ("settings"). Configuration values must be basic data types and be annotated
 * with {@link Setting}, for example:
 *
 * <pre>
 * public class SomeExtension implement ServiceExtension {
 *   \@Setting(key = "foo.bar.baz", description = "some important config", ...)d
 *   private String fooBarBaz;
 * }
 * </pre>
 * Currently, only {@link String}, {@link Double}, {@link Integer}, {@link Long} and {@link Boolean} are supported as data types
 * for the annotated field.
 *
 * @param <T> The type of the declaring class.
 */
public class ValueInjectionPoint<T> implements InjectionPoint<T> {
    public final List<InjectionContainer<T>> emptyProviderlist = List.of();
    private final T objectInstance;
    private final Field targetField;
    private final Setting annotationValue;
    private final Class<?> declaringClass;

    /**
     * Constructs a new ValueInjectionPoint instance
     *
     * @param objectInstance  The object instance that contains the annotated field. May be null in case the declaring class is not a {@link org.eclipse.edc.spi.system.SystemExtension}
     * @param targetField     The annotated {@link Field}
     * @param annotationValue The concrete annotation instance (needed to obtain its attributes)
     * @param declaringClass  The class where the annotated field is declared. Usually, this is {@code objectInstance.getClass()}.
     */
    public ValueInjectionPoint(T objectInstance, Field targetField, Setting annotationValue, Class<?> declaringClass) {
        this.objectInstance = objectInstance;
        this.targetField = targetField;
        this.declaringClass = declaringClass;
        this.targetField.setAccessible(true);
        this.annotationValue = annotationValue;
    }

    public Field getTargetField() {
        return targetField;
    }

    @Override
    public T getTargetInstance() {
        return objectInstance;
    }

    @Override
    public Class<?> getType() {
        return targetField.getType();
    }

    @Override
    public boolean isRequired() {
        return annotationValue.required();
    }

    @Override
    public Result<Void> setTargetValue(Object targetValue) throws IllegalAccessException {
        if (objectInstance != null) {
            targetField.set(objectInstance, targetValue);
            return Result.success();
        }
        return Result.failure("Cannot set field, object instance is null");
    }

    /**
     * Not used here, always returns null;
     *
     * @return always {@code null}
     */
    @Override
    public ServiceProvider getDefaultServiceProvider() {
        return null;
    }

    /**
     * Not used here
     *
     * @param defaultServiceProvider Ignored
     */
    @Override
    public void setDefaultServiceProvider(ServiceProvider defaultServiceProvider) {

    }

    @Override
    public Object resolve(ServiceExtensionContext context, DefaultServiceSupplier defaultServiceSupplier) {
        var config = context.getConfig();
        var type = getType();
        var key = annotationValue.key();

        // value is found in the config
        if (config.hasKey(key)) {
            return parseEntry(config.getString(key), type);
        }

        // not found in config, but there is a default value
        var def = annotationValue.defaultValue();
        if (def != null && !def.trim().equals(Setting.NULL)) {
            var msg = "Config value: no setting found for '%s', falling back to default value '%s'".formatted(key, def);
            if (annotationValue.warnOnMissingConfig()) {
                context.getMonitor().warning(msg);
            } else {
                context.getMonitor().debug(msg);
            }
            return parseEntry(def, type);
        }

        // neither in config, nor default val
        if (annotationValue.required()) {
            throw new EdcInjectionException("No config value and no default value found for injected field " + this);
        }
        return null;
    }

    /**
     * Determines whether a configuration value is "satisfied by" the given {@link ServiceExtensionContext} (the dependency map is ignored).
     *
     * @param dependencyMap Ignored
     * @param context       the {@link ServiceExtensionContext} in which the config is expected to be found.
     * @return success if found in the context, a failure otherwise.
     */
    @Override
    public Result<List<InjectionContainer<T>>> getProviders(Map<Class<?>, List<InjectionContainer<T>>> dependencyMap, ServiceExtensionContext context) {

        if (!annotationValue.required()) {
            return Result.success(emptyProviderlist); // optional configs are always satisfied
        }

        var defaultVal = annotationValue.defaultValue();

        if (defaultVal != null && !defaultVal.trim().equals(Setting.NULL)) {
            return Result.success(emptyProviderlist); // a default value means the value injection point can always be satisfied
        }

        // no default value, the required value may be found in the config
        return context.getConfig().hasKey(annotationValue.key())
                ? Result.success(emptyProviderlist)
                : Result.failure("%s (property \"%s\")".formatted(targetField.getName(), annotationValue.key()));
    }

    @Override
    public String getTypeString() {
        return "Config value";
    }

    @Override
    public String toString() {
        return "Configuration value \"%s\" of type %s (config key '%s') in %s".formatted(targetField.getName(), getType(), annotationValue.key(), declaringClass);
    }

    private Object parseEntry(String string, Class<?> valueType) {
        try {
            if (valueType == Long.class) {
                return Long.parseLong(string);
            }
            if (valueType == Integer.class) {
                return Integer.parseInt(string);
            }
            if (valueType == Double.class) {
                return Double.parseDouble(string);
            }
        } catch (NumberFormatException e) {
            throw new EdcInjectionException("Config field '%s' is of type '%s', but the value resolved from key '%s' is \"%s\" which cannot be interpreted as %s.".formatted(targetField.getName(), valueType, annotationValue.key(), string, valueType));
        }
        if (valueType == Boolean.class) {
            return Boolean.parseBoolean(string);
        }

        return string;
    }

}
