package org.eclipse.dataspaceconnector.core.health;

import org.eclipse.dataspaceconnector.spi.system.health.HealthCheckResult;
import org.eclipse.dataspaceconnector.spi.system.health.LivenessProvider;
import org.eclipse.dataspaceconnector.spi.system.health.ReadinessProvider;
import org.eclipse.dataspaceconnector.spi.system.health.StartupStatusProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class HealthCheckServiceImplTest {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(50);
    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(5);
    private HealthCheckServiceImpl service;

    @BeforeEach
    void setup() {
        var duration = Duration.ofMillis(500);
        var config = HealthCheckServiceConfiguration.Builder.newInstance()
                .livenessPeriod(duration)
                .readinessPeriod(duration)
                .startupStatusPeriod(duration)
                .build();
        service = new HealthCheckServiceImpl(config);
    }

    @Test
    void isLive() {
        LivenessProvider lpm = mock(LivenessProvider.class);
        when(lpm.get()).thenReturn(successResult());
        service.addLivenessProvider(lpm);

        await().pollInterval(POLL_INTERVAL)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(service.isLive().isHealthy()).isTrue();
                    verify(lpm, atLeastOnce()).get();
                    verifyNoMoreInteractions(lpm);
                });
    }

    @Test
    void isLive_throwsException() {
        LivenessProvider lpm = mock(LivenessProvider.class);
        when(lpm.get()).thenReturn(successResult()).thenThrow(new RuntimeException("test exception"));
        service.addLivenessProvider(lpm);

        await().pollInterval(POLL_INTERVAL)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(service.isLive().isHealthy()).isFalse();
                    verify(lpm, atLeastOnce()).get();
                    verifyNoMoreInteractions(lpm);
                });
    }

    @Test
    void isLive_failed() {
        LivenessProvider lpm = mock(LivenessProvider.class);
        when(lpm.get()).thenReturn(failedResult());
        service.addLivenessProvider(lpm);


        await().pollInterval(POLL_INTERVAL)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(service.isLive().isHealthy()).isFalse();
                    verify(lpm, atLeastOnce()).get();
                    verifyNoMoreInteractions(lpm);
                });
    }

    @Test
    void isReady() {
        ReadinessProvider provider = mock(ReadinessProvider.class);
        when(provider.get()).thenReturn(successResult());
        service.addReadinessProvider(provider);

        await().pollInterval(POLL_INTERVAL)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(service.isReady().isHealthy()).isTrue();

                    verify(provider, atLeastOnce()).get();
                    verifyNoMoreInteractions(provider);
                });
    }

    @Test
    void isReady_throwsException() {
        ReadinessProvider provider = mock(ReadinessProvider.class);
        when(provider.get()).thenReturn(successResult()).thenThrow(new RuntimeException("test-exception"));
        service.addReadinessProvider(provider);

        await().pollInterval(POLL_INTERVAL)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(service.isReady().isHealthy()).isFalse();

                    verify(provider, atLeastOnce()).get();
                    verifyNoMoreInteractions(provider);
                });
    }

    @Test
    void isReady_failed() {
        ReadinessProvider provider = mock(ReadinessProvider.class);
        when(provider.get()).thenReturn(failedResult());
        service.addReadinessProvider(provider);

        await().pollInterval(POLL_INTERVAL)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(service.isReady().isHealthy()).isFalse();

                    verify(provider, atLeastOnce()).get();
                    verifyNoMoreInteractions(provider);
                });
    }

    @Test
    void hasStartupFinished() {
        StartupStatusProvider provider = mock(StartupStatusProvider.class);
        when(provider.get()).thenReturn(successResult());
        service.addStartupStatusProvider(provider);

        await().pollInterval(POLL_INTERVAL)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(service.getStartupStatus().isHealthy()).isTrue();

                    verify(provider, atLeastOnce()).get();
                    verifyNoMoreInteractions(provider);
                });

    }

    @Test
    void hasStartupFinished_throwsException() {
        StartupStatusProvider provider = mock(StartupStatusProvider.class);
        when(provider.get()).thenReturn(successResult()).thenThrow(new RuntimeException("test-exception"));
        service.addStartupStatusProvider(provider);

        await().pollInterval(POLL_INTERVAL)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(service.getStartupStatus().isHealthy()).isFalse();

                    verify(provider, atLeastOnce()).get();
                    verifyNoMoreInteractions(provider);
                });

    }

    @Test
    void hasStartupFinished_failed() {
        StartupStatusProvider provider = mock(StartupStatusProvider.class);
        when(provider.get()).thenReturn(failedResult());
        service.addStartupStatusProvider(provider);

        await().pollInterval(POLL_INTERVAL)
                .atMost(AWAIT_TIMEOUT)
                .untilAsserted(() -> {
                    assertThat(service.getStartupStatus().isHealthy()).isFalse();

                    verify(provider, atLeastOnce()).get();
                    verifyNoMoreInteractions(provider);
                });
    }

    private HealthCheckResult failedResult() {
        return HealthCheckResult.failed("test-error");
    }

    private HealthCheckResult successResult() {
        return HealthCheckResult.success();
    }
}