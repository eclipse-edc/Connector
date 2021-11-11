package org.eclipse.dataspaceconnector.cosmos.azure;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CosmosDbApi {

    void createItem(Object item);

    @Nullable Object queryItemById(String id);

    List<Object> queryAllItems();

    List<Object> queryItems(String query);
}
