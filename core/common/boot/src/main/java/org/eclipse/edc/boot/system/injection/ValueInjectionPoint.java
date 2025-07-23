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

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.ValueProvider;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

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
    private final String key;

    /**
     * Constructs a new ValueInjectionPoint instance
     *
     * @param objectInstance  The object instance that contains the annotated field. May be null in case the declaring class is not a {@link org.eclipse.edc.spi.system.SystemExtension}
     * @param targetField     The annotated {@link Field}
     * @param annotationValue The concrete annotation instance (needed to obtain its attributes)
     * @param declaringClass  The class where the annotated field is declared. Usually, this is {@code objectInstance.getClass()}.
     */
    public ValueInjectionPoint(T objectInstance, Field targetField, Setting annotationValue, Class<?> declaringClass) {
        this(objectInstance, targetField, annotationValue, declaringClass, null);
    }

    public ValueInjectionPoint(T objectInstance, Field targetField, Setting annotationValue, Class<?> declaringClass, SettingContext settingContext) {
        this.objectInstance = objectInstance;
        this.targetField = targetField;
        this.declaringClass = declaringClass;
        this.targetField.setAccessible(true);
        this.annotationValue = annotationValue;
        this.key = settingContext == null ? annotationValue.key() : settingContext.value() + "." + annotationValue.key();
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
    public Result<Void> setTargetValue(Object value) {
        if (objectInstance != null) {
            try {
                targetField.set(objectInstance, value);
            } catch (IllegalAccessException e) {
                return Result.failure("Could not assign value '%s' to field '%s'. Reason: %s".formatted(value, targetField, e.getMessage()));
            }
            return Result.success();
        }
        return Result.failure("Cannot set field, object instance is null");
    }

    /**
     * Returns a {@link ValueProvider} that takes the annotation's {@link Setting#defaultValue()} attribute or null
     *
     * @return a nullable default value provider
     */
    @Override
    public @Nullable ValueProvider getDefaultValueProvider() {
        if (!Setting.NULL.equals(annotationValue.defaultValue())) {
            return context -> annotationValue.defaultValue();
        }
        return null;
    }

    /**
     * Not used here
     *
     * @param defaultValueProvider Ignored
     */
    @Override
    public void setDefaultValueProvider(ValueProvider defaultValueProvider) {

    }

    @Override
    public Object resolve(ServiceExtensionContext context, DefaultServiceSupplier defaultServiceSupplier) {
        var config = context.getConfig();
        var type = getType();

        // value is found in the config
        if (config.hasKey(key)) {
            return parseEntry(config.getString(key), type);
        }

        // not found in config, but there is a default value
        var def = ofNullable(defaultServiceSupplier)
                .map(s -> s.provideFor(this, context))
                .map(Object::toString);
        if (def.isPresent()) {
            var defaultValue = def.get();
            if (!defaultValue.trim().equals(Setting.NULL)) {
                var msg = "Config value: no setting found for '%s', falling back to default value '%s'".formatted(key, defaultValue);
                if (annotationValue.warnOnMissingConfig()) {
                    context.getMonitor().warning(msg);
                } else {
                    context.getMonitor().debug(msg);
                }
                return parseEntry(defaultValue, type);
            }
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
     * @param ignoredMap Ignored
     * @param context    the {@link ServiceExtensionContext} in which the config is expected to be found.
     * @return success if found in the context, a failure otherwise.
     */
    @Override
    public Result<List<InjectionContainer<T>>> getProviders(Map<Class<?>, List<InjectionContainer<T>>> ignoredMap, ServiceExtensionContext context) {

        if (!annotationValue.required()) {
            return Result.success(emptyProviderlist); // optional configs are always satisfied
        }

        var defaultVal = annotationValue.defaultValue();

        if (defaultVal != null && !defaultVal.trim().equals(Setting.NULL)) {
            return Result.success(emptyProviderlist); // a default value means the value injection point can always be satisfied
        }

        // no default value, the required value may be found in the config
        return context.getConfig().hasKey(key)
                ? Result.success(emptyProviderlist)
                : Result.failure(toString());
    }

    @Override
    public String getTypeString() {
        return "Configuration value";
    }

    @Override
    public String toString() {
        return "Configuration value \"%s\" of type [%s] (property '%s')".formatted(targetField.getName(), getType(), key);
    }

    private Object parseEntry(String string, Class<?> valueType) {
        try {
            if (valueType == Long.class || valueType == long.class) {
                return Long.parseLong(string);
            }
            if (valueType == Integer.class || valueType == int.class) {
                return Integer.parseInt(string);
            }
            if (valueType == Double.class || valueType == double.class) {
                return Double.parseDouble(string);
            }
        } catch (NumberFormatException e) {
            throw new EdcInjectionException("Config field '%s' is of type '%s', but the value resolved from key '%s' is \"%s\" which cannot be interpreted as %s.".formatted(targetField.getName(), valueType, key, string, valueType));
        }
        if (valueType == Boolean.class || valueType == boolean.class) {
            return Boolean.parseBoolean(string);
        }

        return string;
    }

}
