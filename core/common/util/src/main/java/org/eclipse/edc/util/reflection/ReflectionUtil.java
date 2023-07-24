/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.util.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReflectionUtil {

    private static final String ARRAY_INDEXER_REGEX = ".*\\[([0-9])+\\]";
    private static final String OPENING_BRACKET = "[";
    private static final String CLOSING_BRACKET = "]";

    /**
     * Utility function to get value of a field from an object. For field names currently the dot notation and array
     * indexers are supported:
     * <pre>
     *     someObject.someValue
     *     someObject[2].someValue //someObject must impement the List interface
     * </pre>
     *
     * @param object       The object
     * @param propertyName The name of the field
     * @return The field's value.
     * @throws ReflectionException if the field does not exist or is not accessible
     */
    public static <T> T getFieldValue(String propertyName, Object object) {
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(object, "object");

        if (propertyName.contains(".")) {
            var dotIx = propertyName.indexOf(".");
            var field = propertyName.substring(0, dotIx);
            var rest = propertyName.substring(dotIx + 1);
            object = getFieldValue(field, object);
            if (object == null) {
                return null;
            }
            return getFieldValue(rest, object);
        } else if (propertyName.matches(ARRAY_INDEXER_REGEX)) { //array indexer
            var openingBracketIx = propertyName.indexOf(OPENING_BRACKET);
            var closingBracketIx = propertyName.indexOf(CLOSING_BRACKET);
            var propName = propertyName.substring(0, openingBracketIx);
            var arrayIndex = Integer.parseInt(propertyName.substring(openingBracketIx + 1, closingBracketIx));
            var iterableObject = (List) getFieldValue(propName, object);
            return (T) iterableObject.get(arrayIndex);
        } else {
            if (object instanceof Map<?, ?> map) {
                return (T) map.get(propertyName);
            } else if (object instanceof List<?> list) {
                return (T) list.stream().filter(Objects::nonNull).map(it -> getRecursiveValue(propertyName, it)).toList();
            } else {
                return getRecursiveValue(propertyName, object);
            }
        }
    }

    /**
     * Gets a field with a given name from all declared fields of a class including supertypes. Will include protected
     * and private fields.
     *
     * @param clazz     The class of the object
     * @param fieldName The fieldname
     * @return A field with the given name, null if the field does not exist
     */
    public static Field getFieldRecursive(Class<?> clazz, String fieldName) {
        return getAllFieldsRecursive(clazz).stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
    }

    /**
     * Recursively gets all fields declared in the class and all its superclasses
     *
     * @param clazz The class of the object
     * @return A list of {@link Field}s
     */
    public static List<Field> getAllFieldsRecursive(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }

        List<Field> result = new ArrayList<>(getAllFieldsRecursive(clazz.getSuperclass()));
        var filteredFields = Arrays.stream(clazz.getDeclaredFields()).toList();
        result.addAll(filteredFields);
        return result;
    }

    private static <T> T getRecursiveValue(String propertyName, Object object) {
        var field = getFieldRecursive(object.getClass(), propertyName);
        if (field == null) {
            throw new ReflectionException(propertyName);
        }
        field.setAccessible(true);
        try {
            return (T) field.get(object);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e);
        }
    }

    /**
     * Get the first type argument for the given target from the given clazz.
     * It goes through the hierarchy starting from class and looking for target
     * And return the first type argument of the target
     *
     * @param clazz The class of the object
     * @return The type argument {@link Class} or null
     */
    public static Class<?> getSingleSuperTypeGenericArgument(Class<?> clazz, Class<?> target) {
        var supertype = clazz.getGenericSuperclass();
        var superclass = clazz.getSuperclass();
        while (superclass != null && superclass != Object.class) {
            if (supertype instanceof ParameterizedType pt) {
                if (pt.getRawType() == target) {
                    return getSingleTypeArgument(supertype);
                }
            }

            supertype = superclass.getGenericSuperclass();
            superclass = superclass.getSuperclass();
        }
        return null;
    }

    /**
     * If the Type is a ParameterizedType return the actual type of the first type parameter
     *
     * @param genericType The genericType
     * @return The class of the type argument
     */
    private static Class<?> getSingleTypeArgument(Type genericType) {
        if (genericType instanceof ParameterizedType pt) {
            var actualTypeArguments = pt.getActualTypeArguments();
            if (actualTypeArguments.length == 1) {
                var actualTypeArgument = actualTypeArguments[0];

                if (actualTypeArgument instanceof Class<?> clazz) {
                    return clazz;
                }
                if (actualTypeArgument instanceof ParameterizedType actualParametrizedType) {
                    var rawType = actualParametrizedType.getRawType();
                    if (rawType instanceof Class) {
                        return (Class<?>) rawType;
                    }
                }
            }
        }
        return null;
    }

}
