package org.eclipse.dataspaceconnector.cosmos.azure;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CosmosDbApi {

    void createItem(CosmosDocument<?> item);

    @Nullable Object queryItemById(String id);

    @Nullable Object queryItemById(String id, String partitionKey);

    List<Object> queryAllItems(String partitionKey);

    List<Object> queryAllItems();

    List<Object> queryItems(String query);
}
