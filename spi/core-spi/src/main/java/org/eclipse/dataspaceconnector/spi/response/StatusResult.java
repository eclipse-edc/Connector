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

package org.eclipse.dataspaceconnector.spi.response;

import org.eclipse.dataspaceconnector.spi.result.AbstractResult;

import java.util.Collections;
import java.util.List;

import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.FATAL_ERROR;


public class StatusResult<T> extends AbstractResult<T, ResponseFailure> {

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

    private StatusResult(T content, ResponseFailure failure) {
        super(content, failure);
    }

    public boolean fatalError() {
        return failed() && getFailure().status() == FATAL_ERROR;
    }
}
