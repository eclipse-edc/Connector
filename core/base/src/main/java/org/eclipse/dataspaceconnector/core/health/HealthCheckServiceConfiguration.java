/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.core.health;

import java.time.Duration;

public class HealthCheckServiceConfiguration {
    public static final long DEFAULT_PERIOD_SECONDS = 60;
    public static final int DEFAULT_THREADPOOL_SIZE = 3;
    private int threadPoolSize = DEFAULT_THREADPOOL_SIZE;
    private Duration readinessPeriod = Duration.ofSeconds(DEFAULT_PERIOD_SECONDS);
    private Duration livenessPeriod = Duration.ofSeconds(DEFAULT_PERIOD_SECONDS);
    private Duration startupStatusPeriod = Duration.ofSeconds(DEFAULT_PERIOD_SECONDS);

    /**
     * how many threads should be used by the health check service for periodic polling
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    /**
     * Time delay between before {@link org.eclipse.dataspaceconnector.spi.system.health.ReadinessProvider}s are checked again.
     * Defaults to 10 seconds.
     */
    public Duration getReadinessPeriod() {
        return readinessPeriod;
    }

    /**
     * Time delay between before {@link org.eclipse.dataspaceconnector.spi.system.health.LivenessProvider}s are checked again.
     * Defaults to 10 seconds.
     */
    public Duration getLivenessPeriod() {
        return livenessPeriod;
    }

    /**
     * Time delay between before {@link org.eclipse.dataspaceconnector.spi.system.health.StartupStatusProvider}s are checked again.
     * Defaults to 10 seconds.
     */
    public Duration getStartupStatusPeriod() {
        return startupStatusPeriod;
    }

    public static final class Builder {
        private final HealthCheckServiceConfiguration config;

        private Builder() {
            config = new HealthCheckServiceConfiguration();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder readinessPeriod(Duration readinessPeriod) {
            config.readinessPeriod = readinessPeriod;
            return this;
        }

        public Builder livenessPeriod(Duration livenessPeriod) {
            config.livenessPeriod = livenessPeriod;
            return this;
        }

        public Builder startupStatusPeriod(Duration startupStatusPeriod) {
            config.startupStatusPeriod = startupStatusPeriod;
            return this;
        }

        public Builder threadPoolSize(int threadPoolSize) {
            config.threadPoolSize = threadPoolSize;
            return this;
        }

        public HealthCheckServiceConfiguration build() {
            return config;
        }
    }
}
