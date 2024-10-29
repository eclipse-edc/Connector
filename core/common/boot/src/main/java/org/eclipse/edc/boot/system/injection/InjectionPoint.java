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

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.boot.system.injection.lifecycle.ServiceProvider;

/**
 * Represents an auto-injectable property. Possible implementors are field injection points, constructor injection points, etc.
 *
 * @param <T> the type of the target object
 */
public interface InjectionPoint<T> {
    T getInstance();

    Class<?> getType();

    boolean isRequired();

    void setTargetValue(Object service) throws IllegalAccessException;

    ServiceProvider getDefaultServiceProvider();

    void setDefaultServiceProvider(ServiceProvider defaultServiceProvider);
}
