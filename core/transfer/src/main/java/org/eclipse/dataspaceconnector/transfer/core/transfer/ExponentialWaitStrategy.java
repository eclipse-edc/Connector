/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;

/**
 * Implements an exponential backoff strategy for failed iterations.
 */
public class ExponentialWaitStrategy implements TransferWaitStrategy {
    private final long successWaitPeriodMillis;
    private int errorCount = 0;


    public ExponentialWaitStrategy(long successWaitPeriodMillis) {
        this.successWaitPeriodMillis = successWaitPeriodMillis;
    }

    @Override
    public long waitForMillis() {
        return successWaitPeriodMillis;
    }

    @Override
    public void success() {
        errorCount = 0;
    }

    @Override
    public long retryInMillis() {
        errorCount++;
        double exponentialMultiplier = Math.pow(2.0, errorCount - 1);
        double result = exponentialMultiplier * successWaitPeriodMillis;
        return (long) Math.min(result, Long.MAX_VALUE);
    }
}
