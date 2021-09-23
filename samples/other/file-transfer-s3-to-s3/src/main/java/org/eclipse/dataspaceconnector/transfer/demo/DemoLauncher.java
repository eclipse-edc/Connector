/*
 *  Copyright (c) 2021 Siemens AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Siemens AG - initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.demo;


import org.eclipse.dataspaceconnector.monitor.MonitorProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.system.DefaultServiceExtensionContext;

import java.util.List;
import java.util.ListIterator;

import static org.eclipse.dataspaceconnector.system.ExtensionLoader.bootServiceExtensions;
import static org.eclipse.dataspaceconnector.system.ExtensionLoader.loadMonitor;
import static org.eclipse.dataspaceconnector.system.ExtensionLoader.loadVault;

public class DemoLauncher {

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
        monitor.info("Dataspace Connector ready");

    }

    private static void shutdown(List<ServiceExtension> serviceExtensions, Monitor monitor) {
        ListIterator<ServiceExtension> iter = serviceExtensions.listIterator(serviceExtensions.size());
        while (iter.hasPrevious()) {
            iter.previous().shutdown();
        }
        monitor.info("Dataspace Connector shutdown complete");
    }

}
