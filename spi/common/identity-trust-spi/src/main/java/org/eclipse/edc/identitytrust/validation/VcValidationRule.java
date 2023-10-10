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

package org.eclipse.edc.identitytrust.validation;

import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.spi.result.Result;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface VcValidationRule extends Function<VerifiableCredential, Result<Void>> {
    default VcValidationRule and(VcValidationRule other) {
        return t -> {
            var thisResult = this.apply(t);
            var otherResult = other.apply(t);

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
