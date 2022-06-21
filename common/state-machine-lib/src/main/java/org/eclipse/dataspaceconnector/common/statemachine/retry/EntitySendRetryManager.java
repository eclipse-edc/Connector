/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.common.statemachine.retry;

import org.eclipse.dataspaceconnector.spi.entity.StatefulEntity;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;

import java.time.Clock;
import java.util.function.Supplier;

/**
 * Service enabling a "long retry" mechanism when sending entities across applications.
 * The implementation supports a pluggable retry strategy (e.g. an exponential wait mechanism
 * so as not to overflow the remote service when it becomes available again).
 */
public class EntitySendRetryManager implements SendRetryManager<StatefulEntity> {
    private final Monitor monitor;
    private final Supplier<WaitStrategy> delayStrategySupplier;
    private final int retryLimit;
    private final Clock clock;

    public EntitySendRetryManager(Monitor monitor, Supplier<WaitStrategy> delayStrategySupplier, Clock clock, int retryLimit) {
        this.monitor = monitor;
        this.delayStrategySupplier = delayStrategySupplier;
        this.clock = clock;
        this.retryLimit = retryLimit;
    }

    @Override
    public boolean shouldDelay(StatefulEntity process) {
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
            monitor.debug(String.format("Process %s %s retry #%d will not be attempted before %d ms.", process.getId(), process.getClass().getSimpleName(), retryCount, remainingWaitMillis));
            return true;
        } else {
            monitor.debug(String.format("Process %s %s retry #%d of %d.", process.getId(), process.getClass().getSimpleName(), retryCount, retryLimit));
            return false;
        }

    }

    @Override
    public boolean retriesExhausted(StatefulEntity entity) {
        return entity.getStateCount() > retryLimit;
    }
}
