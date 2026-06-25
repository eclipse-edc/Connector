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
import org.eclipse.edc.spi.result.AbstractResult;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static java.util.concurrent.CompletableFuture.failedFuture;

/**
 * Process implementation that handles a process that returns {@link CompletableFuture} with enclosed {@link StatusResult}
 *
 * @param <E> entity type.
 * @param <I> process input type.
 * @param <O> process output type.
 */
public class FutureResultRetryProcess<E extends StatefulEntity<E>, I, O, R extends AbstractResult<O, ?, R>> implements Process<E, I, O> {

    private final String name;
    private final BiFunction<E, I, CompletableFuture<R>> function;

    public FutureResultRetryProcess(String name, BiFunction<E, I, CompletableFuture<R>> function) {
        this.name = name;
        this.function = function;
    }

    @Override
    public CompletableFuture<ProcessContext<E, O>> execute(ProcessContext<E, I> context) {
        try {
            return new FutureRetryProcess<>(name, function)
                    .execute(context)
                    .thenCompose(asyncContext -> new ResultRetryProcess<E, I, O, R>(name, (e, c) -> asyncContext.content())
                    .execute(new ProcessContext<>(asyncContext.entity(), context.content())));
        } catch (Throwable throwable) {
            return failedFuture(new UnrecoverableEntityStateException(context.entity(), name, throwable.getMessage()));
        }
    }

}
