package com.microsoft.dagx.spi.system;

import com.microsoft.dagx.spi.monitor.Monitor;

/**
 * Contributes capabilities and services
 */
public interface BootExtension extends SystemExtension {

    /**
     * Initializes the extension.
     */
    default void initialize(Monitor monitor) {
    }

}
