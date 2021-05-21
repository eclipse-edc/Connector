package com.microsoft.dagx.transfer.demo.protocols.spi.stream;

/**
 * Observes a destination, receiving callbacks when data is published to it.
 */
@FunctionalInterface
public interface StreamObserver {
    /**
     * Callback when data is published to a destination.
     */
    void onPublish(String destinationName, byte[] payload);
}
