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

package org.eclipse.edc.web.spi.exception;

import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Violation;

import java.util.List;

public class ValidationFailureException extends EdcApiException {
    private final List<Violation> violations;

    public ValidationFailureException(ValidationFailure failure) {
        this(failure.getViolations());
    }

    public ValidationFailureException(List<Violation> violations) {
        super(violations.stream().map(Violation::message).toList());
        this.violations = violations;
    }

    @Override
    public String getType() {
        return "ValidationFailure";
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
