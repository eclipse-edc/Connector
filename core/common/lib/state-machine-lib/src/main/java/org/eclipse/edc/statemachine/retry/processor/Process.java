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

/**
 * Represent a single unit of processing on an entity
 *
 * @param <E> entity type.
 * @param <I> process input type.
 * @param <O> process output type.
 */
public interface Process<E extends StatefulEntity<E>, I, O> {

    /**
     * Instantiates a process that returns a {@link StatusResult}
     *
     * @param name the process name, will be used for logging.
     * @param function that executes the process.
     * @return the process instance.
     */
    static <E extends StatefulEntity<E>, I, O, R extends AbstractResult<O, ?, R>> Process<E, I, O> result(String name, BiFunction<E, I, R> function) {
        return new ResultRetryProcess<>(name, function);
    }

    /**
     * Instantiates a process that returns a {@link CompletableFuture}
     *
     * @param name the process name, will be used for logging.
     * @param function that executes the process.
     * @return the process instance.
     */
    static <E extends StatefulEntity<E>, I, O> Process<E, I, O> future(String name, BiFunction<E, I, CompletableFuture<O>> function) {
        return new FutureRetryProcess<>(name, function);
    }

    /**
     * Instantiates a process that returns a {@link CompletableFuture} that encloses a {@link StatusResult}
     *
     * @param name the process name, will be used for logging.
     * @param function that executes the process.
     * @return the process instance.
     */
    static <E extends StatefulEntity<E>, I, O, R extends AbstractResult<O, ?, R>> Process<E, I, O> futureResult(String name, BiFunction<E, I, CompletableFuture<R>> function) {
        return new FutureResultRetryProcess<>(name, function);
    }

    /**
     * Function that wraps the enclosed content type into a {@link CompletableFuture}
     *
     * @param context the process context.
     * @return a future containing the response type.
     */
    CompletableFuture<ProcessContext<E, O>> execute(ProcessContext<E, I> context);

}
