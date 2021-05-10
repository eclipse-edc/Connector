/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.junit;

import com.microsoft.dagx.monitor.MonitorProvider;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.SystemExtension;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.system.DefaultServiceExtensionContext;
import com.microsoft.dagx.system.ServiceLocator;
import com.microsoft.dagx.system.ServiceLocatorImpl;
import okhttp3.Interceptor;
import org.junit.jupiter.api.extension.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.microsoft.dagx.spi.util.Cast.cast;
import static com.microsoft.dagx.system.ExtensionLoader.*;

/**
 * A JUnit extension for running an embedded DA-GX runtime as part of a test fixture.
 * <p>
 * This extension attaches a DA-GX runtime to the {@link BeforeTestExecutionCallback} and {@link AfterTestExecutionCallback} lifecycle hooks. Parameter injection of runtime services is supported.
 */
public class DagxExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {
    private List<ServiceExtension> runningServiceExtensions;
    private DefaultServiceExtensionContext context;

    private final LinkedHashMap<Class<?>, Object> serviceMocks = new LinkedHashMap<>();
    private final LinkedHashMap<Class<? extends SystemExtension>, List<SystemExtension>> systemExtensions = new LinkedHashMap<>();

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
        systemExtensions.computeIfAbsent(type, k -> new ArrayList<>()).add(extension);
    }

    public void registerInterceptor(Interceptor interceptor) {

    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        var typeManager = new TypeManager();

        var monitor = loadMonitor();

        MonitorProvider.setInstance(monitor);

        context = new DefaultServiceExtensionContext(typeManager, monitor, new MultiSourceServiceLocator());
        context.initialize();

        serviceMocks.forEach((key, value) -> context.registerService(cast(key), value));

        try {
            if (!serviceMocks.containsKey(Vault.class)) {
                loadVault(context);
            }

            runningServiceExtensions = context.loadServiceExtensions();

            bootServiceExtensions(runningServiceExtensions, context);
        } catch (Exception e) {
            throw new DagxException(e);
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (runningServiceExtensions != null) {
            var iter = runningServiceExtensions.listIterator(runningServiceExtensions.size());
            while (iter.hasPrevious()) {
                iter.previous().shutdown();
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(DagxExtension.class)) {
            return true;
        } else if (type instanceof Class) {
            //noinspection unchecked,rawtypes,rawtypes
            return context.hasService((Class) type);
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (type.equals(DagxExtension.class)) {
            return this;
        } else if (type instanceof Class) {
            //noinspection unchecked,rawtypes,rawtypes
            return context.getService((Class) type);
        }
        return null;
    }

    /**
     * A service locator that allows additional extensions to be manually loaded by a test fixture. This locator return the union of registered extensions and extensions loaded
     * by the delegate.
     */
    private class MultiSourceServiceLocator implements ServiceLocator {
        private final ServiceLocator delegate = new ServiceLocatorImpl();

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
                throw new DagxException("Multiple extensions were registered for type: " + type.getName());
            }
            return type.cast(extensions.get(0));
        }
    }

}
