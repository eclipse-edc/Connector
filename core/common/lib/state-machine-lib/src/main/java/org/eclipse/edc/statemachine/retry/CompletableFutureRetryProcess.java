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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Provides retry capabilities to an asynchronous process that returns a {@link CompletableFuture} object
 *
 * @deprecated use {@link RetryProcessor}.
 */
@Deprecated(since = "0.12.0")
public class CompletableFutureRetryProcess<E extends StatefulEntity<E>, C, SELF extends CompletableFutureRetryProcess<E, C, SELF>>
        extends RetryProcess<E, CompletableFutureRetryProcess<E, C, SELF>> {
    private final Supplier<CompletableFuture<C>> process;
    private final Monitor monitor;
    private Function<String, E> entityRetrieve;
    protected BiConsumer<E, C> onSuccessHandler;
    protected BiConsumer<E, Throwable> onFailureHandler;
    protected BiConsumer<E, Throwable> onRetryExhausted;

    public CompletableFutureRetryProcess(E entity, Supplier<CompletableFuture<C>> process, Monitor monitor, Clock clock, EntityRetryProcessConfiguration configuration) {
        super(entity, configuration, monitor, clock);
        this.process = process;
        this.monitor = monitor;
    }

    @Override
    boolean process(E entity, String description) {
        monitor.debug(format("%s: ID %s. %s", entity.getClass().getSimpleName(), entity.getId(), description));

        runProcess()
                .whenComplete((result, throwable) -> {
                    var reloadedEntity = Optional.ofNullable(entityRetrieve)
                            .map(it -> it.apply(entity.getId()))
                            .orElse(entity);

                    if (throwable == null) {
                        onSuccessHandler.accept(reloadedEntity, result);
                    } else {
                        if (retriesExhausted(reloadedEntity)) {
                            var message = format("%s: ID %s. Attempt #%d failed to %s. Retry limit exceeded. Cause: %s",
                                    reloadedEntity.getClass().getSimpleName(),
                                    reloadedEntity.getId(),
                                    reloadedEntity.getStateCount(),
                                    description,
                                    throwable.getMessage());
                            monitor.severe(message, throwable);

                            onRetryExhausted.accept(reloadedEntity, throwable);
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

    private CompletableFuture<C> runProcess() {
        try {
            return process.get();
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public SELF onSuccess(BiConsumer<E, C> onSuccessHandler) {
        this.onSuccessHandler = onSuccessHandler;
        return (SELF) this;
    }

    public SELF onFailure(BiConsumer<E, Throwable> onFailureHandler) {
        this.onFailureHandler = onFailureHandler;
        return (SELF) this;
    }

    public SELF entityRetrieve(Function<String, E> entityRetrieve) {
        this.entityRetrieve = entityRetrieve;
        return (SELF) this;
    }

    public SELF onRetryExhausted(BiConsumer<E, Throwable> onRetryExhausted) {
        this.onRetryExhausted = onRetryExhausted;
        return (SELF) this;
    }
}
