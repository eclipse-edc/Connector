package org.eclipse.dataspaceconnector.spi.system.health;

/**
 * Provides information about the connector's health status.
 * The three different health aspects are based on <a href=https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#types-of-probe>Kubernetes' definition.</a>
 * <p>
 * Implementors should not forward every incoming request directly to the providers, since that does not scale, might
 * cause bottlenecks and can clog the system. Instead, all providers should be queried ("crawled") in a periodic manner and the results
 * should be cached internally.
 */
public interface HealthCheckService {
    String FEATURE = "edc:system:status:readiness-service";

    void addLivenessProvider(LivenessProvider provider);

    void addReadinessProvider(ReadinessProvider provider);

    void addStartupStatusProvider(StartupStatusProvider provider);

    HealthStatus isLive();

    HealthStatus isReady();

    HealthStatus getStartupStatus();
}
