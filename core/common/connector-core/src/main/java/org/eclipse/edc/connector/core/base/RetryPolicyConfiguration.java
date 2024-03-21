/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core.base;

public class RetryPolicyConfiguration {

    private int maxRetries;
    private int minBackoff = 1;
    private int maxBackoff = Integer.MAX_VALUE;
    private boolean logOnRetry;
    private boolean logOnRetryScheduled;
    private boolean logOnRetriesExceeded;
    private boolean logOnFailedAttempt;
    private boolean logOnAbort;

    private RetryPolicyConfiguration() {
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getMinBackoff() {
        return minBackoff;
    }

    public int getMaxBackoff() {
        return maxBackoff;
    }

    public boolean isLogOnRetry() {
        return logOnRetry;
    }

    public boolean isLogOnRetryScheduled() {
        return logOnRetryScheduled;
    }

    public boolean isLogOnRetriesExceeded() {
        return logOnRetriesExceeded;
    }

    public boolean isLogOnFailedAttempt() {
        return logOnFailedAttempt;
    }

    public boolean isLogOnAbort() {
        return logOnAbort;
    }

    public static class Builder {

        private final RetryPolicyConfiguration instance = new RetryPolicyConfiguration();

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        public Builder maxRetries(int maxRetries) {
            instance.maxRetries = maxRetries;
            return this;
        }

        public Builder minBackoff(int minBackoff) {
            instance.minBackoff = minBackoff;
            return this;
        }

        public Builder maxBackoff(int maxBackoff) {
            instance.maxBackoff = maxBackoff;
            return this;
        }

        public Builder logOnRetry(boolean logOnRetry) {
            instance.logOnRetry = logOnRetry;
            return this;
        }

        public Builder logOnRetryScheduled(boolean logOnRetryScheduled) {
            instance.logOnRetryScheduled = logOnRetryScheduled;
            return this;
        }

        public Builder logOnRetriesExceeded(boolean logOnRetriesExceeded) {
            instance.logOnRetriesExceeded = logOnRetriesExceeded;
            return this;
        }

        public Builder logOnFailedAttempt(boolean logOnFailedAttempt) {
            instance.logOnFailedAttempt = logOnFailedAttempt;
            return this;
        }

        public Builder logOnAbort(boolean logOnAbort) {
            instance.logOnAbort = logOnAbort;
            return this;
        }

        public RetryPolicyConfiguration build() {
            return instance;
        }
    }
}
