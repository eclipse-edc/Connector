/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.spi.system;

import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Contributes capabilities and services
 */
public interface BootExtension extends SystemExtension {

    /**
     * Initializes the extension.
     *
     * @param monitor
     */
    default void initialize(Monitor monitor) {
    }

}
