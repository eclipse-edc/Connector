package com.microsoft.dagx.junit;

import com.microsoft.dagx.monitor.MonitorProvider;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.system.DefaultServiceExtensionContext;
import okhttp3.Interceptor;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.LinkedHashMap;
import java.util.List;

import static com.microsoft.dagx.spi.util.Cast.cast;
import static com.microsoft.dagx.system.ExtensionLoader.addHttpClient;
import static com.microsoft.dagx.system.ExtensionLoader.bootServiceExtensions;
import static com.microsoft.dagx.system.ExtensionLoader.loadMonitor;
import static com.microsoft.dagx.system.ExtensionLoader.loadVault;

/**
 * A JUnit extension for running an embedded DA-GX runtime as part of a test fixture.
 *
 * This extension attaches a DA-GX runtime to the {@link BeforeTestExecutionCallback} and {@link AfterTestExecutionCallback} lifecycle hooks. Parameter injection of runtime services is supported.
 */
public class DagxExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {
    private List<ServiceExtension> serviceExtensions;
    private DefaultServiceExtensionContext context;

    private LinkedHashMap<Class<?>, Object> serviceMocks = new LinkedHashMap<>();

    /**
     * Registers a mock service with the runtime.
     *
     * @param mock the service mock
     */
    public <T> void registerServiceMock(Class<T> type, T mock) {
        serviceMocks.put(type, mock);
    }

    public void registerInterceptor(Interceptor interceptor) {
        
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        var typeManager = new TypeManager();

        var monitor = loadMonitor();

        MonitorProvider.setInstance(monitor);

        context = new DefaultServiceExtensionContext(typeManager, monitor);
        context.initialize();

        serviceMocks.forEach((key, value) -> context.registerService(cast(key), value));

        try {
            addHttpClient(context);

            if (!serviceMocks.containsKey(Vault.class)) {
                loadVault(context);
            }

            serviceExtensions = context.loadServiceExtensions();

            bootServiceExtensions(serviceExtensions, context);
        } catch (Exception e) {
            throw new DagxException(e);
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (serviceExtensions != null) {
            var iter = serviceExtensions.listIterator(serviceExtensions.size());
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

}
