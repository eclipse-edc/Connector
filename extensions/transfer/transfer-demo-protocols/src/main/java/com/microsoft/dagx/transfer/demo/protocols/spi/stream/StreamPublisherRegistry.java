package com.microsoft.dagx.transfer.demo.protocols.spi.stream;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;

/**
 * Registers {@link StreamPublisher} to receive streaming transfer requests.
 */
public interface StreamPublisherRegistry {

    /**
     * Registers the publisher.
     */
    void register(StreamPublisher publisher);

    /**
     * Notifies a {@link StreamPublisher} that can handle the request it can begin publishing data to the requested endpoint.
     */
    void notifyPublisher(DataRequest data);
}
