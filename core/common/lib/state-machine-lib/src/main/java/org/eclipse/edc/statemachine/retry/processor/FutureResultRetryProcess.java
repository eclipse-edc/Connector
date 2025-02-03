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
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.failedFuture;

/**
 * Process implementation that handles a process that returns {@link CompletableFuture} with enclosed {@link StatusResult}
 */
public class FutureResultRetryProcess<E extends StatefulEntity<E>, I, O> implements Process<E, I, O> {

    private final String name;
    private final BiFunction<E, I, CompletableFuture<StatusResult<O>>> function;
    private Function<String, E> entityReload;

    public FutureResultRetryProcess(String name, BiFunction<E, I, CompletableFuture<StatusResult<O>>> function) {
        this.name = name;
        this.function = function;
    }

    @Override
    public CompletableFuture<ProcessContext<E, O>> execute(ProcessContext<E, I> context) {
        try {
            return new FutureRetryProcess<>(name, function).entityReload(entityReload)
                    .execute(context)
                    .thenCompose(asyncContext -> new ResultRetryProcess<E, I, O>(name, (e, c) -> asyncContext.content())
                    .execute(new ProcessContext<>(asyncContext.entity(), context.content())));
        } catch (Throwable throwable) {
            return failedFuture(new UnrecoverableEntityStateException(reloadEntity(context.entity()), name, throwable.getMessage()));
        }
    }

    public FutureResultRetryProcess<E, I, O> entityReload(Function<String, E> entityReload) {
        this.entityReload = entityReload;
        return this;
    }

    private E reloadEntity(E entity) {
        return entityReload == null ? entity : entityReload.apply(entity.getId());
    }

}
