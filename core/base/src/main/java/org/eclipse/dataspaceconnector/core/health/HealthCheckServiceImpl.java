package org.eclipse.dataspaceconnector.core.health;

import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService;
import org.eclipse.dataspaceconnector.spi.system.health.HealthStatus;
import org.eclipse.dataspaceconnector.spi.system.health.LivenessProvider;
import org.eclipse.dataspaceconnector.spi.system.health.ReadinessProvider;
import org.eclipse.dataspaceconnector.spi.system.health.StartupStatusProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthCheckServiceImpl implements HealthCheckService {
    private final List<LivenessProvider> livenessProviders;
    private final List<ReadinessProvider> readinessProviders;
    private final List<StartupStatusProvider> startupStatusProviders;

    private final Map<LivenessProvider, HealthCheckResult> cachedLivenessResults;
    private final Map<ReadinessProvider, HealthCheckResult> cachedReadinessResults;
    private final Map<StartupStatusProvider, HealthCheckResult> cachedStartupStatus;

    private final ScheduledExecutorService executor;

    public HealthCheckServiceImpl(HealthCheckServiceConfiguration configuration) {
        readinessProviders = new CopyOnWriteArrayList<>();
        livenessProviders = new CopyOnWriteArrayList<>();
        startupStatusProviders = new CopyOnWriteArrayList<>();

        cachedLivenessResults = new ConcurrentHashMap<>();
        cachedReadinessResults = new ConcurrentHashMap<>();
        cachedStartupStatus = new ConcurrentHashMap<>();

        executor = Executors.newScheduledThreadPool(configuration.getThreadPoolSize());
        start(configuration);
    }

    @Override
    public void addLivenessProvider(LivenessProvider provider) {
        livenessProviders.add(provider);
    }

    @Override
    public void addReadinessProvider(ReadinessProvider provider) {
        readinessProviders.add(provider);
    }

    @Override
    public void addStartupStatusProvider(StartupStatusProvider provider) {
        startupStatusProviders.add(provider);
    }

    @Override
    public HealthStatus isLive() {
        return new HealthStatus(cachedLivenessResults.values());
    }

    @Override
    public HealthStatus isReady() {
        return new HealthStatus(cachedReadinessResults.values());
    }

    @Override
    public HealthStatus getStartupStatus() {
        return new HealthStatus(cachedStartupStatus.values());
    }

    public void stop() {
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
    }


    private void start(HealthCheckServiceConfiguration configuration) {
        //todo: maybe providers should provide their desired timeout instead of a global config?
        executor.scheduleAtFixedRate(this::queryReadiness, 0, configuration.getReadinessPeriod().toMillis(), TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::queryLiveness, 0, configuration.getLivenessPeriod().toMillis(), TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::queryStartupStatus, 0, configuration.getStartupStatusPeriod().toMillis(), TimeUnit.MILLISECONDS);
    }

    private void queryReadiness() {
        readinessProviders.parallelStream().forEach(rp -> {
            try {
                cachedReadinessResults.put(rp, rp.get());
            } catch (Exception ex) {
                cachedReadinessResults.put(rp, HealthCheckResult.failed(ex.getMessage()));
            }
        });
    }

    private void queryLiveness() {
        livenessProviders.parallelStream().forEach(rp -> {
            try {
                cachedLivenessResults.put(rp, rp.get());
            } catch (Exception ex) {
                cachedLivenessResults.put(rp, HealthCheckResult.failed(ex.getMessage()));
            }
        });
    }

    private void queryStartupStatus() {
        startupStatusProviders.parallelStream().forEach(rp -> {
            try {
                cachedStartupStatus.put(rp, rp.get());
            } catch (Exception ex) {
                cachedStartupStatus.put(rp, HealthCheckResult.failed(ex.getMessage()));
            }
        });
    }

}
