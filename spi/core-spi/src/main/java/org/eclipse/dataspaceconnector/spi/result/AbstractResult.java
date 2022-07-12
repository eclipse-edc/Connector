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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Base result type used by services to indicate success or failure.
 *
 * Service operations should generally never throw checked exceptions. Instead, they should return concrete result types and raise unchecked exceptions only when an
 * unexpected event happens, such as a programming error.
 */
public abstract class AbstractResult<T, F extends Failure> {

    private final T content;
    private final F failure;

    protected AbstractResult(T content, F failure) {
        this.content = content;
        this.failure = failure;
    }

    @NotNull
    public T getContent() {
        return content;
    }

    public F getFailure() {
        return failure;
    }

    //will cause problems during JSON serialization if failure is null
    @JsonIgnore
    public List<String> getFailureMessages() {
        Objects.requireNonNull(failure);
        return failure.getMessages();
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
    @JsonIgnore // will cause problems during JSON serialization if failure is null
    public String getFailureDetail() {
        return String.join(", ", getFailureMessages());
    }
}
