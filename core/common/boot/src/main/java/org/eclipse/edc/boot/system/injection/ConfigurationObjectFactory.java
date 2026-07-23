/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class ConfigurationObjectFactory {

    /**
     * Instantiates a configuration object (record or POJO) from the context.
     */
    public Object instantiate(ServiceExtensionContext context, String keyPrefix, Class<?> type) {
        var allFields = Arrays.stream(type.getDeclaredFields())
                .map(f -> {
                    if (f.getAnnotation(Setting.class) != null) {
                        return toFieldValue(context, keyPrefix, f);
                    } else if (f.getAnnotation(Configuration.class) != null) {
                        return toConfigFieldValue(context, keyPrefix, f);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        if (type.isRecord()) {
            // records are treated specially, because they only contain final fields, and must be constructed with a non-default CTOR
            // where every constructor arg MUST be named the same as the field value. We can't rely on this with normal classes
            return instantiateFromRecord(type, allFields);
        } else {
            // all other classes MUST have a default constructor.
            return instantiateFromClass(type, allFields);
        }
    }

    /**
     * Instantiates a configuration Map from the context
     *
     * @param context the context.
     * @param keyPrefix the configuration prefix.
     * @param valueType the value type.
     * @return the map containing the configuration objects.
     */
    public Map<String, Object> instantiateMap(ServiceExtensionContext context, String keyPrefix, Class<?> valueType) {
        var baseConfig = keyPrefix != null ? context.getConfig(keyPrefix) : context.getConfig();
        var result = new HashMap<String, Object>();
        baseConfig.partition().forEach(partitionConfig -> {
            var partitionKey = partitionConfig.currentNode();
            var partitionPrefix = keyPrefix != null ? keyPrefix + "." + partitionKey : partitionKey;
            result.put(partitionKey, instantiate(context, partitionPrefix, valueType));
        });
        return result;
    }

    private @NotNull FieldValue toFieldValue(ServiceExtensionContext context, String keyPrefix, Field field) {
        var ip = new SettingInjectionPoint<>(null, field, keyPrefix);
        var value = ip.resolve(context, new InjectionPointDefaultServiceSupplier());
        return new FieldValue(field.getName(), value);
    }

    private @NotNull FieldValue toConfigFieldValue(ServiceExtensionContext context, String keyPrefix, Field field) {
        var configContext = field.getAnnotation(Configuration.class).context();
        if (configContext.isEmpty()) {
            throw new EdcInjectionException("Nested @Configuration field '%s' in '%s' must declare a non-empty context".formatted(field.getName(), field.getDeclaringClass().getName()));
        }
        var nestedPrefix = keyPrefix != null ? keyPrefix + "." + configContext : configContext;

        if (Map.class.isAssignableFrom(field.getType())) {
            var genericType = (ParameterizedType) field.getGenericType();
            var valueType = (Class<?>) genericType.getActualTypeArguments()[1];
            return new FieldValue(field.getName(), instantiateMap(context, nestedPrefix, valueType));
        } else {
            return new FieldValue(field.getName(), instantiate(context, nestedPrefix, field.getType()));
        }
    }

    private @NotNull Object instantiateFromClass(Class<?> type, List<FieldValue> settingsFields) {
        try {
            var defaultCtor = type.getDeclaredConstructor();
            defaultCtor.setAccessible(true);
            var instance = defaultCtor.newInstance();
            settingsFields.forEach(fe -> {
                try {
                    var field = type.getDeclaredField(fe.fieldName());
                    field.setAccessible(true);
                    field.set(instance, fe.value());
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new EdcInjectionException(e);
                }
            });
            return instance;
        } catch (NoSuchMethodException e) {
            throw new EdcInjectionException("Configuration objects must declare a default constructor, but '%s' does not.".formatted(type));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new EdcInjectionException(e);
        }
    }

    private Object instantiateFromRecord(Class<?> type, List<FieldValue> settingsFields) {
        var argNames = settingsFields.stream().map(FieldValue::fieldName).toList();
        var constructor = Stream.of(type.getDeclaredConstructors())
                .filter(ctor -> ctor.getParameterCount() == settingsFields.size() &&
                        Arrays.stream(ctor.getParameters()).allMatch(p -> argNames.contains(p.getName())))
                .findFirst()
                .orElseThrow(() -> new EdcInjectionException("No suitable constructor found on record class '%s'".formatted(type)));
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(settingsFields.stream().map(FieldValue::value).toArray());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new EdcInjectionException(e);
        }
    }

    record FieldValue(String fieldName, Object value) {
    }
}
