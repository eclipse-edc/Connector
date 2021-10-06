package org.eclipse.dataspaceconnector.catalog.cache.spi;

import java.util.concurrent.CompletableFuture;

public interface ProtocolAdapter {
    CompletableFuture<UpdateResponse> sendRequest(UpdateRequest request);
}
