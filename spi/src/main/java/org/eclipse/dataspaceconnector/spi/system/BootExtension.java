/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.system;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

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
