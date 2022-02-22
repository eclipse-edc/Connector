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

package org.eclipse.dataspaceconnector.common.reflection;

import java.util.Objects;

public class ReflectionUtil {

    /**
     * Utility function to get value of a field from an object
     *
     * @param object       The object
     * @param propertyName The name of the field
     * @return The field's value.
     * @throws ReflectionException if the field does not exist or is not accessible
     */
    public static <T> T getFieldValue(String propertyName, Object object) {
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(object, "object");
        try {
            var field = object.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ReflectionException(e);
        }
    }

    /**
     * Utility function to get value of a field from an object. Essentially the same as {@link ReflectionUtil#getFieldValue(String, Object)}
     * but it does not throw an exception
     *
     * @param object       The object
     * @param propertyName The name of the field
     * @return The field's value. Returns null if the field does not exist or is inaccessible.
     */
    public static <T> T getFieldValueSilent(String propertyName, Object object) {
        try {
            return getFieldValue(propertyName, object);
        } catch (ReflectionException ignored) {
            return null;
        }
    }


}
