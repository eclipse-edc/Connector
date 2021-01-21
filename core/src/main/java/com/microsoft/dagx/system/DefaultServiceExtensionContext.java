package com.microsoft.dagx.system;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.types.TypeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * Base service extension context.
 */
public class DefaultServiceExtensionContext implements ServiceExtensionContext {
    private Monitor monitor;
    private TypeManager typeManager;
    private Map<Class<?>, Object> services = new HashMap<>();

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor) {
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public TypeManager getTypeManager() {
        return typeManager;
    }

    @Override
    public <T> T getSetting(String setting, T defaultValue) {
        return defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> type) {
        return (T) services.get(type);
    }

    @Override
    public <T> void registerService(Class<T> type, T service) {
        services.put(type, service);
    }


    @Override
    public <T> List<T> loadExtensions(Class<T> type) {
        List<T> extensions = new ArrayList<>();
        ServiceLoader.load(type).iterator().forEachRemaining(extensions::add);
        if (extensions.isEmpty()) {
            throw new DagxException("No extensions found of type:  " + type.getName());
        }
        return extensions;
    }

    @Override
    public <T> T loadSingletonExtension(Class<T> type) {
        List<T> extensions = new ArrayList<>();
        ServiceLoader.load(type).iterator().forEachRemaining(extensions::add);
        if (extensions.isEmpty()) {
            throw new DagxException("No extensions found of type:  " + type.getName());
        } else if (extensions.size() > 1) {
            String types = extensions.stream().map(e -> e.getClass().getName()).collect(joining(","));
            throw new DagxException(format("Multiple extensions found of type: %s [%s]", type.getName(), types));
        }
        return extensions.get(0);
    }
}
