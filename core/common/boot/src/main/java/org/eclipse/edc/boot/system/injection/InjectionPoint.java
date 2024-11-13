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
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents an auto-injectable property. Possible implementors are field injection points, constructor injection points, etc.
 *
 * @param <T> the type of the target object
 */
public interface InjectionPoint<T> {
    /**
     * The fully constructed object instance that contains the injection point. For example an extension class
     */
    T getTargetInstance();

    /**
     * The type (=class) of the injected field. For example, this could be the service class for a {@link ServiceInjectionPoint}, or
     * a basic datatype for a {@link ValueInjectionPoint}.
     */
    Class<?> getType();

    /**
     * Whether the injection point must be able to be satisfied from the current runtime. In other words, whether the injected field is nullable.
     */
    boolean isRequired();

    /**
     * Assigns the given value to the injected field.
     *
     * @param value An object instance that is assigned to the injected field.
     * @return a successful result if the assignment could be done, a failure result indicating the reason in other cases.
     */
    Result<Void> setTargetValue(Object value);

    /**
     * Some injection points such as service injection points may have a default value provider.
     *
     * @return the default service provider if any, null otherwise
     */
    @Nullable ServiceProvider getDefaultServiceProvider();

    /**
     * Sets the default service provider.
     *
     * @param defaultServiceProvider the default service provider
     */
    void setDefaultServiceProvider(ServiceProvider defaultServiceProvider);

    /**
     * Resolves the value for an injected field from either the context or a default service supplier. For some injection points,
     * this may also return a (statically declared) default value.
     *
     * @param context                The {@link ServiceExtensionContext} from which the value is resolved.
     * @param defaultServiceSupplier Some service dynamically resolve a default value in case the actual value isn't found on the context.
     * @return The resolved value, or null if the injected field is not required..
     * @throws EdcInjectionException in case the value could not be resolved
     */
    Object resolve(ServiceExtensionContext context, DefaultServiceSupplier defaultServiceSupplier);

    /**
     * Determines whether a particular injection can be resolved by a given map of dependencies or the context.
     *
     * @param dependencyMap a map containing the current dependency list
     * @param context       the fully constructed {@link ServiceExtensionContext}
     * @return successful result containing a (potentially empty) list of injection containers that can provide this service, a failure otherwise.
     */
    Result<List<InjectionContainer<T>>> getProviders(Map<Class<?>, List<InjectionContainer<T>>> dependencyMap, ServiceExtensionContext context);

    /**
     * A human-readable string indicating the type of injection point, e.g. "Service" or "Config value"
     */
    String getTypeString();
}
