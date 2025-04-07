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

package org.eclipse.edc.runtime.core.retry;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

@Settings
public class RetryPolicyConfiguration {
    public static final int DEFAULT_RETRY_POLICY_MAX_RETRIES = 5;
    public static final int DEFAULT_RETRY_POLICY_BACKOFF_MIN_MILLIS = 500;
    public static final int DEFAULT_RETRY_POLICY_BACKOFF_MAX_MILLIS = 10000;
    public static final boolean DEFAULT_RETRY_POLICY_LOG_ON_RETRY = false;
    public static final boolean DEFAULT_RETRY_POLICY_LOG_ON_RETRY_SCHEDULED = false;
    public static final boolean DEFAULT_RETRY_POLICY_LOG_ON_RETRIES_EXCEEDED = false;
    public static final boolean DEFAULT_RETRY_POLICY_LOG_ON_FAILED_ATTEMPT = false;
    public static final boolean DEFAULT_RETRY_POLICY_LOG_ON_ABORT = false;

    @Setting(description = "RetryPolicy: Maximum retries before a failure is propagated", defaultValue = DEFAULT_RETRY_POLICY_MAX_RETRIES + "", key = "edc.core.retry.retries.max")
    private int maxRetries;
    @Setting(description = "RetryPolicy: Minimum number of milliseconds for exponential backoff", defaultValue = DEFAULT_RETRY_POLICY_BACKOFF_MIN_MILLIS + "", key = "edc.core.retry.backoff.min")
    private int minBackoff = 1;
    @Setting(description = "RetryPolicy: Maximum number of milliseconds for exponential backoff", defaultValue = DEFAULT_RETRY_POLICY_BACKOFF_MAX_MILLIS + "", key = "edc.core.retry.backoff.max")
    private int maxBackoff = Integer.MAX_VALUE;
    @Setting(description = "RetryPolicy: Log onRetry events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_RETRY + "", key = "edc.core.retry.log.on.retry")
    private boolean logOnRetry;
    @Setting(description = "RetryPolicy: Log onRetryScheduled events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_RETRY_SCHEDULED + "", key = "edc.core.retry.log.on.retry.scheduled")
    private boolean logOnRetryScheduled;
    @Setting(description = "RetryPolicy: Log onRetriesExceeded events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_RETRIES_EXCEEDED + "", key = "edc.core.retry.log.on.retries.exceeded")
    private boolean logOnRetriesExceeded;
    @Setting(description = "RetryPolicy: Log onFailedAttempt events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_FAILED_ATTEMPT + "", key = "edc.core.retry.log.on.failed.attempt")
    private boolean logOnFailedAttempt;
    @Setting(description = "RetryPolicy: Log onAbort events", defaultValue = DEFAULT_RETRY_POLICY_LOG_ON_ABORT + "", key = "edc.core.retry.log.on.abort")
    private boolean logOnAbort;

    public RetryPolicyConfiguration() {
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

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {

        private final RetryPolicyConfiguration instance;

        private Builder(RetryPolicyConfiguration instance) {
            this.instance = instance;
        }

        public static Builder newInstance() {
            return new Builder(new RetryPolicyConfiguration());
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
