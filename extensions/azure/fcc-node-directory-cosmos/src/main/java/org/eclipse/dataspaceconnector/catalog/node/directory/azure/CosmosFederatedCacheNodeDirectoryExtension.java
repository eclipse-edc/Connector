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
import org.eclipse.dataspaceconnector.catalog.node.directory.azure.model.FederatedCacheNodeDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApiImpl;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 * Provides a persistent implementation of the {@link FederatedCacheNodeDirectory} using CosmosDB.
 */
public class CosmosFederatedCacheNodeDirectoryExtension implements ServiceExtension {

    private static final String NAME = "CosmosDB Federated Cache Node Directory";

    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of(FederatedCacheNodeDirectory.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        monitor.info(String.format("Initializing %s extension...", NAME));

        var configuration = new FederatedCacheNodeDirectoryCosmosConfig(context);
        Vault vault = context.getService(Vault.class);

        CosmosDbApi cosmosDbApi = new CosmosDbApiImpl(vault, configuration);
        FederatedCacheNodeDirectory directory = new CosmosFederatedCacheNodeDirectory(cosmosDbApi, configuration.getPartitionKey(), context.getTypeManager(), context.getService(RetryPolicy.class));
        context.registerService(FederatedCacheNodeDirectory.class, directory);

        context.getTypeManager().registerTypes(FederatedCacheNodeDocument.class);
        monitor.info(String.format("Initialized %s extension", NAME));
    }

    @Override
    public void start() {
        monitor.info(String.format("Started %s extension", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdowns %s extension", NAME));
    }
}

