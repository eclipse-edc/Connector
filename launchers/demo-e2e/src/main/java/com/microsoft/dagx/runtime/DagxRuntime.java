/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.runtime;

import com.microsoft.dagx.monitor.MonitorProvider;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.system.DefaultServiceExtensionContext;

import java.util.List;
import java.util.ListIterator;

import static com.microsoft.dagx.system.ExtensionLoader.*;

/**
 * Main entrypoint for the default runtime.
 */
public class DagxRuntime {

    public static void main(String... arg) {
        TypeManager typeManager = new TypeManager();

        var monitor = loadMonitor();

        MonitorProvider.setInstance(monitor);

        DefaultServiceExtensionContext context = new DefaultServiceExtensionContext(typeManager, monitor);
        context.initialize();

        try {

            loadVault(context);

            List<ServiceExtension> serviceExtensions = context.loadServiceExtensions();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(serviceExtensions, monitor)));

            bootServiceExtensions(serviceExtensions, context);
        } catch (Exception e) {
            monitor.severe("Error booting runtime", e);
            System.exit(-1);  // stop the process
        }
        monitor.info("DA-GX ready");

    }

    private static void shutdown(List<ServiceExtension> serviceExtensions, Monitor monitor) {
        ListIterator<ServiceExtension> iter = serviceExtensions.listIterator(serviceExtensions.size());
        while (iter.hasPrevious()) {
            iter.previous().shutdown();
        }
        monitor.info("DA-GX shutdown complete");
    }


}
