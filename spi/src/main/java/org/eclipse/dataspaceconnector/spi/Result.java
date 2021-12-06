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

    public static <T> Result<T> failure(String failure) {
        return new Result<>(null, List.of(failure));
    }

    public static <T> Result<T> failure(List<String> failures) {
        return new Result<>(null, failures);
    }

    private final T content;
    private final List<String> failures;

    private Result(T content, @NotNull List<String> failures) {
        this.content = content;
        this.failures = failures;
    }

    public T getContent() {
        return content;
    }

    public boolean succeeded() {
        return failures.isEmpty();
    }

    public boolean failed() {
        return !failures.isEmpty();
    }

    public String getFailure() {
        return failures.stream().findFirst().orElseThrow(() -> new EdcException("This result is successful"));
    }

    public List<String> getFailures() {
        return failures;
    }
}
