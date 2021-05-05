/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.system;

import com.microsoft.dagx.spi.monitor.Monitor;

/**
 * Contributes a {@link Monitor} to the system during bootstrap.
 */
public interface MonitorExtension extends SystemExtension {

    /**
     * Returns the system monitor.
     */
    Monitor getMonitor();

}
