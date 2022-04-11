package org.eclipse.dataspaceconnector.transfer.core.transfer;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

import java.time.Clock;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Service enabling a "long retry" mechanism when sending {@link TransferProcess}es across applications.
 * The implementation supports a pluggable retry strategy (e.g. an exponential wait mechanism
 * so as not to overflow the remote service when it becomes available again).
 */
public class TransferProcessSendRetryManager implements SendRetryManager<TransferProcess> {
    private final Monitor monitor;
    private final Supplier<WaitStrategy> delayStrategySupplier;
    private final int retryLimit;
    protected Clock clock = Clock.systemUTC();

    public TransferProcessSendRetryManager(Monitor monitor, Supplier<WaitStrategy> delayStrategySupplier, int retryLimit) {
        this.monitor = monitor;
        this.delayStrategySupplier = delayStrategySupplier;
        this.retryLimit = retryLimit;
    }

    @Override
    public boolean shouldDelay(TransferProcess process) {
        int retryCount = process.getStateCount() - 1;
        if (retryCount <= 0) {
            return false;
        }

        // Get a new instance of WaitStrategy.
        var delayStrategy = delayStrategySupplier.get();

        // Set the WaitStrategy to have observed <retryCount> previous failures.
        // This is relevant for stateful strategies such as exponential wait.
        delayStrategy.failures(retryCount);

        // Get the delay time following the number of failures.
        var waitMillis = delayStrategy.retryInMillis();

        long remainingWaitMillis = process.getStateTimestamp() + waitMillis - clock.millis();
        if (remainingWaitMillis > 0) {
            monitor.debug(format("Process %s transfer retry #%d will not be attempted before %d ms.", process.getId(), retryCount, remainingWaitMillis));
            return true;
        }
        monitor.debug(format("Process %s transfer retry #%d of %d.", process.getId(), retryCount, retryLimit));
        return false;
    }

    @Override
    public boolean retriesExhausted(TransferProcess entity) {
        return entity.getStateCount() > retryLimit;
    }
}
