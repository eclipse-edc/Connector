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

package org.eclipse.edc.spi.result;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A generic result type.
 */
public class Result<T> extends AbstractResult<T, Failure, Result<T>> {

    private Result(T content, Failure failure) {
        super(content, failure);
    }

    public static Result<Void> success() {
        return new Result<>(null, null);
    }

    public static <T> Result<T> success(T content) {
        return new Result<>(content, null);
    }

    public static <T> Result<T> failure(String failure) {
        return new Result<>(null, new Failure(List.of(failure)));
    }

    public static <T> Result<T> failure(List<String> failures) {
        return new Result<>(null, new Failure(failures));
    }


    /**
     * Runs the block provided by the {@link Supplier} and wrap the return into a Result
     *
     * @param supplier The block to execute
     * @return The Result of the supplier call. Success if no {@link Exception} were thrown. Failure otherwise
     */
    public static <T> Result<T> ofThrowable(Supplier<T> supplier) {
        try {
            return Result.success(supplier.get());
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }

    /**
     * Converts a {@link Optional} into a result, interpreting the Optional's value as content.
     *
     * @return {@link Result#failure(String)} if the Optional is empty, {@link Result#success(Object)} using the Optional's value otherwise.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Result<T> from(Optional<T> opt) {
        return opt.map(Result::success).orElse(Result.failure("Empty optional"));
    }

    public static <T> Collector<Result<T>, ?, Result<List<T>>> collector() {
        return Collector.of(
                ResultAccumulator<T>::new,
                ResultAccumulator::add,
                ResultAccumulator::combine,
                ResultAccumulator::finish
        );
    }

    /**
     * Merges this result with another one. If both Results are successful, a new one is created with no content. If
     * either this result or {@code other} is a failure, the merged result will be a {@code failure()} and will contain
     * all failure messages, no content. If both results are failures, the merged result will contain failure messages
     * of {@code this} result, then the failure messages of {@code other}.
     */
    public <R> Result<R> merge(Result<?> other) {
        if (succeeded() && other.succeeded()) {
            return new Result<>(null, null);
        } else {
            var messages = new ArrayList<String>();
            messages.addAll(Optional.ofNullable(getFailure()).map(Failure::getMessages).orElse(Collections.emptyList()));
            messages.addAll(Optional.ofNullable(other.getFailure()).map(Failure::getMessages).orElse(Collections.emptyList()));
            return Result.failure(messages);
        }
    }

    /**
     * Converts this result into an {@link Optional}. When this result is failed, or there is no content,
     * {@link Optional#isEmpty()} is returned, otherwise the content is the {@link Optional}'s value
     *
     * @return {@link Optional#empty()} if failed, or no content, {@link Optional#of(Object)} otherwise.
     */
    public Optional<T> asOptional() {
        return succeeded() && getContent() != null ? Optional.of(getContent()) : Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    @NotNull
    protected <R1 extends AbstractResult<C1, Failure, R1>, C1> R1 newInstance(@Nullable C1 content, @Nullable Failure failure) {
        return (R1) new Result<>(content, failure);
    }

    private static class ResultAccumulator<T> {
        final List<T> successes = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        void add(Result<T> result) {
            if (result.succeeded()) {
                successes.add(result.getContent());
            } else {
                errors.addAll(result.getFailureMessages());
            }
        }

        ResultAccumulator<T> combine(ResultAccumulator<T> other) {
            successes.addAll(other.successes);
            errors.addAll(other.errors);
            return this;
        }

        Result<List<T>> finish() {
            if (errors.isEmpty()) {
                return Result.success(successes);
            } else {
                return Result.failure(errors);
            }
        }
    }

}
