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

/**
 * Process implementation that handles a process that returns {@link StatusResult}
 *
 * @param <E> entity type.
 * @param <I> process input type.
 * @param <O> process output type.
 */
public class ResultRetryProcess<E extends StatefulEntity<E>, I, O> implements Process<E, I, O> {

    private final String name;
    private final BiFunction<E, I, StatusResult<O>> function;

    public ResultRetryProcess(String name, BiFunction<E, I, StatusResult<O>> function) {
        this.name = name;
        this.function = function;
    }

    @Override
    public CompletableFuture<ProcessContext<E, O>> execute(ProcessContext<E, I> context) {
        try {
            var result = function.apply(context.entity(), context.content());

            if (result.fatalError()) {
                return CompletableFuture.failedFuture(new UnrecoverableEntityStateException(context.entity(), name, result.getFailureDetail()));
            }

            if (result.failed()) {
                return CompletableFuture.failedFuture(new EntityStateException(context.entity(), name, result.getFailureDetail()));
            }

            return CompletableFuture.completedFuture(new ProcessContext<>(context.entity(), result.getContent()));
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }

    }
}
