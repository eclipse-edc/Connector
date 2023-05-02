/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.store.cosmos;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosClientProvider;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.connector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.edc.connector.dataplane.store.cosmos.model.DataFlowRequestDocument;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;

@Extension(value = CosmosDataPlaneStoreExtension.NAME)
@Provides(DataPlaneStore.class)
public class CosmosDataPlaneStoreExtension implements ServiceExtension {

    public static final String NAME = "CosmosDB Data Plane Store";

    @Inject
    private Vault vault;
    @Inject
    private CosmosClientProvider clientProvider;
    @Inject
    private RetryPolicy<Object> retryPolicy;
    @Inject
    private TypeManager typeManager;
    @Inject
    private HealthCheckService healthCheckService;
    @Inject
    private Clock clock;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        typeManager.registerTypes(DataFlowRequestDocument.class);
    }

    @Provider
    public DataPlaneStore dataPlaneStore(ServiceExtensionContext context) {

        var configuration = new CosmosDataPlaneStoreConfig(context);
        var client = clientProvider.createClient(vault, configuration);
        var cosmosDbApi = new CosmosDbApiImpl(configuration, client);
        var store = new CosmosDataPlaneStore(cosmosDbApi, typeManager.getMapper(), retryPolicy, configuration.getPartitionKey(), clock);
        healthCheckService.addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

        return store;
    }

}
