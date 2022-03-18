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

package org.eclipse.dataspaceconnector.catalog.node.directory.azure;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.catalog.node.directory.azure.model.FederatedCacheNodeDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;

/**
 * Provides a persistent implementation of the {@link FederatedCacheNodeDirectory} using CosmosDB.
 */
@Provides(FederatedCacheNodeDirectory.class)
public class CosmosFederatedCacheNodeDirectoryExtension implements ServiceExtension {

    @Override
    public String name() {
        return "CosmosDB Federated Cache Node Directory";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var configuration = new FederatedCacheNodeDirectoryCosmosConfig(context);
        Vault vault = context.getService(Vault.class);

        var cosmosDbApi = new CosmosDbApiImpl(vault, configuration);
        FederatedCacheNodeDirectory directory = new CosmosFederatedCacheNodeDirectory(cosmosDbApi, configuration.getPartitionKey(), context.getTypeManager(), context.getService(RetryPolicy.class));
        context.registerService(FederatedCacheNodeDirectory.class, directory);

        context.getTypeManager().registerTypes(FederatedCacheNodeDocument.class);

        context.getService(HealthCheckService.class).addReadinessProvider(() -> cosmosDbApi.get().forComponent(name()));

    }

}

