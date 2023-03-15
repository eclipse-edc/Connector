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

package org.eclipse.edc.statemachine.retry;

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.retry.WaitStrategy;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service enabling a "long retry" mechanism when sending entities across applications.
 * The implementation supports a pluggable retry strategy (e.g. an exponential wait mechanism
 * so as not to overflow the remote service when it becomes available again).
 */
public class EntitySendRetryManager implements SendRetryManager {
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
    public <T extends StatefulEntity<T>> boolean shouldDelay(T entity) {
        int retryCount = entity.getStateCount() - 1;
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

        long remainingWaitMillis = entity.getStateTimestamp() + waitMillis - clock.millis();
        if (remainingWaitMillis > 0) {
            monitor.debug(String.format("Entity %s %s retry #%d will not be attempted before %d ms.", entity.getId(), entity.getClass().getSimpleName(), retryCount, remainingWaitMillis));
            return true;
        } else {
            monitor.debug(String.format("Entity %s %s retry #%d of %d.", entity.getId(), entity.getClass().getSimpleName(), retryCount, retryLimit));
            return false;
        }

    }

    @Override
    public <T extends StatefulEntity<T>> boolean retriesExhausted(T entity) {
        return entity.getStateCount() > retryLimit;
    }

    @Override
    public <T extends StatefulEntity<T>> SimpleRetryProcess<T> doSimpleProcess(T entity, Supplier<Boolean> process) {
        return new SimpleRetryProcess<>(entity, process, this);
    }

    @Override
    public <T extends StatefulEntity<T>, C> StatusResultRetryProcess<T, C> doSyncProcess(T entity, String description, Supplier<StatusResult<C>> process) {
        return new StatusResultRetryProcess<>(entity, process, this, monitor, description);
    }

    @Override
    public <T extends StatefulEntity<T>, C> CompletableFutureRetryProcess<T, C> doAsyncProcess(T entity, String description, Supplier<CompletableFuture<C>> process) {
        return new CompletableFutureRetryProcess<>(entity, process, this, monitor, description);
    }

}
