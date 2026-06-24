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

import org.eclipse.edc.spi.result.Failure;

import java.util.List;

/**
 * Failure class for {@link Validator}
 */
public class ValidationFailure extends Failure {

    private final List<Violation> violations;

    public ValidationFailure(List<Violation> violations) {
        super(violations.stream().map(Violation::message).toList());
        this.violations = violations;
    }

    public List<Violation> getViolations() {
        return violations;
    }

}
