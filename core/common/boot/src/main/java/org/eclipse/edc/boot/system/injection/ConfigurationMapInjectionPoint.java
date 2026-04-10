/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.ValueProvider;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Injection point for configuration maps. A configuration map field is annotated with {@link org.eclipse.edc.runtime.metamodel.annotation.Configuration}
 * and has type {@link Map}{@code <String, T>} where {@code T} is a type annotated with {@link org.eclipse.edc.runtime.metamodel.annotation.Settings}.
 * The map keys are derived from the configuration path segments under the {@link SettingContext} prefix.
 * <p>
 * For example, given prefix {@code edc.iam.publickeys} and settings:
 * <pre>
 *   edc.iam.publickeys.pubname.id = myId
 *   edc.iam.publickeys.pubname.value = myValue
 * </pre>
 * the map will contain an entry with key {@code "pubname"} and value resolved from those settings.
 *
 * @param <T> The type of the declaring class.
 */
public class ConfigurationMapInjectionPoint<T> implements InjectionPoint<T> {

    private final T targetInstance;
    private final Field mapField;
    private final Class<?> valueType;
    private final ConfigurationObjectFactory configurationObjectFactory;

    public ConfigurationMapInjectionPoint(T instance, Field field, ConfigurationObjectFactory configurationObjectFactory) {
        this.targetInstance = instance;
        this.mapField = field;
        this.configurationObjectFactory = configurationObjectFactory;
        this.mapField.setAccessible(true);
        var genericType = (ParameterizedType) field.getGenericType();
        this.valueType = (Class<?>) genericType.getActualTypeArguments()[1];
    }

    @Override
    public T getTargetInstance() {
        return targetInstance;
    }

    @Override
    public Class<?> getType() {
        return mapField.getType();
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public Result<Void> setTargetValue(Object value) {
        try {
            mapField.set(targetInstance, value);
            return Result.success();
        } catch (IllegalAccessException e) {
            return Result.failure("Could not assign value to field '%s'. Reason: %s".formatted(mapField, e.getMessage()));
        }
    }

    @Override
    public @Nullable ValueProvider getDefaultValueProvider() {
        return null;
    }

    @Override
    public void setDefaultValueProvider(ValueProvider defaultValueProvider) {
    }

    @Override
    public Object resolve(ServiceExtensionContext context, DefaultServiceSupplier defaultServiceSupplier) {
        var settingContext = mapField.getAnnotation(SettingContext.class);
        var prefix = settingContext != null ? settingContext.value() : null;

        var baseConfig = prefix != null ? context.getConfig(prefix) : context.getConfig();

        var result = new HashMap<String, Object>();
        baseConfig.partition().forEach(partitionConfig -> {
            var partitionKey = partitionConfig.currentNode();
            var partitionPrefix = prefix != null ? prefix + "." + partitionKey : partitionKey;
            var value = configurationObjectFactory.instantiate(context, partitionPrefix, valueType);
            result.put(partitionKey, value);
        });
        return result;
    }

    @Override
    public Result<List<InjectionContainer<T>>> getProviders(Map<Class<?>, List<InjectionContainer<T>>> dependencyMap, ServiceExtensionContext context) {
        return Result.success(List.of());
    }
}
