package com.microsoft.dagx.junit;

import com.microsoft.dagx.monitor.MonitorProvider;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.system.DefaultServiceExtensionContext;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.util.List;

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

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) {
        var typeManager = new TypeManager();

        var monitor = loadMonitor();

        MonitorProvider.setInstance(monitor);

        context = new DefaultServiceExtensionContext(typeManager, monitor);
        context.initialize();

        try {
            addHttpClient(context);

            loadVault(context);

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
        if (Class.class.isAssignableFrom(type.getClass())) {
            //noinspection unchecked,rawtypes,rawtypes
            return context.hasService((Class) type);
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        var type = parameterContext.getParameter().getParameterizedType();
        if (Class.class.isAssignableFrom(type.getClass())) {
            //noinspection unchecked,rawtypes,rawtypes
            return context.getService((Class) type);
        }
        return null;
    }
}
