/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.util;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * A utility class that can be used to automatically retry a certain action after it fails.
 * Also provides an option to exclude certain exceptions from the retry loop.
 * Currently, a binary exponential backoff is implemented.
 */
public class RetryPolicy {
    private final int maxRetries;
    private long timeoutMilliseconds;
    private Class<? extends Exception>[] excludedExceptions;

    private RetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public static RetryPolicyBuilder create() {
        return new RetryPolicyBuilder();
    }

    private <R> R execute(RetryableAction<R> action) {
        int count = 0;
        Exception lastException = null;
        while (count < maxRetries) {
            try {
                return action.perform();
            } catch (Exception e) {
                if (isBlacklisted(e)) {
                    throw e;
                }
                count++;
                lastException = e;
                try {
                    System.out.println("attempt " + count + ": waiting " + timeoutMilliseconds + " milliseconds");
                    Thread.sleep(timeoutMilliseconds);
                } catch (InterruptedException ignored) {
                }
                //todo: delegate timeout to backoff strategy
                timeoutMilliseconds = (long) (Math.pow(2, count) * 500);

            }
        }
        assert lastException != null;
        throw (RuntimeException) lastException;
    }

    private boolean isBlacklisted(Exception e) {
        if (excludedExceptions == null || excludedExceptions.length == 0) {
            return false;
        }
        return Arrays.stream(excludedExceptions).anyMatch(excludedException -> excludedException.isAssignableFrom(e.getClass()));
    }

    @FunctionalInterface
    public interface RetryableAction<R> {
        R perform();
    }

    public static final class RetryPolicyBuilder {
        private long timeoutMillis;
        private int maxRetries;
        private Class<? extends Exception>[] excludedExceptions;

        private RetryPolicyBuilder() {
            maxRetries = 3;
            timeoutMillis = 2;
        }

        public RetryPolicyBuilder withTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public RetryPolicyBuilder withTimeout(int timeout, TimeUnit timeunit) {
            timeoutMillis = timeunit.toMillis(timeout);
            return this;
        }

        public RetryPolicyBuilder withMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public <R> R execute(RetryableAction<R> action) {
            RetryPolicy retryPolicy = new RetryPolicy(maxRetries);
            retryPolicy.timeoutMilliseconds = timeoutMillis;
            retryPolicy.excludedExceptions = excludedExceptions;
            return retryPolicy.execute(action);
        }

        @SafeVarargs
        public final RetryPolicyBuilder excluding(Class<? extends Exception>... exceptions) {
            excludedExceptions = exceptions;
            return this;
        }
    }
}
