package com.microsoft.dagx.transfer.core.transfer;

import com.microsoft.dagx.spi.transfer.TransferWaitStrategy;

/**
 * Implements an exponential backoff strategy for failed iterations.
 */
public class ExponentialWaitStrategy implements TransferWaitStrategy {
    private int errorCount = 0;
    private long successWaitPeriod;


    public ExponentialWaitStrategy(long successWaitPeriod) {
        this.successWaitPeriod = successWaitPeriod;
    }

    @Override
    public long waitForMillis() {
        return successWaitPeriod;
    }

    @Override
    public void success() {
        errorCount = 0;
    }

    @Override
    public long retryInMillis() {
        errorCount++;
        double exponentialMultiplier = Math.pow(2.0, errorCount - 1);
        double result = exponentialMultiplier * successWaitPeriod;
        return (long) Math.min(result, Long.MAX_VALUE);
    }
}
