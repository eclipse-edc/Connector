package org.eclipse.dataspaceconnector.system.health;

import java.time.Duration;

public class HealthCheckServiceConfiguration {
    public static final HealthCheckServiceConfiguration DEFAULT = HealthCheckServiceConfiguration.Builder.newInstance().build();
    private static final long DEFAULT_PERIOD_SECONDS = 10;
    private int threadPoolSize = 3;
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
