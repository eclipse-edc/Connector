package org.eclipse.dataspaceconnector.catalog.spi;


import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateRequest;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Takes an {@link UpdateRequest}, sends it to the intended endpoint using a particular application protocol (e.g. IDS) to get that
 * endpoint's catalog.
 * <p>
 * For example, an {@code IdsProtocolAdapter} would perform an IDS Description Request to whatever URL is contained in the {@code UpdateRequest}
 * and return the response to that.
 */
public interface CatalogQueryAdapter {
    CompletableFuture<UpdateResponse> sendRequest(UpdateRequest request);
}
