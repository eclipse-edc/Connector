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

package org.eclipse.edc.connector.store.azure.cosmos.transferprocess;

import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.azure.cosmos.CosmosClientProvider;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.connector.store.azure.cosmos.transferprocess.model.TransferProcessDocument;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;

import java.time.Clock;


/**
 * Provides an in-memory implementation of the {@link TransferProcessStore} for testing.
 */
@Provides(TransferProcessStore.class)
@Extension(value = CosmosTransferProcessStoreExtension.NAME)
public class CosmosTransferProcessStoreExtension implements ServiceExtension {

    public static final String NAME = "Cosmos Transfer Process Store";
    @Inject
    private RetryPolicy<Object> retryPolicy;

    @Inject
    private HealthCheckService healthService;
    @Inject
    private Vault vault;

    @Inject
    private CosmosClientProvider clientProvider;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var connectorId = context.getConnectorId();

        monitor.debug("CosmosTransferProcessStore will use connector id '" + connectorId + "'");
        TransferProcessStoreCosmosConfig configuration = new TransferProcessStoreCosmosConfig(context);
        var client = clientProvider.createClient(vault, configuration);
        var cosmosDbApi = new CosmosDbApiImpl(configuration, client);
        context.registerService(TransferProcessStore.class, new CosmosTransferProcessStore(cosmosDbApi, context.getTypeManager(), configuration.getPartitionKey(), connectorId, retryPolicy, clock));

        context.getTypeManager().registerTypes(TransferProcessDocument.class);

        healthService.addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

        if (context.getSetting(configuration.allowSprocAutoUploadSetting(), true)) {
            cosmosDbApi.uploadStoredProcedure("nextForState");
            cosmosDbApi.uploadStoredProcedure("lease");
        }
    }
}

