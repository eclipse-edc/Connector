package org.eclipse.dataspaceconnector.core.health;

import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
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
import java.util.function.Supplier;

public class HealthCheckServiceImpl implements HealthCheckService {
    private final List<LivenessProvider> livenessProviders;
    private final List<ReadinessProvider> readinessProviders;
    private final List<StartupStatusProvider> startupStatusProviders;

    private final Map<LivenessProvider, HealthCheckResult> cachedLivenessResults;
    private final Map<ReadinessProvider, HealthCheckResult> cachedReadinessResults;
    private final Map<StartupStatusProvider, HealthCheckResult> cachedStartupStatus;

    private final ScheduledExecutorService executor;
    private final HealthCheckServiceConfiguration configuration;

    public HealthCheckServiceImpl(HealthCheckServiceConfiguration configuration,
                                  ExecutorInstrumentation executorInstrumentation) {
        this.configuration = configuration;
        readinessProviders = new CopyOnWriteArrayList<>();
        livenessProviders = new CopyOnWriteArrayList<>();
        startupStatusProviders = new CopyOnWriteArrayList<>();

        cachedLivenessResults = new ConcurrentHashMap<>();
        cachedReadinessResults = new ConcurrentHashMap<>();
        cachedStartupStatus = new ConcurrentHashMap<>();

        executor = executorInstrumentation.instrument(
                Executors.newScheduledThreadPool(configuration.getThreadPoolSize()),
                HealthCheckService.class.getSimpleName());
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

    @Override
    public void refresh() {
        executor.execute(this::queryReadiness);
        executor.execute(this::queryLiveness);
        executor.execute(this::queryStartupStatus);
    }

    public void stop() {
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    public void start() {
        //todo: maybe providers should provide their desired timeout instead of a global config?
        executor.scheduleAtFixedRate(this::queryReadiness, 0, configuration.getReadinessPeriod().toMillis(), TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::queryLiveness, 0, configuration.getLivenessPeriod().toMillis(), TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::queryStartupStatus, 0, configuration.getStartupStatusPeriod().toMillis(), TimeUnit.MILLISECONDS);
    }

    private void queryReadiness() {
        readinessProviders.parallelStream().forEach(provider -> updateCache(provider, cachedReadinessResults));
    }

    private void queryLiveness() {
        livenessProviders.parallelStream().forEach(provider -> updateCache(provider, cachedLivenessResults));
    }

    private void queryStartupStatus() {
        startupStatusProviders.parallelStream().forEach(provider -> updateCache(provider, cachedStartupStatus));
    }

    private <T extends Supplier<HealthCheckResult>> void updateCache(T provider, Map<T, HealthCheckResult> cache) {
        try {
            cache.put(provider, provider.get());
        } catch (Exception ex) {
            cache.put(provider, HealthCheckResult.failed(ex.getMessage()));
        }
    }

}
