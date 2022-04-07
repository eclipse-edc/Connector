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

package org.eclipse.dataspaceconnector.junit.launcher;

import org.eclipse.dataspaceconnector.boot.system.DefaultServiceExtensionContext;
import org.eclipse.dataspaceconnector.boot.system.ServiceLocator;
import org.eclipse.dataspaceconnector.boot.system.ServiceLocatorImpl;
import org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ConfigurationExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.SystemExtension;
import org.eclipse.dataspaceconnector.spi.system.configuration.ConfigFactory;
import org.eclipse.dataspaceconnector.spi.system.injection.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.dataspaceconnector.common.types.Cast.cast;

/**
 * A JUnit extension for running an embedded EDC runtime as part of a test fixture.
 * This extension attaches an EDC runtime to the {@link BeforeTestExecutionCallback} and {@link AfterTestExecutionCallback} lifecycle hooks. Parameter injection of runtime services is supported.
 * <p>
 * If only basic dependency injection is needed, use {@link DependencyInjectionExtension} instead.
 */
public class EdcExtension extends BaseRuntime implements BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {
    private final LinkedHashMap<Class<?>, Object> serviceMocks = new LinkedHashMap<>();
    private List<ServiceExtension> runningServiceExtensions;
    private DefaultServiceExtensionContext context;

    public EdcExtension() {
        super(new MultiSourceServiceLocator());
    }

    /**
     * Registers a mock service with the runtime.
     *
     * @param mock the service mock
     */
    public <T> void registerServiceMock(Class<T> type, T mock) {
        serviceMocks.put(type, mock);
    }

    /**
     * Registers a service extension with the runtime.
     */
    public <T extends SystemExtension> void registerSystemExtension(Class<T> type, SystemExtension extension) {
        ((MultiSourceServiceLocator) serviceLocator).registerSystemExtension(type, extension);
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        bootWithoutShutdownHook();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        shutdown();
        // clear the systemExtensions map to prevent it from piling up between subsequent runs
        ((MultiSourceServiceLocator) serviceLocator).clearSystemExtensions();
    }

    @Override
    protected void bootExtensions(ServiceExtensionContext context, List<InjectionContainer<ServiceExtension>> serviceExtensions) {
        super.bootExtensions(context, serviceExtensions);
    }

    @Override
    protected @NotNull ServiceExtensionContext createContext(TypeManager typeManager, Monitor monitor, Telemetry telemetry) {
        this.context = new DefaultServiceExtensionContext(typeManager, monitor, telemetry, loadConfigurationExtensions());
        return this.context;
    }

    @Override
    protected void initializeContext(ServiceExtensionContext context) {
        serviceMocks.forEach((key, value) -> context.registerService(cast(key), value));
        super.initializeContext(context);
    }

    @Override
    protected void initializeVault(ServiceExtensionContext context) {
        if (!serviceMocks.containsKey(Vault.class)) {
            super.initializeVault(context);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(EdcExtension.class)) {
            return true;
        } else if (type instanceof Class) {
            return context.hasService(cast(type));
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(EdcExtension.class)) {
            return this;
        } else if (type instanceof Class) {
            return context.getService(cast(type));
        }
        return null;
    }

    public void setConfiguration(Map<String, String> configuration) {
        registerSystemExtension(ConfigurationExtension.class, (ConfigurationExtension) () -> ConfigFactory.fromMap(configuration));
    }

    /**
     * A service locator that allows additional extensions to be manually loaded by a test fixture. This locator return the union of registered extensions and extensions loaded
     * by the delegate.
     */
    private static class MultiSourceServiceLocator implements ServiceLocator {
        private final ServiceLocator delegate = new ServiceLocatorImpl();
        private final LinkedHashMap<Class<? extends SystemExtension>, List<SystemExtension>> systemExtensions;

        public MultiSourceServiceLocator() {
            this.systemExtensions  = new LinkedHashMap<>();
        }

        @Override
        public <T> List<T> loadImplementors(Class<T> type, boolean required) {
            List<T> extensions = cast(systemExtensions.getOrDefault(type, new ArrayList<>()));
            extensions.addAll(delegate.loadImplementors(type, required));
            return extensions;
        }

        /**
         * This implementation will override singleton implementions found by the delegate.
         */
        @Override
        public <T> T loadSingletonImplementor(Class<T> type, boolean required) {
            List<SystemExtension> extensions = systemExtensions.get(type);
            if (extensions == null || extensions.isEmpty()) {
                return delegate.loadSingletonImplementor(type, required);
            } else if (extensions.size() > 1) {
                throw new EdcException("Multiple extensions were registered for type: " + type.getName());
            }
            return type.cast(extensions.get(0));
        }

        public <T extends SystemExtension> void registerSystemExtension(Class<T> type, SystemExtension extension) {
            systemExtensions.computeIfAbsent(type, k -> new ArrayList<>()).add(extension);
        }

        public void clearSystemExtensions() {
            systemExtensions.clear();
        }
    }

}