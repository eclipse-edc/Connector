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

package org.eclipse.edc.connector.store.azure.cosmos.policydefinition;

import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.azure.cosmos.CosmosClientProvider;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.store.azure.cosmos.policydefinition.model.PolicyDocument;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;

@Provides({ PolicyDefinitionStore.class })
@Extension(value = CosmosPolicyStoreExtension.NAME)
public class CosmosPolicyStoreExtension implements ServiceExtension {

    public static final String NAME = "CosmosDB Policy Store";
    @Inject
    private RetryPolicy<Object> retryPolicy;

    @Inject
    private TypeManager typeManager;

    @Inject
    private Monitor monitor;
    @Inject
    private CosmosClientProvider clientProvider;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configuration = new CosmosPolicyStoreConfig(context);

        var vault = context.getService(Vault.class);

        var cosmosDbApi = new CosmosDbApiImpl(configuration, clientProvider.createClient(vault, configuration));

        var store = new CosmosPolicyDefinitionStore(cosmosDbApi, typeManager, retryPolicy, configuration.getPartitionKey(), monitor);
        context.registerService(PolicyDefinitionStore.class, store);

        context.getTypeManager().registerTypes(PolicyDocument.class);

        context.getService(HealthCheckService.class).addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

    }

}

