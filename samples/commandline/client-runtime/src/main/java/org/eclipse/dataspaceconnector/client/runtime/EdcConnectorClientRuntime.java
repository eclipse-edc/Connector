/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.client.runtime;

import org.eclipse.dataspaceconnector.monitor.MonitorProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.system.DefaultServiceExtensionContext;

import java.util.List;
import java.util.ListIterator;

import static org.eclipse.dataspaceconnector.system.ExtensionLoader.*;

public class EdcConnectorClientRuntime {
    private Monitor monitor;
    private TypeManager typeManager;
    private List<ServiceExtension> serviceExtensions;
    private DefaultServiceExtensionContext context;

    private EdcConnectorClientRuntime() {
    }

    public void start() {
        MonitorProvider.setInstance(monitor);

        context = new DefaultServiceExtensionContext(typeManager, monitor);
        context.initialize();

        try {
            loadVault(context);

            serviceExtensions = context.loadServiceExtensions();

            bootServiceExtensions(serviceExtensions, context);
            monitor.debug("Client runtime started");
        } catch (Exception e) {
            monitor.severe("Error booting runtime", e);   // do not stop the process as the client may be embedded
        }
    }

    public void shutdown() {
        ListIterator<ServiceExtension> iter = serviceExtensions.listIterator(serviceExtensions.size());
        while (iter.hasPrevious()) {
            iter.previous().shutdown();
        }
        monitor.debug("Client runtime shutdown complete");
    }

    public TypeManager getTypeManager() {
        return typeManager;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public <T> T getService(Class<T> type) {
        return context.getService(type);
    }

    public static class Builder {
        private final EdcConnectorClientRuntime runtime;
        private boolean immutable;

        private Builder() {
            runtime = new EdcConnectorClientRuntime();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder monitor(Monitor monitor) {
            checkImmutable();
            runtime.monitor = monitor;
            return this;
        }

        public Builder typeManager(TypeManager typeManager) {
            checkImmutable();
            runtime.typeManager = typeManager;
            return this;
        }

        public EdcConnectorClientRuntime build() {
            checkImmutable();
            immutable = true;

            if (runtime.monitor == null) {
                runtime.monitor = loadMonitor();
            }
            if (runtime.typeManager == null) {
                runtime.typeManager = new TypeManager();
            }

            return runtime;
        }

        private void checkImmutable() {
            if (immutable) {
                throw new IllegalStateException("Runtime has already been built");
            }
        }
    }
}
