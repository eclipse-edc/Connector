/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.edc.junit.extensions;

import org.eclipse.edc.spi.system.ConfigurationExtension;
import org.eclipse.edc.spi.system.SystemExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.util.types.Cast.cast;

/**
 * A JUnit extension for running an embedded EDC runtime as part of a test fixture. This extension attaches an EDC
 * runtime to the {@link BeforeTestExecutionCallback} and {@link AfterTestExecutionCallback} lifecycle hooks. Parameter
 * injection of runtime services is supported.
 * <p>
 * If only basic dependency injection is needed, use {@link DependencyInjectionExtension} instead.
 *
 * @deprecated please use either {@link RuntimePerMethodExtension} or {@link RuntimePerClassExtension}.
 */
@Deprecated(since = "0.7.0")
public class EdcExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {
    protected final EmbeddedRuntime runtime;

    public EdcExtension() {
        this(new EmbeddedRuntime("runtime", emptyMap()));
    }

    protected EdcExtension(EmbeddedRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Registers a mock service with the runtime. Note that the mock will overwrite any service already registered with the context.
     * This is intentional. A warning will be logged to STDOUT if the mock actually replaces an existing service.
     *
     * @param mock the service mock
     */
    public <T> void registerServiceMock(Class<T> type, T mock) {
        runtime.registerServiceMock(type, mock);
    }

    /**
     * Registers a service extension with the runtime.
     */
    public <T extends SystemExtension> void registerSystemExtension(Class<T> type, SystemExtension extension) {
        runtime.registerSystemExtension(type, extension);
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        runtime.boot(false);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        runtime.shutdown();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(EdcExtension.class)) {
            return true;
        } else if (type.equals(EmbeddedRuntime.class)) {
            return true;
        } else if (type instanceof Class) {
            return runtime.getContext().hasService(cast(type));
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(EdcExtension.class)) {
            return this;
        } else if (type.equals(EmbeddedRuntime.class)) {
            return runtime;
        } else if (type instanceof Class) {
            return runtime.getContext().getService(cast(type));
        }
        return null;
    }

    public void setConfiguration(Map<String, String> configuration) {
        registerSystemExtension(ConfigurationExtension.class, (ConfigurationExtension) () -> ConfigFactory.fromMap(configuration));
    }

    public <T> T getService(Class<T> clazz) {
        return runtime.getService(clazz);
    }

}
