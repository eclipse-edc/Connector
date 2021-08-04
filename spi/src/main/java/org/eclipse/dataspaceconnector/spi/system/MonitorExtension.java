/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.system;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

/**
 * Contributes a {@link Monitor} to the system during bootstrap.
 */
public interface MonitorExtension extends SystemExtension {

    /**
     * Returns the system monitor.
     */
    Monitor getMonitor();

}
