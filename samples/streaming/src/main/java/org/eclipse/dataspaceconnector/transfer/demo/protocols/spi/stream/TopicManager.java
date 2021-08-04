package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream;

import org.eclipse.dataspaceconnector.transfer.demo.protocols.common.DataDestination;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Manages pub/sub topics.
 */
public interface TopicManager {

    /**
     * Asynchronously provisions a topic.
     */
    CompletableFuture<DataDestination> provision(String topicName);

    /**
     * Creates a connection to a topic. Connections are used to publish messages.
     */
    ConnectionResult connect(String topicName, String accessToken);

    /**
     * Subscribes to  topic.
     */
    SubscriptionResult subscribe(String topicName, String accessToken, Consumer<byte[]> consumer);

    /**
     * Un-subscribes from a topic.
     */
    void unsubscribe(Subscription subscription);

    /**
     * Registers an observer that receives callbacks when data is published to a topic.
     */
    String registerObserver(StreamObserver observer);

    /**
     * Un-registers an observer/
     */
    void unRegisterObserver(String id);

}
