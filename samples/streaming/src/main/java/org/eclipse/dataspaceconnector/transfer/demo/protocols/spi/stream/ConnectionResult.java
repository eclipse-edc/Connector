package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.stream;

import java.util.function.Consumer;

/**
 * Result of a connect operation.
 */
public class ConnectionResult {
    private boolean success;
    private String error;
    private Consumer<byte[]> consumer;

    /**
     * Returns true if the connection was successful.
     */
    public boolean success() {
        return success;
    }

    /**
     * Returns an error if the connection was unsuccessful; otherwise null.
     */
    public String getError() {
        return error;
    }

    /**
     * Returns a consumer that can be used to publish data to the topic.
     */
    public Consumer<byte[]> getConsumer() {
        return consumer;
    }

    public ConnectionResult(Consumer<byte[]> consumer) {
        this.consumer = consumer;
        success = true;
    }

    public ConnectionResult(String error) {
        success = false;
        this.error = error;
    }


}
