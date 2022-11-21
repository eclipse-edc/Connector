/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.store.cosmos;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.cosmos.CosmosClientProvider;
import org.eclipse.edc.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Extensions that expose an implementation of {@link DataPlaneInstanceStore} that uses SQL as backend storage
 */
@Provides(DataPlaneInstanceStore.class)
@Extension(value = CosmosDataPlaneInstanceStoreExtension.NAME)
public class CosmosDataPlaneInstanceStoreExtension implements ServiceExtension {

    public static final String NAME = "CosmosDB Data Plane Instance Store";

    @Inject
    private TypeManager typeManager;

    @Inject
    private RetryPolicy<Object> retryPolicy;

    @Inject
    private CosmosClientProvider clientProvider;

    @Inject
    private HealthCheckService healthCheckService;

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configuration = new CosmosDataPlaneInstanceStoreConfig(context);

        var cosmosDbApi = new CosmosDbApiImpl(configuration, clientProvider.createClient(vault, configuration));

        var store = new CosmosDataPlaneInstanceStore(cosmosDbApi, typeManager, retryPolicy, configuration.getPartitionKey());
        context.registerService(CosmosDataPlaneInstanceStore.class, store);

        context.getTypeManager().registerTypes(DataPlaneInstanceDocument.class);
    }
}
