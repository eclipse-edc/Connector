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

import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * Sets all fields of a {@link ServiceExtension}, that are annotated with {@code @Inject} by attempting to get the
 * corresponding service from the {@link ServiceExtensionContext}.
 * <p>
 * Injectors must throw an {@link EdcInjectionException} should they fail to set any field's value.
 */
@FunctionalInterface
public interface Injector {
    /**
     * Attempts to set all fields (i.e. {@link InjectionContainer#getInjectionPoints()}) of a service extension (i.e. {@link InjectionContainer#getInjectionTarget()})
     * by attempting to resolve as service of the field's type from the context.
     */
    <T> T inject(InjectionContainer<T> container, ServiceExtensionContext context);
}
