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

package org.eclipse.dataspaceconnector.spi.result;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Base result type used by services to indicate success or failure.
 * <p>
 * Service operations should generally never throw checked exceptions. Instead, they should return concrete result types
 * and raise unchecked exceptions only when an unexpected event happens, such as a programming error.
 *
 * @param <F> The type of {@link Failure}.
 * @param <T> The type of the content
 */
public abstract class AbstractResult<T, F extends Failure> {

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
        return failure == null ? null : String.join(", ", getFailureMessages());
    }
}
