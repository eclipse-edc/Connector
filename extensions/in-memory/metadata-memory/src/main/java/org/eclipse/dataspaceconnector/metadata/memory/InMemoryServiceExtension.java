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

package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.metadata.MetadataObservable;
import org.eclipse.dataspaceconnector.spi.metadata.MetadataStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryServiceExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public LoadPhase phase() {
        return LoadPhase.PRIMORDIAL;
    }

    @Override
    public Set<String> provides() {
        return Set.of("dataspaceconnector:metadata-store-observable");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        InMemoryMetadataStore service = new InMemoryMetadataStore();
        context.registerService(MetadataStore.class, service);
        context.registerService(MetadataObservable.class, service);

        monitor.info("Initialized In-Memory Metadata extension");
    }

    @Override
    public void start() {
        monitor.info("Started In-Memory Metadata extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown In-Memory Metadata extension");
    }
}
