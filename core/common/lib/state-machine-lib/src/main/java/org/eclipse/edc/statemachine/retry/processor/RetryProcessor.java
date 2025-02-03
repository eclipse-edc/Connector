/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.statemachine.retry.processor;

import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Component that watches processes to be executed on entities in the state machine.
 * One or more processes can either:
 * <ul>
 *   <li>not be executed because of retry policies not met</li>
 *   <li>executed successfully</li>
 *   <li>failed, to be retried on the next state machine iteration</li>
 *   <li>failed exceeding the retry limit</li>
 *   <li>failed with an unrecoverable error</li>
 * </ul>
 *
 * The component has been designed with a process chain component on which multiple processes can be chained.
 */
public class RetryProcessor<E extends StatefulEntity<E>, C> {

    private final E entity;
    private final Monitor monitor;
    private final Clock clock;
    private final EntityRetryProcessConfiguration configuration;
    private final Function<Void, CompletableFuture<ProcessContext<E, C>>> processChain;

    private BiConsumer<E, C> onSuccess;
    private BiConsumer<E, Throwable> onFailure;
    private BiConsumer<E, Throwable> onFinalFailure;

    public RetryProcessor(E entity, Monitor monitor, Clock clock, EntityRetryProcessConfiguration configuration) {
        this(entity, monitor, clock, configuration, v -> CompletableFuture.completedFuture(new ProcessContext<>(entity, null)));
    }

    private RetryProcessor(E entity, Monitor monitor, Clock clock, EntityRetryProcessConfiguration configuration, Function<Void, CompletableFuture<ProcessContext<E, C>>> processChain) {
        this.entity = entity;
        this.monitor = monitor;
        this.clock = clock;
        this.configuration = configuration;
        this.processChain = processChain;
    }

    public <C1> RetryProcessor<E, C1> doProcess(Process<E, C, C1> process) {
        return new RetryProcessor<>(entity, monitor, clock, configuration, c -> processChain.apply(c).thenCompose(process::execute));
    }

    public RetryProcessor<E, C> onSuccess(BiConsumer<E, C> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public RetryProcessor<E, C> onFailure(BiConsumer<E, Throwable> onFailure) {
        this.onFailure = onFailure;
        return this;
    }

    public RetryProcessor<E, C> onFinalFailure(BiConsumer<E, Throwable> onFinalFailure) {
        this.onFinalFailure = onFinalFailure;
        return this;
    }

    /**
     * Execute the processes applying eventual retry policy
     * Will execute the onSuccess, onFailure, onFinalFailure handlers at need
     *
     * @return true if the process has been run, false otherwise.
     */
    public boolean execute() {
        if (isRetry(entity)) {
            var delay = delayMillis(entity);
            if (delay > 0) {
                monitor.debug(String.format("Entity %s %s retry #%d will not be attempted before %d ms.", entity.getId(), entity.getClass().getSimpleName(), entity.getStateCount() - 1, delay));
                return false;
            } else {
                monitor.debug(String.format("Entity %s %s retry #%d of %d.", entity.getId(), entity.getClass().getSimpleName(), entity.getStateCount() - 1, configuration.retryLimit()));
            }
        }

        processChain.apply(null)
                .whenComplete((content, throwable) -> {
                    if (throwable == null) {
                        onSuccess.accept(content.entity(), content.content());
                    } else {
                        var cause = throwable.getCause();
                        if (cause instanceof UnrecoverableEntityStateException unrecoverable) {
                            monitor.severe(unrecoverable.getUnrecoverableMessage());
                            onFinalFailure.accept(entity, unrecoverable);
                        } else if (cause instanceof EntityStateException entityStateException) {
                            var exceptionEntity = entityStateException.getEntity();
                            if (exceptionEntity.getStateCount() > configuration.retryLimit()) {
                                monitor.severe(entityStateException.getRetryLimitExceededMessage());
                                onFinalFailure.accept(entity, entityStateException);
                            } else {
                                monitor.debug(entityStateException.getRetryFailedMessage());
                                onFailure.accept(entity, entityStateException);
                            }
                        } else {
                            monitor.severe("Runtime exception caught by retry processor: %s".formatted(cause.getMessage()), cause);
                            onFinalFailure.accept(entity, cause);
                        }
                    }
                });

        return true;
    }

    private boolean isRetry(E entity) {
        return entity.getStateCount() - 1 > 0;
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

}
