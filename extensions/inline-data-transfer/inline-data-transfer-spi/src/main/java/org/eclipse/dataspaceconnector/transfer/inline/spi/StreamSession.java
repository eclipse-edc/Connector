package org.eclipse.dataspaceconnector.transfer.inline.spi;

/**
 * A stateful connection to a streaming topic.
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
