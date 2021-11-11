package org.eclipse.dataspaceconnector.cosmos.azure;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CosmosDbApiImpl implements CosmosDbApi {

    private static final String HOST_TEMPLATE = "https://%s.documents.azure.com:443/";

    private final PartitionKey partitionKey;
    private final CosmosItemRequestOptions itemRequestOptions;
    private final CosmosQueryRequestOptions queryRequestOptions;
    private final CosmosContainer container;

    public CosmosDbApiImpl(@NotNull CosmosContainer container, @NotNull String partitionKey, boolean isQueryMetricsEnabled) {
        if (partitionKey.isEmpty()) {
            throw new EdcException("Partition key cannot be empty");
        }
        this.partitionKey = new PartitionKey(partitionKey);
        queryRequestOptions = new CosmosQueryRequestOptions();
        queryRequestOptions.setQueryMetricsEnabled(isQueryMetricsEnabled);
        itemRequestOptions = new CosmosItemRequestOptions();
        this.container = container;
    }

    public CosmosDbApiImpl(@NotNull Vault vault, @NotNull AbstractCosmosConfig config) {
        this(getContainer(vault, config), config.getPartitionKey(), config.isQueryMetricsEnabled());
    }

    private static void handleResponse(CosmosItemResponse<?> response) {
        int code = response.getStatusCode();
        if (code < 200 || code >= 300) {
            throw new EdcException("Error during CosmosDB interaction: " + code);
        }
    }

    private static CosmosContainer getContainer(Vault vault, AbstractCosmosConfig config) {
        CosmosClient client = createClient(vault, config.getAccountName(), Collections.singletonList(config.getPreferredRegion()));
        CosmosDatabase database = getDatabase(client, config.getDbName());
        if (database.readAllContainers().stream().noneMatch(sp -> sp.getId().equals(config.getContainerName()))) {
            throw new EdcException("No CosmosDB container named '" + config.getContainerName() + "' was found in account '" + config.getAccountName() + "'. Please create one, preferably using terraform.");
        }
        return database.getContainer(config.getContainerName());
    }

    private static CosmosClient createClient(Vault vault, String accountName, List<String> preferredRegions) {
        var accountKey = vault.resolveSecret(accountName);
        if (StringUtils.isNullOrEmpty(accountKey)) {
            throw new EdcException("No credentials found in vault for Cosmos DB '" + accountName + "'");
        }

        // create cosmos db api client
        String host = String.format(HOST_TEMPLATE, accountName);
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

    @Override
    public void createItem(Object item) {
        try {
            CosmosItemResponse<Object> response = container.createItem(item, partitionKey, itemRequestOptions);
            handleResponse(response);
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public @Nullable Object queryItemById(String id) {
        CosmosItemResponse<Object> response;
        try {
            response = container.readItem(id, partitionKey, itemRequestOptions, Object.class);
        } catch (NotFoundException e) {
            return null;
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
        handleResponse(response);
        return response.getItem();
    }

    @Override
    public List<Object> queryAllItems() {
        try {
            return container.readAllItems(partitionKey, queryRequestOptions, Object.class).stream().collect(Collectors.toList());
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public List<Object> queryItems(String query) {
        try {
            return container.queryItems(query, queryRequestOptions, Object.class).stream().collect(Collectors.toList());
        } catch (CosmosException e) {
            throw new EdcException(e);
        }
    }
}
