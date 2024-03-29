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

package org.eclipse.edc.validator.jsonobject.validators;

import jakarta.json.JsonString;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static java.lang.String.format;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Verify that the @id field is not null or blank.
 */
public class MandatoryIdNotBlank implements Validator<JsonString> {

    private final JsonLdPath path;

    public MandatoryIdNotBlank(JsonLdPath path) {
        this.path = path;
    }

    @Override
    public ValidationResult validate(JsonString id) {
        if (id == null || id.getString().isBlank()) {
            return ValidationResult.failure(violation(format("%s cannot be null or blank", path), path.toString()));
        } else {
            return ValidationResult.success();
        }
    }
}
