/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.spi.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base result type used by services to indicate success or failure.
 * <p>
 * Service operations should generally never throw checked exceptions. Instead, they should return concrete result types
 * and raise unchecked exceptions only when an unexpected event happens, such as a programming error.
 *
 * @param <F> The type of {@link Failure}.
 * @param <T> The type of the content.
 * @param <R> The result self type.
 */
public abstract class AbstractResult<T, F extends Failure, R extends AbstractResult<T, F, R>> {

    private final T content;
    private final F failure;

    protected AbstractResult(T content, F failure) {
        this.content = content;
        this.failure = failure;
    }

    public T getContent() {
        return content;
    }

    public F getFailure() {
        return failure;
    }

    //will cause problems during JSON serialization if failure is null TODO: is this comment still valid?
    @JsonIgnore
    public List<String> getFailureMessages() {
        return failure == null ? List.of() : failure.getMessages();
    }

    public boolean succeeded() {
        return failure == null;
    }

    public boolean failed() {
        return !succeeded();
    }

    /**
     * Returns a string that contains all the failure messages.
     *
     * @return a string that contains all the failure messages.
     */
    @JsonIgnore // will cause problems during JSON serialization if failure is null TODO: is this comment still valid?
    public String getFailureDetail() {
        return failure == null ? null : failure.getFailureDetail();
    }

    /**
     * Executes a {@link Consumer} if this {@link Result} is successful
     */
    public R onSuccess(Consumer<T> successAction) {
        if (succeeded()) {
            successAction.accept(getContent());
        }
        return self();
    }

    /**
     * Executes a {@link Consumer} if this {@link Result} failed. Passes the {@link Failure} to the consumer
     */
    public R onFailure(Consumer<F> failureAction) {
        if (failed()) {
            failureAction.accept(getFailure());
        }
        return self();
    }

    /**
     * Execute an action if this {@link Result} is not successful.
     *
     * @param failureAction The function that maps a {@link Failure} into the content.
     * @return T the success value if successful, otherwise the object returned by the failureAction
     */
    public T orElse(Function<F, T> failureAction) {
        if (failed()) {
            return failureAction.apply(getFailure());
        } else {
            return getContent();
        }
    }

    /**
     * Throws an exception returned by the mapper {@link Function} if this {@link Result} is not successful.
     *
     * @param exceptionMapper The function that maps a {@link Failure} into an exception.
     * @return T the success value
     */
    public <X extends Throwable> T orElseThrow(Function<F, X> exceptionMapper) throws X {
        if (failed()) {
            throw exceptionMapper.apply(getFailure());
        } else {
            return getContent();
        }
    }

    /**
     * Return this instance, cast to the {@link R} self type.
     *
     * @return this instance.
     */
    @SuppressWarnings({"unchecked"})
    public R self() {
        return (R) this;
    }

    /**
     * Returns a new result instance.
     * This default implementation exists only to avoid breaking changes, in a future this should become an abstract
     * method.
     * If this {@link UnsupportedOperationException} was thrown, please override this method with a proper behavior.
     *
     * @param content the content.
     * @param failure the failure.
     * @param <R1> the new result type.
     * @param <C1> the new content type.
     * @return a new result instance
     */
    @NotNull
    protected <R1 extends AbstractResult<C1, F, R1>, C1> R1 newInstance(@Nullable C1 content, @Nullable F failure) {
        throw new UnsupportedOperationException("Not implemented for " + getClass());
    }

    /**
     * Map the content into another, applying the mapping function.
     *
     * @param mapFunction a function that converts the content into another.
     * @param <T2> the new content type.
     * @param <R2> the new result type.
     * @return a new result with a mapped content if succeeded, a new failed one otherwise.
     */
    public <T2, R2 extends AbstractResult<T2, F, R2>> R2 map(Function<T, T2> mapFunction) {
        if (succeeded()) {
            return newInstance(mapFunction.apply(getContent()), null);
        } else {
            return newInstance(null, getFailure());
        }
    }

    /**
     * Maps one result into another, applying the mapping function.
     *
     * @param mappingFunction a function converting this result into another
     * @return the result of the mapping function
     */
    public <T2, F2 extends Failure, R2 extends AbstractResult<T2, F2, R2>> R2 flatMap(Function<R, R2> mappingFunction) {
        return mappingFunction.apply(self());
    }

    /**
     * If the result is successful maps the content into a result applying the mapping function, otherwise do nothing.
     *
     * @param mappingFunction a function converting this result into another
     * @return the result of the mapping function
     */
    public <T2, R2 extends AbstractResult<T2, F, R2>> R2 compose(Function<T, R2> mappingFunction) {
        if (succeeded()) {
            return mappingFunction.apply(getContent());
        } else {
            return newInstance(null, getFailure());
        }
    }
}
