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
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.failedFuture;

/**
 * Process implementation that handles a process that returns {@link CompletableFuture}
 *
 * @param <E> entity type.
 * @param <I> process input type.
 * @param <O> process output type.
 */
public class FutureRetryProcess<E extends StatefulEntity<E>, I, O> implements Process<E, I, O> {

    private final String name;
    private final BiFunction<E, I, CompletableFuture<O>> function;
    private Function<String, StoreResult<E>> entityReload;

    public FutureRetryProcess(String name, BiFunction<E, I, CompletableFuture<O>> function) {
        this.name = name;
        this.function = function;
    }

    @Override
    public CompletableFuture<ProcessContext<E, O>> execute(ProcessContext<E, I> context) {
        try {
            return function.apply(context.entity(), context.content())
                    .handle((content, throwable) -> {
                        var reloadedEntity = entityReload == null
                                ? context.entity()
                                : entityReload.apply(context.entity().getId()).orElseThrow(failedEntityReload(context));

                        if (throwable == null) {
                            return new ProcessContext<>(reloadedEntity, content);
                        } else {
                            throw new EntityStateException(reloadedEntity, name, throwable.getMessage());
                        }
                    });
        } catch (Throwable throwable) {
            return failedFuture(new UnrecoverableEntityStateException(context.entity(), name, throwable.getMessage()));
        }
    }

    public FutureRetryProcess<E, I, O> entityReload(Function<String, StoreResult<E>> entityReload) {
        this.entityReload = entityReload;
        return this;
    }

    private @NotNull Function<StoreFailure, UnrecoverableEntityStateException> failedEntityReload(ProcessContext<E, I> context) {
        return failure -> new UnrecoverableEntityStateException(context.entity(), name, "Cannot reload entity: " + failure.getFailureDetail());
    }

}
