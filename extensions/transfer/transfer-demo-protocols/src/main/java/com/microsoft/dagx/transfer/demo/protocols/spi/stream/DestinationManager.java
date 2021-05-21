package com.microsoft.dagx.transfer.demo.protocols.spi.stream;

import com.microsoft.dagx.transfer.demo.protocols.common.DataDestination;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Manages destinations.
 */
public interface DestinationManager {

    /**
     * Asynchronously provisions a destination.
     */
    CompletableFuture<DataDestination> provision(String destinationName);

    /**
     * Creates a connection to a destination. Connections are used to publish messages.
     */
    ConnectionResult connect(String destinationName, String accessToken);

    /**
     * Subscribes to  destination.
     */
    SubscriptionResult subscribe(String destinationName, String accessToken, Consumer<byte[]> consumer);

    /**
     * Un-subscribes from a destination.
     */
    void unsubscribe(Subscription subscription);

    /**
     * Registers an observer that receives callbacks when data is published to a destination.
     */
    String registerObserver(StreamObserver observer);

    /**
     * Unre-registers an observer/
     */
    void unRegisterObserver(String id);

}
