package org.eclipse.dataspaceconnector.spi.transfer;

public interface WaitStrategy {

    /**
     * Returns the number of milliseconds to pause for the current iteration.
     */
    long waitForMillis();


    /**
     * Marks the iteration as successful.
     */
    default void success() {
    }

    /**
     * Returns the number of milliseconds to wait before retrying.
     */
    default long retryInMillis() {
        return waitForMillis();
    }
}
