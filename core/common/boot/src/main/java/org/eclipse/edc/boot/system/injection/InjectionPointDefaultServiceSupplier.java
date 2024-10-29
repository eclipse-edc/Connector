/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

/**
 * Supplies the default {@link org.eclipse.edc.boot.system.injection.lifecycle.ServiceProvider} that has been stored in
 * the {@link InjectionPoint}
 */
public class InjectionPointDefaultServiceSupplier implements DefaultServiceSupplier {

    @Override
    public @Nullable Object provideFor(InjectionPoint<?> injectionPoint, ServiceExtensionContext context) {
        var defaultService = injectionPoint.getDefaultServiceProvider();
        if (injectionPoint.isRequired() && defaultService == null) {
            throw new EdcInjectionException("No default provider for required service " + injectionPoint.getType());
        }
        return defaultService == null ? null : defaultService.register(context);
    }

}
