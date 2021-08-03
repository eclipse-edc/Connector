/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.system;

import java.util.Collections;
import java.util.Set;

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
     * Returns the list of features provided by this extension.
     */
    default Set<String> provides() {
        return Collections.emptySet();
    }

    /**
     * Returns the features this extension depends on. Dependencies must be in the same {@link LoadPhase} and will be ordered on runtime boot.
     */
    default Set<String> requires() {
        return Collections.emptySet();
    }

    /**
     * Returns the load phase for the extension.
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
