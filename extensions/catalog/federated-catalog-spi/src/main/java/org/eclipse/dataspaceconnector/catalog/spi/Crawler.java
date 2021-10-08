package org.eclipse.dataspaceconnector.catalog.spi;

import java.util.concurrent.CompletableFuture;

public interface Crawler extends Runnable {
    String FEATURE = "edc:catalog:cache:crawler";

    void addAdapter(ProtocolAdapter adapter);

    CompletableFuture<Void> waitForCompletion();

}
