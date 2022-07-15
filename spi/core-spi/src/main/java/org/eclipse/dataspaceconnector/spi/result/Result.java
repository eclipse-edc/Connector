/*
 *  Copyright (c) 2021 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.spi.result;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A generic result type.
 */
public class Result<T> extends AbstractResult<T, Failure> {

    private Result(T content, Failure failure) {
        super(content, failure);
    }

    public static Result<Void> success() {
        return new Result<>(null, null);
    }

    public static <T> Result<T> success(@NotNull T content) {
        return new Result<>(content, null);
    }

    public static <T> Result<T> failure(String failure) {
        return new Result<>(null, new Failure(List.of(failure)));
    }

    public static <T> Result<T> failure(List<String> failures) {
        return new Result<>(null, new Failure(failures));
    }

    /**
     * Merges this result with another one. If both Results are successful, a new one is created with no content. If
     * either this result or {@code other} is a failure, the merged result will be a {@code failure()} and will contain
     * all failure messages, no content. If both results are failures, the merged result will contain failure messages
     * of {@code this} result, then the failure messages of {@code other}.
     */
    public Result<T> merge(Result<T> other) {
        if (succeeded() && other.succeeded()) {
            return new Result<>(null, null);
        } else {
            var messages = new ArrayList<String>();
            messages.addAll(Optional.ofNullable(getFailure()).map(Failure::getMessages).orElse(Collections.emptyList()));
            messages.addAll(Optional.ofNullable(other.getFailure()).map(Failure::getMessages).orElse(Collections.emptyList()));
            return Result.failure(messages);
        }
    }

    public <R> Result<R> map(Function<T, R> mapFunction) {
        if (succeeded()) {
            return Result.success(mapFunction.apply(getContent()));
        } else {
            return Result.failure(getFailureMessages());
        }
    }

}

