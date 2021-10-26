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

package org.eclipse.dataspaceconnector.assetindex.azure;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.assetindex.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.cosmos.azure.AbstractCosmosConfig;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Provides a persistent implementation of the {@link org.eclipse.dataspaceconnector.spi.asset.AssetIndex} using CosmosDB.
 */
public class CosmosAssetIndexExtension implements ServiceExtension {

    private static final String NAME = "CosmosDB Asset Index";

    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of(AssetIndex.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        monitor.info(String.format("Initializing %s extension...", NAME));

        var configuration = new AssetIndexCosmosConfig(context);
        Vault service = context.getService(Vault.class);

        // create client
        var cosmosClient = createClient(service, configuration.getAccountName(), Collections.singletonList(configuration.getPreferredRegion()));

        // get database, throw exception if not exists
        var database = createDatabase(configuration, cosmosClient);
        var container = getContainer(database, configuration);

        context.registerService(AssetIndex.class, new CosmosAssetIndex(
                container, configuration.getPartitionKey(), context.getTypeManager(), context.getService(RetryPolicy.class), configuration.isQueryMetricsEnabled()));

        context.getTypeManager().registerTypes(AssetDocument.class);
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

    private static CosmosDatabase getDatabase(CosmosClient client, String databaseName) {
        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(databaseName);
        return client.getDatabase(databaseResponse.getProperties().getId());
    }

    private static CosmosContainer getContainer(CosmosDatabase database, AssetIndexCosmosConfig config) {
        String containerName = config.getContainerName();
        if (database.readAllContainers().stream().noneMatch(sp -> sp.getId().equals(containerName))) {
            throw new EdcException(
                    "A CosmosDB container named '" + containerName + "' was not found in account '" + config.getAccountName() + "'. Please create one, preferably using terraform.");
        }
        return database.getContainer(containerName);
    }
}

