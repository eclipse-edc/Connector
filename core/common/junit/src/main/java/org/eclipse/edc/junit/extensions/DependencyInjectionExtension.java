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

package org.eclipse.edc.junit.extensions;

import org.eclipse.edc.boot.system.injection.InjectionPointScanner;
import org.eclipse.edc.boot.system.injection.InjectorImpl;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.boot.system.runtime.BaseRuntime;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.Mockito;

import static org.eclipse.edc.util.types.Cast.cast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * A JUnit extension for running an embedded EDC dependency injection container as part of a test fixture. This
 * extension attaches a dependency injection container to the test lifecycle. Parameter injection of runtime services is
 * supported.
 * <p>
 * If additional lifecycle services are needed (detection, loading and booting of extensions), use {@link EdcExtension}
 * instead.
 * The {@link ServiceExtensionContext} instance is wrapped by a Mockito Spy.
 */
public class DependencyInjectionExtension extends BaseRuntime implements BeforeEachCallback, ParameterResolver {

    private ServiceExtensionContext context;
    private ObjectFactory factory;

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        monitor = mock();
        context = spy(super.createServiceExtensionContext(ConfigFactory.empty()));
        context.initialize();
        factory = new ReflectiveObjectFactory(
                new InjectorImpl(Mockito::mock),
                new InjectionPointScanner(),
                context
        );
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(ObjectFactory.class)) {
            return true;
        } else if (type.equals(ServiceExtensionContext.class)) {
            return true;
        } else if (type instanceof Class<?> clazz) {
            if (ServiceExtension.class.isAssignableFrom(clazz)) {
                return true;
            }
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
        } else if (type instanceof Class<?> clazz) {
            if (ServiceExtension.class.isAssignableFrom(clazz)) {
                return factory.constructInstance(clazz);
            }
            return context.getService(cast(type));
        }
        return null;
    }

}
