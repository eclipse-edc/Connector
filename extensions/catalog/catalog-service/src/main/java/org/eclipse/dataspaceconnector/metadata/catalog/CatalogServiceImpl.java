package org.eclipse.dataspaceconnector.metadata.catalog;


import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.QueryRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.common.types.Cast.cast;


public class CatalogServiceImpl implements CatalogService {
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;

    public CatalogServiceImpl(RemoteMessageDispatcherRegistry dispatcherRegistry) {
        this.dispatcherRegistry = dispatcherRegistry;
    }

    @Override
    public CompletableFuture<List<String>> listArtifacts(String connectorName, String connectorAddress) {
        var queryRequest = QueryRequest.Builder.newInstance()
                .connectorAddress(connectorAddress)
                .connectorId(connectorName)
                .queryLanguage("dagx")
                .query("select *")
                .protocol("ids-rest")
                .build();

        return cast(dispatcherRegistry.send(List.class, queryRequest, () -> null));
    }
}
