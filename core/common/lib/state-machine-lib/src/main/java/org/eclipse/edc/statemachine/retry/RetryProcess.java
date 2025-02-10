/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.statemachine.retry;

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.statemachine.retry.processor.RetryProcessor;

import java.time.Clock;
import java.util.function.Consumer;

/**
 * Represent a process on a {@link StatefulEntity} that is retried after a certain delay if it fails.
 * This works only used on a state machine, where states are persisted.
 * The process is a unit of logic that can be executed on the entity.
 *
 * @deprecated use {@link RetryProcessor}.
 */
@Deprecated(since = "0.12.0")
public abstract class RetryProcess<E extends StatefulEntity<E>, SELF extends RetryProcess<E, SELF>> {

    private final E entity;
    protected final EntityRetryProcessConfiguration configuration;
    protected final Monitor monitor;
    protected final Clock clock;
    protected Consumer<E> onDelay;
    protected String description;

    protected RetryProcess(E entity, EntityRetryProcessConfiguration configuration, Monitor monitor, Clock clock) {
        this.entity = entity;
        this.configuration = configuration;
        this.monitor = monitor;
        this.clock = clock;
    }

    /**
     *  Execute some logic on the {@link E} entity, return true if the process happened, false otherwise.
     */
    abstract boolean process(E entity, String description);

    /**
     * If entity is not yet ready to be processed executes {@link #onDelay} handler and return false,
     * otherwise processes it.
     *
     * @param description the process description.
     * @return false if process should not be run yet, the result of the process otherwise.
     */
    public boolean execute(String description) {
        this.description = description;
        if (isRetry(entity)) {
            var delay = delayMillis(entity);
            if (delay > 0) {
                monitor.debug(String.format("Entity %s %s retry #%d will not be attempted before %d ms.", entity.getId(), entity.getClass().getSimpleName(), entity.getStateCount() - 1, delay));
                if (onDelay != null) {
                    onDelay.accept(entity);
                }
                return false;
            } else {
                monitor.debug(String.format("Entity %s %s retry #%d of %d.", entity.getId(), entity.getClass().getSimpleName(), entity.getStateCount() - 1, configuration.retryLimit()));
            }
        }

        return process(entity, description);
    }

    /**
     * Handler that is called if the entity is not yet ready for processing
     */
    public SELF onDelay(Consumer<E> onDelay) {
        this.onDelay = onDelay;
        return (SELF) this;
    }

    /**
     * Determines whether retries for sending the given entity have been exhausted.
     *
     * @param entity entity to be evaluated.
     * @return {@code true} if the entity should not be sent anymore.
     */
    protected boolean retriesExhausted(E entity) {
        return entity.getStateCount() > configuration.retryLimit();
    }

    private long delayMillis(E entity) {
        // Get a new instance of WaitStrategy.
        var delayStrategy = configuration.delayStrategySupplier().get();

        // Set the WaitStrategy to have observed <retryCount> previous failures.
        // This is relevant for stateful strategies such as exponential wait.
        delayStrategy.failures(entity.getStateCount() - 1);

        // Get the delay time following the number of failures.
        var waitMillis = delayStrategy.retryInMillis();

        return entity.getStateTimestamp() + waitMillis - clock.millis();
    }

    private boolean isRetry(E entity) {
        return entity.getStateCount() - 1 > 0;
    }
}
