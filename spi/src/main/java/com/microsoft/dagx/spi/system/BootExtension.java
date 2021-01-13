package com.microsoft.dagx.spi.system;

/**
 * Contributes capabilities and services
 */
public interface BootExtension extends SystemExtension {

    /**
     * Initializes the extension.
     */
    default void initialize() {
    }

}
