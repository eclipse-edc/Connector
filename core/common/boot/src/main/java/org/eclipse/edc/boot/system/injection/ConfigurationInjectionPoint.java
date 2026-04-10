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
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.ValueProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Injection point for configuration objects. Configuration objects are records or POJOs that contain fields annotated with {@link Setting}.
 * Configuration objects themselves must be annotated with {@link org.eclipse.edc.runtime.metamodel.annotation.Settings}.
 * Example:
 * <pre>
 *      public class SomeExtension implements ServiceExtension {
 *          \@Settings
 *          private SomeConfig someConfig;
 *      }
 *
 *      \@Settings
 *      public record SomeConfig(@Setting(key = "foo.bar.baz") String fooValue){ }
 * </pre>
 *
 * @param <T> The type of the declaring class.
 */
public class ConfigurationInjectionPoint<T> implements InjectionPoint<T> {
    private final T targetInstance;
    private final Field configurationField;
    private final ConfigurationObjectFactory configurationObjectFactory;

    public ConfigurationInjectionPoint(T instance, Field configurationField, ConfigurationObjectFactory configurationObjectFactory) {
        this.targetInstance = instance;
        this.configurationField = configurationField;
        this.configurationObjectFactory = configurationObjectFactory;
        this.configurationField.setAccessible(true);

    }

    @Override
    public T getTargetInstance() {
        return targetInstance;
    }

    @Override
    public Class<?> getType() {
        return configurationField.getType();
    }

    @Override
    public boolean isRequired() {
        return Arrays.stream(configurationField.getType().getDeclaredFields())
                .filter(f -> f.getAnnotation(Setting.class) != null)
                .anyMatch(f -> f.getAnnotation(Setting.class).required());
    }

    @Override
    public Result<Void> setTargetValue(Object value) {
        try {
            configurationField.set(targetInstance, value);
            return Result.success();
        } catch (IllegalAccessException e) {
            return Result.failure("Could not assign value '%s' to field '%s'. Reason: %s".formatted(value, configurationField, e.getMessage()));
        }
    }

    /**
     * Not used here, will always return null
     */
    @Override
    public @Nullable ValueProvider getDefaultValueProvider() {
        return null;
    }

    /**
     * Not used here
     */
    @Override
    public void setDefaultValueProvider(ValueProvider defaultValueProvider) {

    }

    @Override
    public Object resolve(ServiceExtensionContext context, DefaultServiceSupplier defaultServiceSupplier) {
        var keyPrefix = configurationField.getAnnotation(SettingContext.class) != null
                ? configurationField.getAnnotation(SettingContext.class).value() : null;
        return configurationObjectFactory.instantiate(context, keyPrefix, configurationField.getType());
    }

    @Override
    public Result<List<InjectionContainer<T>>> getProviders(Map<Class<?>, List<InjectionContainer<T>>> dependencyMap, ServiceExtensionContext context) {
        var violators = Arrays.stream(configurationField.getType().getDeclaredFields())
                .filter(f -> f.getAnnotation(Setting.class) != null)
                .<SettingInjectionPoint<T>>map(this::createInjectionPoint)
                .map(ip -> ip.getProviders(dependencyMap, context))
                .filter(Result::failed)
                .map(AbstractResult::getFailureDetail)
                .toList();

        return violators.isEmpty() ? Result.success(List.of()) : Result.failure("%s, through nested %s".formatted(asString(), violators));
    }

    private String asString() {
        return "Configuration object \"%s\" of type [%s]"
                .formatted(configurationField.getName(), configurationField.getType());
    }

    private <P> @NotNull SettingInjectionPoint<P> createInjectionPoint(Field field) {
        return new SettingInjectionPoint<>(null, field, null);
    }

}
