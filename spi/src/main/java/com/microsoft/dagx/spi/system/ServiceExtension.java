package com.microsoft.dagx.spi.system;

/**
 * Contributes services used by the runtime.
 *
 * Service extensions are started after system boostrap.
 */
public interface ServiceExtension extends SystemExtension {
    /**
     * Defines the load sequence for extensions.
     */
    enum LoadPhase {PRIMORDIAL, DEFAULT}

    /**
     * Returns the load sequence for the extension.
     */
    default LoadPhase phase() {
        return LoadPhase.DEFAULT;
    }

    /**
     * Initializes the extension.
     */
    default void initialize(ServiceExtensionContext context) {
    }

    /**
     * Signals the extension to prepare for the runtime to receive requests.
     */
    default void start() {
    }

    /**
     * Signals the extension to release resources and shutdown.
     */
    default void shutdown() {
    }
}
