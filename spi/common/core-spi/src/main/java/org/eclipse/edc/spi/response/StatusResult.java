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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

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

    public boolean fatalError() {
        return failed() && getFailure().status() == FATAL_ERROR;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    @Override
    protected <R1 extends AbstractResult<C1, ResponseFailure, R1>, C1> R1 newInstance(@Nullable C1 content, @Nullable ResponseFailure failure) {
        return (R1) new StatusResult<>(content, failure);
    }
}
