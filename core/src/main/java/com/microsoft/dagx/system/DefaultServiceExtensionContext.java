package com.microsoft.dagx.system;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.types.TypeManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Base service extension context.
 */
public class DefaultServiceExtensionContext implements ServiceExtensionContext {
    private Monitor monitor;
    private TypeManager typeManager;
    private Map<Class<?>, Object> services = new HashMap<>();

    public DefaultServiceExtensionContext(TypeManager typeManager, Monitor monitor) {
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
}
