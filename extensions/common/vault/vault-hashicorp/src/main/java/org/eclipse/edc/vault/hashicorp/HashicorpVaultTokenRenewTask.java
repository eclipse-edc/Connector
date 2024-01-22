package org.eclipse.edc.vault.hashicorp;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This task implements the Hashicorp Vault token renewal mechanism.
 * To ensure that this task is really cancelled, call the stop method before program shut down.
 */
public class HashicorpVaultTokenRenewTask {

    @NotNull
    private final ScheduledExecutorService scheduledExecutorService;
    @NotNull
    private final HashicorpVaultClient hashicorpVaultClient;
    @NotNull
    private final Monitor monitor;
    private final long renewBuffer;
    private Future<?> tokenRenewTask;

    public HashicorpVaultTokenRenewTask(@NotNull ExecutorInstrumentation executorInstrumentation,
                                        @NotNull HashicorpVaultClient hashicorpVaultClient,
                                        long renewBuffer,
                                        @NotNull Monitor monitor) {
        this.scheduledExecutorService = executorInstrumentation.instrument(Executors.newSingleThreadScheduledExecutor(), HashicorpVaultExtension.NAME);
        this.hashicorpVaultClient = hashicorpVaultClient;
        this.renewBuffer = renewBuffer;
        this.monitor = monitor;
    }

    /**
     * Starts the scheduled token renewal.
     */
    public void start() {
        initTokenRenewalTask();
    }

    /**
     * Stops the scheduled token renewal. Running tasks will be interrupted.
     */
    public void stop() {
        if (tokenRenewTask != null) {
            tokenRenewTask.cancel(true);
        }
        scheduledExecutorService.shutdownNow();
    }

    /**
     * Performs the initial token lookup and renewal. Schedules the token renewal if both operations were successful.
     * Runs asynchronously.
     */
    private void initTokenRenewalTask() {
        scheduledExecutorService.execute(() -> {
            var tokenLookUpResult = hashicorpVaultClient.lookUpToken();

            if (tokenLookUpResult.failed()) {
                monitor.warning("Initial token look up failed with reason: %s".formatted(tokenLookUpResult.getFailureDetail()));
                return;
            }

            var tokenLookUpResponse = tokenLookUpResult.getContent();

            if (tokenLookUpResponse.getData().isRenewable()) {
                var tokenRenewResult = hashicorpVaultClient.renewToken();

                if (tokenRenewResult.failed()) {
                    monitor.warning("Initial token renewal failed with reason: %s".formatted(tokenRenewResult.getFailureDetail()));
                    return;
                }

                var tokenRenewResponse = tokenRenewResult.getContent();
                scheduleNextTokenRenewal(tokenRenewResponse.getAuth().getLeaseDuration());
            }
        });
    }

    /**
     * Schedules the token renewal which executes after a delay defined as {@code delay = ttl - renewBuffer}.
     * After successfully renewing the token the next renewal is scheduled. This operation will not be re-scheduled if
     * the renewal failed for some reason since tokens are invalidated forever after their ttl expires.
     *
     * @param ttl the ttl of the token
     */
    private void scheduleNextTokenRenewal(long ttl) {
        var delay = ttl - renewBuffer;

        tokenRenewTask = scheduledExecutorService.schedule(() -> {
            var tokenRenewResult = hashicorpVaultClient.renewToken();

            if (tokenRenewResult.succeeded()) {
                var tokenRenewResponse = tokenRenewResult.getContent();
                scheduleNextTokenRenewal(tokenRenewResponse.getAuth().getLeaseDuration());
            } else {
                monitor.warning("Scheduled token renewal failed: %s".formatted(tokenRenewResult.getFailureDetail()));
            }
        }, delay, TimeUnit.SECONDS);
    }
}
