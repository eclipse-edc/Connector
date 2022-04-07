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

/**
 * Represents an auto-injectable property. Possible implementors are field injection points, constructor injection points, etc.
 *
 * @param <T> the type of the target object
 */
public interface InjectionPoint<T> {
    T getInstance();

    String getFeatureName();

    Class<?> getType();

    boolean isRequired();

    void setTargetValue(Object service) throws IllegalAccessException;
}
