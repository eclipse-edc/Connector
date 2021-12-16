package org.eclipse.dataspaceconnector.catalog.cache.query;

import org.eclipse.dataspaceconnector.catalog.spi.NodeQueryAdapter;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.CatalogRequest;

import java.util.concurrent.CompletableFuture;

import static org.eclipse.dataspaceconnector.common.types.Cast.cast;

public class IdsMultipartNodeQueryAdapter implements NodeQueryAdapter {
    public static final String IDS_MULTIPART_PROTOCOL = "ids-multipart";
    private final String connectorId;
    private final RemoteMessageDispatcherRegistry dispatcherRegistry;
    private final TypeManager typeManager;

    public IdsMultipartNodeQueryAdapter(String connectorId, RemoteMessageDispatcherRegistry dispatcherRegistry, TypeManager typeManager) {
        this.connectorId = connectorId;
        this.dispatcherRegistry = dispatcherRegistry;
        this.typeManager = typeManager;
    }

    @Override
    public CompletableFuture<UpdateResponse> sendRequest(UpdateRequest updateRequest) {
        CatalogRequest catalogRequest = CatalogRequest.Builder.newInstance()
                .protocol(IDS_MULTIPART_PROTOCOL)
                .connectorAddress(getNodeUrl(updateRequest))
                .connectorId(connectorId)
                .build();

        CompletableFuture<Catalog> future = cast(dispatcherRegistry.send(Object.class, catalogRequest, () -> null));

        return future.thenApply(catalog -> {
            // This is a dirty hack which is necessary because "assetNames" will get deserialized to
            // a list of LinkedHashMaps, instead of Assets.
            // so we need to serialize and manually deserialize again...
//            var assetNames = Collections.emptyList();
//            var assets = assetNames.stream()
//                    .map(asset -> typeManager.readValue(typeManager.writeValueAsString(asset), Asset.class))
//                    .collect(Collectors.toList());

            return new UpdateResponse(getNodeUrl(updateRequest), catalog);
        });
    }

    // adds /api/ids/multipart if not already there
    private String getNodeUrl(UpdateRequest updateRequest) {
        var url = updateRequest.getNodeUrl();
        if (!url.endsWith("/api/ids/multipart")) {
            url += "/api/ids/multipart";
        }

        return url;
    }
}
