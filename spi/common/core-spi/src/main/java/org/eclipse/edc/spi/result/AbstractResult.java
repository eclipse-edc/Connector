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
 * @param <T> The type of the content
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
    public AbstractResult<T, F, R> onSuccess(Consumer<T> successAction) {
        if (succeeded()) {
            successAction.accept(getContent());
        }
        return this;
    }

    /**
     * Executes a {@link Consumer} if this {@link Result} failed. Passes the {@link Failure} to the consumer
     */
    public AbstractResult<T, F, R> onFailure(Consumer<F> failureAction) {
        if (failed()) {
            failureAction.accept(getFailure());
        }
        return this;
    }

    /**
     * Alias for {@link AbstractResult#onFailure(Consumer)} to make code a bit more easily readable.
     */
    public AbstractResult<T, F, R> orElse(Consumer<F> failureAction) {
        return onFailure(failureAction);
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
     * Maps one result into another, applying the mapping function.
     *
     * @param mappingFunction a function converting this result into another
     * @return the result of the mapping function
     */
    public <T2, F2 extends Failure, R2 extends AbstractResult<T2, F2, R2>> R2 flatMap(Function<R, R2> mappingFunction) {
        return mappingFunction.apply(self());
    }

    public R self() {
        return (R) this;
    }

}
