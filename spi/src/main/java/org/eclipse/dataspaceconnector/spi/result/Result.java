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

import java.util.List;

public class Result<T> extends AbstractResult<T, GenericFailure> {

    public static <T> Result<T> success(@NotNull T content) {
        return new Result<>(content, null);
    }

    public static <T> Result<T> failure(String failure) {
        return new Result<>(null, new GenericFailure(List.of(failure)));
    }

    public static <T> Result<T> failure(List<String> failures) {
        return new Result<>(null, new GenericFailure(failures));
    }

    private Result(T content, GenericFailure failure) {
        super(content, failure);
    }

}

