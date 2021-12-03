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

package org.eclipse.dataspaceconnector.transfer.store.cosmos;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.transfer.store.cosmos.model.TransferProcessDocument;

import java.util.Set;

/**
 * Provides an in-memory implementation of the {@link org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore} for testing.
 */
public class CosmosTransferProcessStoreExtension implements ServiceExtension {

    private static final String NAME = "Cosmos Transfer Process Store";

    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of("dataspaceconnector:transferprocessstore");
    }

    @Override
    public Set<String> requires() {
        return Set.of("dataspaceconnector:blobstoreapi", "edc:retry-policy");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        monitor.info(String.format("Initializing %s extension...", NAME));

        var vault = context.getService(Vault.class);
        var connectorId = context.getConnectorId();

        var retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        monitor.info("CosmosTransferProcessStore will use connector id '" + connectorId + "'");
        TransferProcessStoreCosmosConfig configuration = new TransferProcessStoreCosmosConfig(context);
        CosmosDbApi cosmosDbApi = new CosmosDbApiImpl(vault, configuration);
        context.registerService(TransferProcessStore.class, new CosmosTransferProcessStore(cosmosDbApi, context.getTypeManager(), configuration.getPartitionKey(), connectorId, retryPolicy));

        context.getTypeManager().registerTypes(TransferProcessDocument.class);
        monitor.info(String.format("Initialized %s extension", NAME));
    }

    @Override
    public void start() {
        monitor.info(String.format("Started Initialized %s extension", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown Initialized %s extension", NAME));
    }
}

