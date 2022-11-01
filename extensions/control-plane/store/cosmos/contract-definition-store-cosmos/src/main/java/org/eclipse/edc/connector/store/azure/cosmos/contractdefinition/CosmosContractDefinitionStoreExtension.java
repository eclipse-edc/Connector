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

package org.eclipse.edc.connector.store.azure.cosmos.contractdefinition;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosClientProvider;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.store.azure.cosmos.contractdefinition.model.ContractDefinitionDocument;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;

@Provides({ ContractDefinitionStore.class })
@Extension(value = CosmosContractDefinitionStoreExtension.NAME)
public class CosmosContractDefinitionStoreExtension implements ServiceExtension {

    public static final String NAME = "CosmosDB ContractDefinition Store";
    @Inject
    private Vault vault;

    @Inject
    private CosmosClientProvider clientProvider;

    @Inject
    private Monitor monitor;

    @Inject
    private RetryPolicy<Object> retryPolicy;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configuration = new CosmosContractDefinitionStoreConfig(context);

        var client = clientProvider.createClient(vault, configuration);
        var cosmosDbApi = new CosmosDbApiImpl(configuration, client);

        var store = new CosmosContractDefinitionStore(cosmosDbApi, typeManager, retryPolicy, configuration.getPartitionKey(), monitor);
        context.registerService(ContractDefinitionStore.class, store);

        context.getTypeManager().registerTypes(ContractDefinitionDocument.class);

        context.getService(HealthCheckService.class).addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

    }

}

