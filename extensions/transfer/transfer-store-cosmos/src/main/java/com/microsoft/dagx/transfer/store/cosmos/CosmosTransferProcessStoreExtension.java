/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.store.cosmos;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.microsoft.dagx.common.string.StringUtils;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.DagxSetting;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;
import com.microsoft.dagx.spi.transfer.store.TransferProcessStore;
import com.microsoft.dagx.transfer.store.cosmos.model.TransferProcessDocument;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * Provides an in-memory implementation of the {@link com.microsoft.dagx.spi.transfer.store.TransferProcessStore} for testing.
 */
public class CosmosTransferProcessStoreExtension implements ServiceExtension {
    /**
     * The setting for the CosmosDB account name
     */
    @DagxSetting
    private final static String COSMOS_ACCOUNTNAME_SETTING = "dagx.cosmos.account.name";
    /**
     * The setting for the name of the database where TransferProcesses will be stored
     */
    @DagxSetting
    private final static String COSMOS_DBNAME_SETTING = "dagx.cosmos.database.name";
    @DagxSetting
    private final static String COSMOS_PARTITION_KEY_SETTING = "dagx.cosmos.partitionkey";
    private final static String DEFAULT_PARTITION_KEY = "dagx";
    private final static String CONTAINER_NAME = "dagx-transferprocess";

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {

        monitor = context.getMonitor();
        monitor.info("Initializing Cosmos Transfer Process Store extension...");

        // configure cosmos db
        var cosmosAccountName = context.getSetting(COSMOS_ACCOUNTNAME_SETTING, null);
        if (StringUtils.isNullOrEmpty(cosmosAccountName)) {
            throw new DagxException("'" + COSMOS_ACCOUNTNAME_SETTING + "' cannot be null or empty!");
        }
        var cosmosDbName = context.getSetting(COSMOS_DBNAME_SETTING, null);
        if (StringUtils.isNullOrEmpty(cosmosDbName)) {
            throw new DagxException("'" + COSMOS_DBNAME_SETTING + "' cannot be null or empty!");
        }

        // get cosmos db access key
        var vault = context.getService(Vault.class);
        var accountKey = vault.resolveSecret(cosmosAccountName);
        if (StringUtils.isNullOrEmpty(accountKey)) {
            throw new DagxException("No credentials found in vault for Cosmos DB '" + cosmosAccountName + "'");
        }

        // create cosmos db api client
        final String host = "https://" + cosmosAccountName + ".documents.azure.com:443/";

        ArrayList<String> preferredRegions = new ArrayList<>();
        preferredRegions.add("West US");
        var client = new CosmosClientBuilder()
                .endpoint(host)
                .key(accountKey)
                .preferredRegions(preferredRegions)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .buildClient();


        var database = getDatabase(client, cosmosDbName);
        if (database.readAllContainers().stream().noneMatch(sp -> sp.getId().equals(CONTAINER_NAME))) {
            throw new DagxException("A CosmosDB container named '" + CONTAINER_NAME + "' was not found in account '" + cosmosAccountName + "'. Please create one, preferably using terraform.");
        }

        var container = database.getContainer(CONTAINER_NAME);
        var partitionKey = context.getSetting(COSMOS_PARTITION_KEY_SETTING, DEFAULT_PARTITION_KEY);

        // get unique connector name
        var connectorId = context.getSetting("dagx.ids.connector.name", "dagx-connector-" + UUID.randomUUID());

        monitor.info("CosmosTransferProcessStore will use connector id '" + connectorId + "'");
        context.registerService(TransferProcessStore.class, new CosmosTransferProcessStore(container, context.getTypeManager(), partitionKey, connectorId));

        context.getTypeManager().registerTypes(TransferProcessDocument.class);
        monitor.info("Initialized CosmosDB Transfer Process Store extension");

    }

    @Override
    public Set<String> requires() {
        return Set.of("dagx:blobstoreapi");
    }

    @Override
    public void start() {
        monitor.info("Started Initialized Cosmos Transfer Process Store extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown Initialized Cosmos Transfer Process Store extension");
    }

    private CosmosDatabase getDatabase(CosmosClient client, String databaseName) {
        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(databaseName);
        return client.getDatabase(databaseResponse.getProperties().getId());
    }


}

