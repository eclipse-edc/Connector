package org.eclipse.dataspaceconnector.cosmos.azure;

import com.azure.cosmos.models.SqlQuerySpec;
import org.eclipse.dataspaceconnector.spi.system.health.ReadinessProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface CosmosDbApi extends ReadinessProvider {

    void saveItem(CosmosDocument<?> item);

    void saveItems(Collection<CosmosDocument<?>> definitions);

    void deleteItem(String id);

    @Nullable Object queryItemById(String id);

    @Nullable Object queryItemById(String id, String partitionKey);

    List<Object> queryAllItems(String partitionKey);

    List<Object> queryAllItems();

    Stream<Object> queryItems(SqlQuerySpec querySpec);

    Stream<Object> queryItems(String query);

    String invokeStoredProcedure(String procedureName, String partitionKey, Object... args);
}
