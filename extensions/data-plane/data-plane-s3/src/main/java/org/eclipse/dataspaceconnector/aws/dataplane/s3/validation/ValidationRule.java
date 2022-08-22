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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ValidationRule<T> extends Function<T, Result<Void>> {

    static <T> ValidationRule<T> identity() {
        return t -> Result.success();
    }

    default ValidationRule<T> and(ValidationRule<T> other) {
        return t -> {
            Result<Void> thisResult = this.apply(t);
            Result<Void> otherResult = other.apply(t);

            var thisFailureMessages = thisResult.failed() ? thisResult.getFailureMessages().stream() : Stream.<String>empty();
            var otherFailureMessages = otherResult.failed() ? otherResult.getFailureMessages().stream() : Stream.<String>empty();
            var totalFailureMessages = Stream.concat(thisFailureMessages, otherFailureMessages).collect(Collectors.toList());
            if (totalFailureMessages.isEmpty()) {
                return Result.success();
            } else {
                return Result.failure(totalFailureMessages);
            }
        };
    }
}
