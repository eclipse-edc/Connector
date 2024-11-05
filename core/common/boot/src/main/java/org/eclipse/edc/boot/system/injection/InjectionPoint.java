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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.List;
import java.util.Map;

/**
 * Represents an auto-injectable property. Possible implementors are field injection points, constructor injection points, etc.
 *
 * @param <T> the type of the target object
 */
public interface InjectionPoint<T> {
    T getTargetInstance();

    Class<?> getType();

    boolean isRequired();

    Result<Void> setTargetValue(Object service) throws IllegalAccessException;

    ServiceProvider getDefaultServiceProvider();

    void setDefaultServiceProvider(ServiceProvider defaultServiceProvider);

    Object resolve(ServiceExtensionContext context, DefaultServiceSupplier defaultServiceSupplier);

    /**
     * Determines whether a particular injection can be resolved by a given map of dependencies or the context.
     *
     * @param dependencyMap a map containing the current dependency list
     * @param context       the fully constructed {@link ServiceExtensionContext}
     * @return success if it can be resolved, a failure otherwise.
     */
    Result<Void> isSatisfiedBy(Map<Class<?>, List<ServiceExtension>> dependencyMap, ServiceExtensionContext context);

    /**
     * A human-readable string indicating the type of injection point, e.g. "Service" or "Config value"
     */
    String getTypeString();
}
