package org.eclipse.dataspaceconnector.core.base;

import org.eclipse.dataspaceconnector.spi.transfer.WaitStrategy;

public class GracefulShutdownWaitStrategy implements WaitStrategy {
    @Override
    public long waitForMillis() {
        return 0;
    }

    @Override
    public void success() {
        WaitStrategy.super.success();
    }

    @Override
    public long retryInMillis() {
        return WaitStrategy.super.retryInMillis();
    }
}
