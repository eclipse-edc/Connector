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
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
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
 *
 * @param <E> entity type.
 * @param <C> content type that is returned by the {@link #processChain} and that will be available in the {@link #onSuccess} handler.
 *
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
     * @return success if the execution succeeded, failure otherwise.
     */
    public CompletableFuture<StatusResult<Void>> execute() {
        return processChain.apply(null)
                .handle((content, throwable) -> {
                    if (throwable == null) {
                        onSuccess.accept(content.entity(), content.content());
                        return StatusResult.success();
                    } else {
                        var cause = throwable.getCause();
                        if (cause instanceof UnrecoverableEntityStateException unrecoverable) {
                            monitor.severe(unrecoverable.getUnrecoverableMessage());
                            onFinalFailure.accept(entity, unrecoverable);
                            return StatusResult.failure(ResponseStatus.FATAL_ERROR, unrecoverable.getUnrecoverableMessage());
                        } else if (cause instanceof EntityStateException entityStateException) {
                            var exceptionEntity = entityStateException.getEntity();
                            if (exceptionEntity.getStateCount() > configuration.retryLimit()) {
                                monitor.severe(entityStateException.getRetryLimitExceededMessage());
                                onFinalFailure.accept(entity, entityStateException);
                                return StatusResult.failure(ResponseStatus.FATAL_ERROR, entityStateException.getRetryLimitExceededMessage());
                            } else {
                                monitor.debug(entityStateException.getRetryFailedMessage());
                                onFailure.accept(entity, entityStateException);
                                return StatusResult.failure(ResponseStatus.ERROR_RETRY, entityStateException.getRetryFailedMessage());
                            }
                        } else {
                            var errorMessage = "Runtime exception caught by retry processor: %s".formatted(cause.getMessage());
                            monitor.severe(errorMessage, cause);
                            onFinalFailure.accept(entity, cause);
                            return StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMessage);
                        }
                    }
                });
    }

}
