package org.eclipse.dataspaceconnector.catalog.spi;


import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.concurrent.CompletableFuture;

public interface ProtocolAdapter {
    CompletableFuture<UpdateResponse> sendRequest(UpdateRequest request);
}
