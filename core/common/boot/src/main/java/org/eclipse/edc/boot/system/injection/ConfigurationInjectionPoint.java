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
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.ValueProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
    private final Field configurationObject;

    public ConfigurationInjectionPoint(T instance, Field configurationObject) {
        this.targetInstance = instance;
        this.configurationObject = configurationObject;
        this.configurationObject.setAccessible(true);

    }

    @Override
    public T getTargetInstance() {
        return targetInstance;
    }

    @Override
    public Class<?> getType() {
        return configurationObject.getType();
    }

    @Override
    public boolean isRequired() {
        return Arrays.stream(configurationObject.getType().getDeclaredFields())
                .filter(f -> f.getAnnotation(Setting.class) != null)
                .anyMatch(f -> f.getAnnotation(Setting.class).required());
    }

    @Override
    public Result<Void> setTargetValue(Object value) {
        try {
            configurationObject.set(targetInstance, value);
            return Result.success();
        } catch (IllegalAccessException e) {
            return Result.failure("Could not assign value '%s' to field '%s'. Reason: %s".formatted(value, configurationObject, e.getMessage()));
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

        // all fields annotated with the @Setting annotation
        var settingsFields = resolveSettingsFields(context, configurationObject.getType().getDeclaredFields(), configurationObject.getAnnotation(SettingContext.class));

        // records are treated specially, because they only contain final fields, and must be constructed with a non-default CTOR
        // where every constructor arg MUST be named the same as the field value. We can't rely on this with normal classes
        if (configurationObject.getType().isRecord()) {
            // find matching constructor
            var constructor = Stream.of(configurationObject.getType().getDeclaredConstructors())
                    .filter(constructorFilter(settingsFields))
                    .findFirst()
                    .orElseThrow(() -> new EdcInjectionException("No suitable constructor found on record class '%s'".formatted(configurationObject.getType())));

            try {
                // invoke CTor with the previously resolved config values
                constructor.setAccessible(true);
                return constructor.newInstance(settingsFields.stream().map(FieldValue::value).toArray());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new EdcInjectionException(e);
            }

        } else { // all other classes MUST have a default constructor.
            try {
                var pojoClass = configurationObject.getType();
                var defaultCtor = pojoClass.getDeclaredConstructor();
                defaultCtor.setAccessible(true);
                var instance = defaultCtor.newInstance();

                // set the field values on the newly-constructed object instance
                settingsFields.forEach(fe -> {
                    try {
                        var field = pojoClass.getDeclaredField(fe.fieldName());
                        field.setAccessible(true);
                        field.set(instance, fe.value());
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        throw new EdcInjectionException(e);
                    }
                });

                return instance;
            } catch (NoSuchMethodException e) {
                throw new EdcInjectionException("Configuration objects must declare a default constructor, but '%s' does not.".formatted(configurationObject.getType()));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new EdcInjectionException(e);
            }
        }
    }

    @Override
    public Result<List<InjectionContainer<T>>> getProviders(Map<Class<?>, List<InjectionContainer<T>>> dependencyMap, ServiceExtensionContext context) {
        var violators = injectionPointsFrom(configurationObject.getType().getDeclaredFields(), null)
                .map(ip -> ip.getProviders(dependencyMap, context))
                .filter(Result::failed)
                .map(AbstractResult::getFailureDetail)
                .toList();
        return violators.isEmpty() ? Result.success(List.of()) : Result.failure("%s, through nested settings %s".formatted(toString(), violators));
    }

    @Override
    public String getTypeString() {
        return "Configuration object";
    }

    @Override
    public String toString() {
        return "Configuration object \"%s\" of type [%s]"
                .formatted(configurationObject.getName(), configurationObject.getType());
    }

    private Predicate<Constructor<?>> constructorFilter(List<FieldValue> args) {
        var argNames = args.stream().map(FieldValue::fieldName).toList();
        return ctor -> ctor.getParameterCount() == args.size() &&
                       Arrays.stream(ctor.getParameters()).allMatch(p -> argNames.contains(p.getName()));

    }

    private @NotNull List<FieldValue> resolveSettingsFields(ServiceExtensionContext context, Field[] fields, SettingContext settingContext) {
        return injectionPointsFrom(fields, settingContext)
                .map(ip -> {
                    var val = ip.resolve(context, new InjectionPointDefaultServiceSupplier());
                    var fieldName = ip.getTargetField().getName();
                    return new FieldValue(fieldName, val);
                })
                .toList();
    }

    private @NotNull Stream<ValueInjectionPoint<T>> injectionPointsFrom(Field[] fields, SettingContext settingContext) {
        return Arrays.stream(fields)
                .filter(f -> f.getAnnotation(Setting.class) != null)
                .map(f -> {
                    if (settingContext == null) {
                        return new ValueInjectionPoint<>(null, f, f.getAnnotation(Setting.class), targetInstance.getClass());
                    } else {
                        return new ValueInjectionPoint<>(null, f, f.getAnnotation(Setting.class), targetInstance.getClass(), settingContext);
                    }
                });
    }

    private record FieldValue(String fieldName, Object value) {
    }
}
