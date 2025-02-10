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
import org.eclipse.edc.spi.response.ResponseFailure;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.statemachine.retry.processor.RetryProcessor;

import java.time.Clock;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Provides retry capabilities to a synchronous process that returns a {@link StatusResult} object
 *
 * @deprecated use {@link RetryProcessor}.
 */
@Deprecated(since = "0.12.0")
public class StatusResultRetryProcess<E extends StatefulEntity<E>, C> extends RetryProcess<E, StatusResultRetryProcess<E, C>> {
    private final Supplier<StatusResult<C>> process;
    private final Monitor monitor;
    private BiConsumer<E, C> onSuccessHandler;
    private BiConsumer<E, ResponseFailure> onFailureHandler;
    private BiConsumer<E, ResponseFailure> onFatalError;
    private BiConsumer<E, ResponseFailure> onRetryExhausted;

    public StatusResultRetryProcess(E entity, Supplier<StatusResult<C>> process, Monitor monitor, Clock clock, EntityRetryProcessConfiguration configuration) {
        super(entity, configuration, monitor, clock);
        this.process = process;
        this.monitor = monitor;
    }

    @Override
    boolean process(E entity, String description) {
        monitor.debug(format("%s: ID %s. %s", entity.getClass().getSimpleName(), entity.getId(), description));

        var result = runProcess();

        if (result.succeeded()) {
            if (onSuccessHandler != null) {
                onSuccessHandler.accept(entity, result.getContent());
            }
        } else {
            if (result.fatalError() && onFatalError != null) {
                monitor.severe(format("%s: ID %s. Fatal error while %s. Error details: %s",
                        entity.getClass().getSimpleName(), entity.getId(), description, result.getFailureDetail()));

                if (onFatalError != null) {
                    onFatalError.accept(entity, result.getFailure());
                }
            } else if (retriesExhausted(entity)) {
                var message = format("%s: ID %s. Attempt #%d failed to %s. Retry limit exceeded. Cause: %s",
                        entity.getClass().getSimpleName(),
                        entity.getId(),
                        entity.getStateCount(),
                        description,
                        result.getFailureDetail());
                monitor.severe(message);

                if (onRetryExhausted != null) {
                    onRetryExhausted.accept(entity, result.getFailure());
                }
            } else {
                var message = format("%s: ID %s. Attempt #%d failed to %s. Cause: %s",
                        entity.getClass().getSimpleName(),
                        entity.getId(),
                        entity.getStateCount(),
                        description,
                        result.getFailureDetail());

                monitor.debug(message);

                if (onFailureHandler != null) {
                    onFailureHandler.accept(entity, result.getFailure());
                }
            }
        }

        return true;
    }

    public StatusResultRetryProcess<E, C> onSuccess(BiConsumer<E, C> onSuccessHandler) {
        this.onSuccessHandler = onSuccessHandler;
        return this;
    }

    public StatusResultRetryProcess<E, C> onFailure(BiConsumer<E, ResponseFailure> onFailureHandler) {
        this.onFailureHandler = onFailureHandler;
        return this;
    }

    public StatusResultRetryProcess<E, C> onFatalError(BiConsumer<E, ResponseFailure> onFatalError) {
        this.onFatalError = onFatalError;
        return this;
    }

    public StatusResultRetryProcess<E, C> onRetryExhausted(BiConsumer<E, ResponseFailure> onRetryExhausted) {
        this.onRetryExhausted = onRetryExhausted;
        return this;
    }

    private StatusResult<C> runProcess() {
        try {
            return process.get();
        } catch (Throwable e) {
            return StatusResult.failure(ERROR_RETRY, "Unexpected exception thrown %s: %s".formatted(e, e.getMessage()));
        }
    }

}
