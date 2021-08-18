package org.eclipse.dataspaceconnector.metadata.catalog;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface CatalogService {
    CompletableFuture<List<String>> listArtifacts(String connectorName, String connectorAddress);
}
