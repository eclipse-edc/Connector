/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.did;

import org.eclipse.dataspaceconnector.boot.monitor.MonitorProvider;
import org.eclipse.dataspaceconnector.boot.system.DefaultServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.InjectionContainer;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import static org.eclipse.dataspaceconnector.boot.system.ExtensionLoader.bootServiceExtensions;
import static org.eclipse.dataspaceconnector.boot.system.ExtensionLoader.loadMonitor;
import static org.eclipse.dataspaceconnector.boot.system.ExtensionLoader.loadVault;

public class RegistrationServiceRuntime {

    public static void main(String[] args) {
        TypeManager typeManager = new TypeManager();
        var monitor = loadMonitor();
        MonitorProvider.setInstance(monitor);
        DefaultServiceExtensionContext context = new DefaultServiceExtensionContext(typeManager, monitor);
        context.initialize();

        try {
            loadVault(context);
            List<InjectionContainer<ServiceExtension>> serviceExtensions = context.loadServiceExtensions();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(serviceExtensions.stream().map(InjectionContainer::getInjectionTarget).collect(Collectors.toList()), monitor)));
            bootServiceExtensions(serviceExtensions, context);
        } catch (Exception e) {
            monitor.severe("Error booting runtime", e);
            System.exit(-1);  // stop the process
        }
        monitor.info("Registry Service App ready");

    }

    private static void shutdown(List<ServiceExtension> serviceExtensions, Monitor monitor) {
        ListIterator<ServiceExtension> iter = serviceExtensions.listIterator(serviceExtensions.size());
        while (iter.hasPrevious()) {
            var extension = iter.previous();
            extension.shutdown();
            monitor.info("Shutdown " + extension);
        }
        monitor.info("Registry Service App shutdown complete");
    }

}
