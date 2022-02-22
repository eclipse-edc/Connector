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
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.transfer.store.cosmos.model.TransferProcessDocument;

/**
 * Provides an in-memory implementation of the {@link org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore} for testing.
 */
@Provides(TransferProcessStore.class)
public class CosmosTransferProcessStoreExtension implements ServiceExtension {

    @Inject
    private RetryPolicy<Object> retryPolicy;
    @Inject
    private HealthCheckService healthService;

    @Override
    public String name() {
        return "Cosmos Transfer Process Store";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();


        var vault = context.getService(Vault.class);
        var connectorId = context.getConnectorId();

        retryPolicy = (RetryPolicy<Object>) context.getService(RetryPolicy.class);
        monitor.info("CosmosTransferProcessStore will use connector id '" + connectorId + "'");
        TransferProcessStoreCosmosConfig configuration = new TransferProcessStoreCosmosConfig(context);
        var cosmosDbApi = new CosmosDbApiImpl(vault, configuration);
        context.registerService(TransferProcessStore.class, new CosmosTransferProcessStore(cosmosDbApi, context.getTypeManager(), configuration.getPartitionKey(), connectorId, retryPolicy));

        context.getTypeManager().registerTypes(TransferProcessDocument.class);

        healthService.addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

    }
}

