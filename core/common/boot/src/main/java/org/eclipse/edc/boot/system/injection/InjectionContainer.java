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
import org.eclipse.edc.spi.system.ServiceExtension;

import java.util.List;
import java.util.Set;

/**
 * Represents one {@link ServiceExtension} with a description of all its auto-injectable fields, which in turn are
 * represented by {@link ServiceInjectionPoint}s.
 */
public class InjectionContainer<T> {
    private final T injectionTarget;
    private final List<ServiceProvider> serviceProviders;
    private final Set<InjectionPoint<T>> injectionPoint;

    public InjectionContainer(T target, Set<InjectionPoint<T>> injectionPoint, List<ServiceProvider> serviceProviders) {
        injectionTarget = target;
        this.serviceProviders = serviceProviders;
        this.injectionPoint = injectionPoint;
    }

    public T getInjectionTarget() {
        return injectionTarget;
    }

    public Set<InjectionPoint<T>> getInjectionPoints() {
        return injectionPoint;
    }

    public List<ServiceProvider> getServiceProviders() {
        return serviceProviders;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "injectionTarget=" + injectionTarget +
                '}';
    }

}
