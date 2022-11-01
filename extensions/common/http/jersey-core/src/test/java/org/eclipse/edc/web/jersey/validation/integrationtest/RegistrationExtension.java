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

package org.eclipse.edc.web.jersey.validation.integrationtest;

import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.validation.InterceptorFunction;
import org.eclipse.edc.web.spi.validation.InterceptorFunctionRegistry;

import java.lang.reflect.Method;

/**
 * This extension registers 3 InterceptorFunctions: 1 globally, 1 type-bound and 1 method-bound and it also registers the test controller.
 */
class RegistrationExtension implements ServiceExtension {
    private final Method method;
    private final InterceptorFunction function;
    private final Class<?> type;
    private final InterceptorFunction typeFunction;
    private final InterceptorFunction globalFunction;

    @Inject
    private InterceptorFunctionRegistry registry;

    @Inject
    private WebService webService;

    RegistrationExtension(Method method, InterceptorFunction function, Class<?> type, InterceptorFunction typeFunction, InterceptorFunction globalFunction) {
        this.method = method;
        this.function = function;
        this.type = type;
        this.typeFunction = typeFunction;
        this.globalFunction = globalFunction;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registry.addFunction(method, function);
        registry.addFunction(type, typeFunction);
        registry.addFunction(globalFunction);

        webService.registerResource(new TestController());
    }
}
