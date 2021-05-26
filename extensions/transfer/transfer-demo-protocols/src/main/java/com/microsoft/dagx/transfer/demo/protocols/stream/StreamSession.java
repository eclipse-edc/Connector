package com.microsoft.dagx.transfer.demo.protocols.stream;

/**
 * A stateful connection to a streaming destination.
 */
public interface StreamSession extends AutoCloseable {

    /**
     * Publishes the data.
     */
    void publish(byte[] data);

    /**
     * Closes the session, releasing acquired resources.
     */
    void close();
}
