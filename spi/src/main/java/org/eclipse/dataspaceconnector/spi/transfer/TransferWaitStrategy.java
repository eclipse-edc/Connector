/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.spi.transfer;

/**
 * Implements a wait strategy for the {@link TransferProcessManager}.
 *
 * Implementations may choose to to enforce an incremental backoff period when successive errors are encountered.
 */
@FunctionalInterface
public interface TransferWaitStrategy {

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
