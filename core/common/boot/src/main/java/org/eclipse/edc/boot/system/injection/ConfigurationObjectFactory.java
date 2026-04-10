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

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ConfigurationObjectFactory {

    /**
     * Instantiates a configuration object (record or POJO) from the context.
     */
    public Object instantiate(ServiceExtensionContext context, String keyPrefix, Class<?> type) {
        var settingsFields = Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.getAnnotation(Setting.class) != null)
                .map(f -> toFieldValue(context, keyPrefix, f))
                .toList();

        if (type.isRecord()) {
            // records are treated specially, because they only contain final fields, and must be constructed with a non-default CTOR
            // where every constructor arg MUST be named the same as the field value. We can't rely on this with normal classes
            return instantiateFromRecord(type, settingsFields);
        } else {
            // all other classes MUST have a default constructor.
            return instantiateFromClass(type, settingsFields);
        }
    }

    private @NotNull FieldValue toFieldValue(ServiceExtensionContext context, String keyPrefix, Field field) {
        var ip = new SettingInjectionPoint<>(null, field, keyPrefix);
        var value = ip.resolve(context, new InjectionPointDefaultServiceSupplier());
        return new FieldValue(field.getName(), value);
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
