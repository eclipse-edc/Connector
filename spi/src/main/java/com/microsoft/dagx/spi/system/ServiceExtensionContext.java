package com.microsoft.dagx.spi.system;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.types.TypeManager;

/**
 * Context provided to extensions when they are initialized.
 */
public interface ServiceExtensionContext {

    /**
     * Returns the system monitor.
     */
    Monitor getMonitor();

    /**
     * Returns the type manager.
     */
    TypeManager getTypeManager();

    /**
     * Returns the configuration value, or the default value if not found.
     */
    <T> T getSetting(String setting, T defaultValue);

    /**
     * Returns a system service.
     */
    <T> T getService(Class<T> type);

    /**
     * Registers a service
     */
    default <T> void registerService(Class<T> type, T service) {
    }

}
