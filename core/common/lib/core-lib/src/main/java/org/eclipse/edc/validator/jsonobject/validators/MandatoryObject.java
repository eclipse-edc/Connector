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

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Verifies that a single object is present.
 */
public class MandatoryObject implements Validator<JsonObject> {

    private final JsonLdPath path;

    public MandatoryObject(JsonLdPath path) {
        this.path = path;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return Optional.ofNullable(input.getJsonArray(path.last()))
                .filter(it -> it.size() > 0)
                .map(it -> it.getJsonObject(0))
                .map(it -> ValidationResult.success())
                .orElseGet(() -> ValidationResult.failure(violation(format("mandatory object '%s' is missing", path), path.toString())));
    }
}
