/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.contract.negotiation.store;

import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosClientProvider;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.contract.negotiation.store.model.ContractNegotiationDocument;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

import java.time.Clock;


@Provides({ ContractNegotiationStore.class })
public class CosmosContractNegotiationStoreExtension implements ServiceExtension {

    @Inject
    private Vault vault;

    @Inject
    private CosmosClientProvider clientProvider;

    @Inject
    private Clock clock;

    @Override
    public String name() {
        return "CosmosDB ContractNegotiation Store";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configuration = new CosmosContractNegotiationStoreConfig(context);

        var cosmosDbApi = new CosmosDbApiImpl(configuration, clientProvider.createClient(vault, configuration));
        var store = new CosmosContractNegotiationStore(cosmosDbApi, context.getTypeManager(), (RetryPolicy<Object>) context.getService(RetryPolicy.class), configuration.getPartitionKey(), clock);
        context.registerService(ContractNegotiationStore.class, store);

        context.getTypeManager().registerTypes(ContractNegotiationDocument.class);

        context.getService(HealthCheckService.class).addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

        if (context.getSetting(configuration.allowSprocAutoUploadSetting(), true)) {
            cosmosDbApi.uploadStoredProcedure("nextForState");
            cosmosDbApi.uploadStoredProcedure("lease");
        }
    }

}

