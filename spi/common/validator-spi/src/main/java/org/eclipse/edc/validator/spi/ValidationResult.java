/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.validator.spi;

import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Result class for {@link Validator}
 */
public class ValidationResult extends AbstractResult<Void, ValidationFailure, ValidationResult> {

    public static ValidationResult success() {
        return new ValidationResult(null, null);
    }

    public static ValidationResult failure(Violation violation) {
        return new ValidationResult(null, new ValidationFailure(List.of(violation)));
    }

    public static ValidationResult failure(List<Violation> violations) {
        return new ValidationResult(null, new ValidationFailure(violations));
    }

    protected ValidationResult(Void content, ValidationFailure failure) {
        super(content, failure);
    }

    @SuppressWarnings("unchecked")
    @Override
    @NotNull
    protected <R1 extends AbstractResult<C1, ValidationFailure, R1>, C1> R1 newInstance(@Nullable C1 content, @Nullable ValidationFailure failure) {
        return (R1) new ValidationResult(null, failure);
    }

    public Result<Void> toResult() {
        if (succeeded()) {
            return Result.success();
        } else {
            var messages = getFailure().getViolations().stream()
                    .map(it -> "%s. Path: %s. Illegal value: %s".formatted(it.message(), it.path(), it.value()))
                    .toList();
            return Result.failure(messages);
        }
    }
}
