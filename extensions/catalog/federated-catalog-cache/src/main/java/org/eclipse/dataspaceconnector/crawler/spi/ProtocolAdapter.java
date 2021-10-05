package org.eclipse.dataspaceconnector.crawler.spi;

import java.util.concurrent.CompletableFuture;

public interface ProtocolAdapter {
    CompletableFuture<UpdateResponse> sendRequest(UpdateRequest request);
}
