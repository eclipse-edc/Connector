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

import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosClientProvider;
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
    @Inject
    private Vault vault;

    @Inject
    private CosmosClientProvider clientProvider;

    @Override
    public String name() {
        return "Cosmos Transfer Process Store";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var connectorId = context.getConnectorId();

        monitor.info("CosmosTransferProcessStore will use connector id '" + connectorId + "'");
        TransferProcessStoreCosmosConfig configuration = new TransferProcessStoreCosmosConfig(context);
        var client = clientProvider.createClient(vault, configuration);
        var cosmosDbApi = new CosmosDbApiImpl(configuration, client);
        context.registerService(TransferProcessStore.class, new CosmosTransferProcessStore(cosmosDbApi, context.getTypeManager(), configuration.getPartitionKey(), connectorId, retryPolicy));

        context.getTypeManager().registerTypes(TransferProcessDocument.class);

        healthService.addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

        if (context.getSetting(configuration.allowSprocAutoUploadSetting(), true)) {
            cosmosDbApi.uploadStoredProcedure("nextForState");
            cosmosDbApi.uploadStoredProcedure("lease");
        }
    }
}

