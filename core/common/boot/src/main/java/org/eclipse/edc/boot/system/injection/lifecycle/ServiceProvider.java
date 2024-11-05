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

package org.eclipse.edc.boot.system.injection.lifecycle;

import org.eclipse.edc.boot.system.injection.ProviderMethod;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.ValueProvider;

/**
 * Represent a service provider, that's a method annotated with the {@link Provider}
 *
 * @param method    the provider method.
 * @param extension the extension in which the method is contained.
 */
public record ServiceProvider(ProviderMethod method, ServiceExtension extension) implements ValueProvider {

    /**
     * Call the method and register the service.
     *
     * @param context the service context.
     * @return the instantiated service.
     */
    @Override
    public Object apply(ServiceExtensionContext context) {
        var type = method.getReturnType();
        var service = method.invoke(extension, context);
        context.registerService(type, service);
        return service;
    }
}
