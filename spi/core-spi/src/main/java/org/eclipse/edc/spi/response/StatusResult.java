/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.spi.response;

import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;


public class StatusResult<T> extends AbstractResult<T, ResponseFailure, StatusResult<T>> {

    private StatusResult(T content, ResponseFailure failure) {
        super(content, failure);
    }

    public static StatusResult<Void> success() {
        return new StatusResult<>(null, null);
    }

    public static <T> StatusResult<T> success(T content) {
        return new StatusResult<>(content, null);
    }

    public static <T> StatusResult<T> failure(ResponseStatus status) {
        return new StatusResult<>(null, new ResponseFailure(status, Collections.emptyList()));
    }

    public static <T> StatusResult<T> failure(ResponseStatus status, String error) {
        return new StatusResult<>(null, new ResponseFailure(status, List.of(error)));
    }

    public static <T> StatusResult<T> fatalError(String error) {
        return failure(FATAL_ERROR, error);
    }

    public boolean fatalError() {
        return failed() && getFailure().status() == FATAL_ERROR;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    protected <R1 extends AbstractResult<C1, ResponseFailure, R1>, C1> R1 newInstance(@Nullable C1 content, @Nullable ResponseFailure failure) {
        return (R1) new StatusResult<>(content, failure);
    }

    /**
     * Merges this result with another one. If both Results are successful, a new one is created with no content. If
     * either this result or {@code other} is a failure, the merged result will be a {@code failure()} and will contain
     * all failure messages, no content. If both results are failures, the merged result will contain failure messages
     * of {@code this} result, then the failure messages of {@code other}.
     */
    public <C> StatusResult<C> merge(StatusResult<C> other) {
        if (succeeded() && other.succeeded()) {
            return newInstance(null, null);
        } else {
            var messages = new ArrayList<String>();
            messages.addAll(Optional.ofNullable(getFailure()).map(Failure::getMessages).orElse(Collections.emptyList()));
            messages.addAll(Optional.ofNullable(other.getFailure()).map(Failure::getMessages).orElse(Collections.emptyList()));
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, String.join(",", messages));
        }
    }

    public static <T> StatusResult<T> from(StoreResult<T> storeResult) {
        if (storeResult.succeeded()) {
            return success(storeResult.getContent());
        }

        if (storeResult.reason() == StoreFailure.Reason.ALREADY_LEASED) {
            return StatusResult.failure(ERROR_RETRY, storeResult.getFailureDetail());
        } else {
            return StatusResult.failure(FATAL_ERROR, storeResult.getFailureDetail());
        }
    }
}
