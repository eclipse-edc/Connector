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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
     * Converts a {@link Optional} into a result, interpreting the Optional's value as content.
     *
     * @return {@link Result#failure(String)} if the Optional is empty, {@link Result#success(Object)} using the
     *         Optional's value otherwise.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Result<T> from(Optional<T> opt) {
        return opt.map(Result::success).orElse(Result.failure("Empty optional"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R1 extends AbstractResult<C1, Failure, R1>, C1> R1 newInstance(C1 content, Failure failure) {
        return (R1) new Result<>(content, failure);
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

    /**
     * Maps this {@link Result} into another, maintaining the basic semantics (failed vs success). If this
     * {@link Result} is successful, the content is discarded. If this {@link Result} failed, the failures are carried
     * over. This method is intended for use when the return type is implicit, for example:
     * <pre>
     *   public Result&lt;Void&gt; someMethod() {
     *      Result&lt;String&gt; result = getStringResult();
     *      return result.mapTo();
     *   }
     * </pre>
     *
     * @see Result#map(Function)
     * @see Result#mapTo(Class)
     */
    public <R> Result<R> mapTo() {
        if (succeeded()) {
            return new Result<>(null, null);
        } else {
            return Result.failure(getFailureMessages());
        }
    }

    /**
     * Maps this {@link Result} into another, maintaining the basic semantics (failed vs success). If this
     * {@link Result} is successful, the content is discarded. If this {@link Result} failed, the failures are carried
     * over. This method is intended for use when an explicit return type is needed, for example when using var:
     * <pre>
     *      Result&lt;String&gt; result = getStringResult();
     *      var voidResult = result.mapTo(Void.class);
     * </pre>
     *
     * @param clazz type of the result, with which the resulting {@link Result} should be parameterized
     * @see Result#map(Function)
     * @see Result#mapTo()
     */
    public <R> Result<R> mapTo(Class<R> clazz) {
        return mapTo();
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

}
