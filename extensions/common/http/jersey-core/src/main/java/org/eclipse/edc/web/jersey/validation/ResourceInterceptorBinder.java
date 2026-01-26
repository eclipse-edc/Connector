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

package org.eclipse.edc.web.jersey.validation;

import org.glassfish.jersey.inject.hk2.AbstractBinder;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

/**
 * Binds a concrete instance of a {@link ResourceInterceptorProvider} to a {@link ResourceMethodInvocationHandlerProvider}, thus enabling
 * the method interceptor mechanism.
 */
public class ResourceInterceptorBinder extends AbstractBinder {

    private final ResourceInterceptorProvider provider;

    public ResourceInterceptorBinder(ResourceInterceptorProvider provider) {
        this.provider = provider;
    }

    @Override
    protected void configure() {
        bindFactory(() -> provider).to(ResourceMethodInvocationHandlerProvider.class);
    }
}
