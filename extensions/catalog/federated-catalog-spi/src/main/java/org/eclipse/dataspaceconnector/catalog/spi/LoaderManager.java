package org.eclipse.dataspaceconnector.catalog.spi;


import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.concurrent.BlockingQueue;

/**
 * Manages a list of {@link Loader}s.
 * If for example a Queue is used to receive {@link org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse} objects,
 * the LoaderManager's job is to coordinate all its {@link Loader}s and forward that batch to them.
 */
public interface LoaderManager {

    String FEATURE = "edc:catalog:cache:loadermanager";

    /**
     * Begins observing
     */
    void start(BlockingQueue<UpdateResponse> queue);

    void stop();

    void addLoader(Loader loader);
}
