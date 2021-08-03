/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.spi.system;

import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Contributes a {@link Monitor} to the system during bootstrap.
 */
public interface MonitorExtension extends SystemExtension {

    /**
     * Returns the system monitor.
     */
    Monitor getMonitor();

}
