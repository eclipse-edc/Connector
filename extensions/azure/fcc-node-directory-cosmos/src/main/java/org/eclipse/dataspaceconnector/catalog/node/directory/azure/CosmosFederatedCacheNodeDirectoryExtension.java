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

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.catalog.node.directory.azure.model.FederatedCacheNodeDocument;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.cosmos.azure.AbstractCosmosConfig;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Collections;
import java.util.List;
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
    public Set<String> requires() {
        return Set.of();
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        monitor.info(String.format("Initializing %s extension...", NAME));

        var configuration = new FederatedCacheNodeDirectoryCosmosConfig(context);
        Vault service = context.getService(Vault.class);

        // create client
        var cosmosClient = createClient(service, configuration.getAccountName(), Collections.singletonList(configuration.getPreferredRegion()));

        // get database, throw exception if not exists
        var database = createDatabase(configuration, cosmosClient);
        String containerName = configuration.getContainerName();
        if (database.readAllContainers().stream().noneMatch(sp -> sp.getId().equals(containerName))) {
            //todo: maybe create database if not exist?
            throw new EdcException(
                    "A CosmosDB container named '" + containerName + "' was not found in account '" + configuration.getAccountName() + "'. Please create one, preferably using terraform.");
        }
        CosmosContainer container = database.getContainer(containerName);
        context.registerService(FederatedCacheNodeDirectory.class, new CosmosFederatedCacheNodeDirectory(container, configuration.getPartitionKey(), context.getTypeManager(), context.getService(RetryPolicy.class)));

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

    private CosmosDatabase createDatabase(AbstractCosmosConfig configuration, CosmosClient client) {

        return getDatabase(client, configuration.getDbName());
    }

    private CosmosClient createClient(Vault vault, String accountName, List<String> preferredRegions) {
        var accountKey = vault.resolveSecret(accountName);
        if (StringUtils.isNullOrEmpty(accountKey)) {
            throw new EdcException("No credentials found in vault for Cosmos DB '" + accountName + "'");
        }

        // create cosmos db api client
        String host = "https://" + accountName + ".documents.azure.com:443/";

        return new CosmosClientBuilder()
                .endpoint(host)
                .key(accountKey)
                .preferredRegions(preferredRegions)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .buildClient();
    }

    private CosmosDatabase getDatabase(CosmosClient client, String databaseName) {
        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(databaseName);
        return client.getDatabase(databaseResponse.getProperties().getId());
    }


}

