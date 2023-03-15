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

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Provides retry capabilities to an asynchronous process that returns a {@link CompletableFuture} object
 */
public class CompletableFutureRetryProcess<E extends StatefulEntity<E>, C> extends RetryProcess<E, CompletableFutureRetryProcess<E, C>> {
    private final Supplier<CompletableFuture<C>> process;
    private final Monitor monitor;
    private Function<String, E> entityRetrieve;
    private BiConsumer<E, C> onSuccessHandler;
    private BiConsumer<E, Throwable> onFailureHandler;
    private BiConsumer<E, Throwable> onRetryExhausted;

    public CompletableFutureRetryProcess(E entity, Supplier<CompletableFuture<C>> process, SendRetryManager sendRetryManager, Monitor monitor) {
        super(entity, sendRetryManager);
        this.process = process;
        this.monitor = monitor;
    }

    @Override
    boolean process(E entity, String description) {
        monitor.debug(format("%s: ID %s. %s", entity.getClass().getSimpleName(), entity.getId(), description));
        process.get()
                .whenComplete((result, throwable) -> {
                    var reloadedEntity = entityRetrieve.apply(entity.getId());

                    if (throwable == null) {
                        onSuccessHandler.accept(reloadedEntity, result);
                    } else {
                        if (retriesExhausted(entity)) {
                            var message = format("%s: ID %s. Attempt #%d failed to %s. Retry limit exceeded. Cause: %s",
                                    reloadedEntity.getClass().getSimpleName(),
                                    reloadedEntity.getId(),
                                    reloadedEntity.getStateCount(),
                                    description,
                                    throwable.getMessage());
                            monitor.severe(message, throwable);

                            onRetryExhausted.accept(entity, throwable);
                        } else {
                            var message = format("%s: ID %s. Attempt #%d failed to %s. Cause: %s",
                                    reloadedEntity.getClass().getSimpleName(),
                                    reloadedEntity.getId(),
                                    reloadedEntity.getStateCount(),
                                    description,
                                    throwable.getMessage());
                            monitor.debug(message, throwable);

                            onFailureHandler.accept(reloadedEntity, throwable);
                        }
                    }
                });

        return true;
    }

    public CompletableFutureRetryProcess<E, C> onSuccess(BiConsumer<E, C> onSuccessHandler) {
        this.onSuccessHandler = onSuccessHandler;
        return this;
    }

    public CompletableFutureRetryProcess<E, C> onFailure(BiConsumer<E, Throwable> onFailureHandler) {
        this.onFailureHandler = onFailureHandler;
        return this;
    }

    public CompletableFutureRetryProcess<E, C> entityRetrieve(Function<String, E> entityRetrieve) {
        this.entityRetrieve = entityRetrieve;
        return this;
    }

    public CompletableFutureRetryProcess<E, C> onRetryExhausted(BiConsumer<E, Throwable> onRetryExhausted) {
        this.onRetryExhausted = onRetryExhausted;
        return this;
    }
}
