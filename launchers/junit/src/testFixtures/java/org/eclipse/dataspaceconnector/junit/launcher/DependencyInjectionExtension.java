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

package org.eclipse.dataspaceconnector.junit.launcher;

import org.eclipse.dataspaceconnector.boot.system.injection.InjectorImpl;
import org.eclipse.dataspaceconnector.boot.system.injection.ReflectiveObjectFactory;
import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionPointScanner;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import static org.eclipse.dataspaceconnector.common.types.Cast.cast;

/**
 * A JUnit extension for running an embedded EDC dependency injection container as part of a test fixture.
 * This extension attaches a dependency injection container to the test lifecycle. Parameter injection of runtime services is supported.
 * <p>
 * If additional lifecycle services are needed (detection, loading and booting of extensions), use {@link EdcExtension} instead.
 */
public class DependencyInjectionExtension extends BaseRuntime implements BeforeEachCallback, ParameterResolver {
    private ServiceExtensionContext context;
    private ObjectFactory factory;

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        context = super.createServiceExtensionContext();
        context.initialize();
        factory = new ReflectiveObjectFactory(
                new InjectorImpl(),
                new InjectionPointScanner(),
                context
        );
    }

    @Override
    protected @NotNull ServiceExtensionContext createServiceExtensionContext() {
        return context;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(ObjectFactory.class)) {
            return true;
        } else if (type.equals(ServiceExtensionContext.class)) {
            return true;
        } else if (type instanceof Class) {
            return context.hasService(cast(type));
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(ServiceExtensionContext.class)) {
            return context;
        } else if (type.equals(ObjectFactory.class)) {
            return factory;
        } else if (type instanceof Class) {
            return context.getService(cast(type));
        }
        return null;
    }
}
