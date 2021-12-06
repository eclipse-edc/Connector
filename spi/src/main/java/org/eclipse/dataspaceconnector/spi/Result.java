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

package org.eclipse.dataspaceconnector.spi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static java.util.Collections.emptyList;

public class Result<T> {

    public static <T> Result<T> success(@NotNull T content) {
        return new Result<>(content, emptyList());
    }

    public static <T> Result<T> failure(String error) {
        return new Result<>(null, List.of(error));
    }

    private final T content;
    private final List<String> errors;

    private Result(T content, @NotNull List<String> errors) {
        this.content = content;
        this.errors = errors;
    }

    public T getContent() {
        return content;
    }

    public boolean succeeded() {
        return errors.isEmpty();
    }

    public boolean failed() {
        return !errors.isEmpty();
    }

    public String getFailure() {
        return errors.stream().findFirst().orElseThrow(() -> new EdcException("This result is successful"));
    }
}
