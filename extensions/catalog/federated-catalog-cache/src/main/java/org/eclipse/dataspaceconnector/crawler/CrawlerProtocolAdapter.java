package org.eclipse.dataspaceconnector.crawler;

import java.util.concurrent.CompletableFuture;

public interface CrawlerProtocolAdapter {
    CompletableFuture<UpdateResponse> sendRequest(UpdateRequest request);
}
