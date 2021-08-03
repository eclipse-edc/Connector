package org.eclipse.edc.transfer.demo.protocols.spi.stream;

/**
 * Observes a topic, receiving callbacks when data is published to it.
 */
@FunctionalInterface
public interface StreamObserver {
    /**
     * Callback when data is published to a topic.
     */
    void onPublish(String topicName, byte[] payload);
}
