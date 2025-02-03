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

import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseFailure;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.statemachine.retry.processor.RetryProcessor;

import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Provides retry capabilities to an asynchronous process that returns a {@link CompletableFuture} with a {@link StatusResult} content
 *
 * @deprecated use {@link RetryProcessor}.
 */
@Deprecated(since = "0.12.0")
public class AsyncStatusResultRetryProcess<E extends StatefulEntity<E>, C, SELF extends AsyncStatusResultRetryProcess<E, C, SELF>>
        extends CompletableFutureRetryProcess<E, StatusResult<C>, SELF> {
    private final Monitor monitor;
    private BiConsumer<E, ResponseFailure> onFatalError;

    public AsyncStatusResultRetryProcess(E entity, Supplier<CompletableFuture<StatusResult<C>>> process, Monitor monitor, Clock clock, EntityRetryProcessConfiguration configuration) {
        super(entity, process, monitor, clock, configuration);
        this.monitor = monitor;
    }

    @Override
    public SELF onSuccess(BiConsumer<E, StatusResult<C>> onSuccessHandler) {
        return onSuccessResult((entity, result) -> onSuccessHandler.accept(entity, StatusResult.success(result)));
    }

    public SELF onSuccessResult(BiConsumer<E, C> onSuccessHandler) {
        this.onSuccessHandler = (entity, result) -> {
            new StatusResultRetryProcess<>(entity, () -> result, monitor, clock, configuration)
                    .onSuccess(onSuccessHandler)
                    .onFatalError(onFatalError)
                    .onRetryExhausted((e, failure) -> onRetryExhausted.accept(e, new EdcException(failure.getFailureDetail())))
                    .onFailure((e, failure) -> onFailureHandler.accept(e, new EdcException(failure.getFailureDetail())))
                    .process(entity, description);
        };
        return (SELF) this;
    }

    public SELF onFatalError(BiConsumer<E, ResponseFailure> onFatalError) {
        this.onFatalError = onFatalError;
        return (SELF) this;
    }
}
