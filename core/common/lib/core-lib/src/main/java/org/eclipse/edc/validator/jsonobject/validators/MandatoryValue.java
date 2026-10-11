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
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Verifies that the first item has a non-blank string @value.
 */
public class MandatoryValue implements Validator<JsonObject> {
    private final JsonLdPath path;

    public MandatoryValue(JsonLdPath path) {
        this.path = path;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return Optional.ofNullable(input.getJsonArray(path.last()))
                .filter(it -> !it.isEmpty())
                .map(it -> it.get(0))
                .filter(JsonObject.class::isInstance)
                .map(JsonValue::asJsonObject)
                .map(it -> it.get(VALUE))
                .filter(JsonString.class::isInstance)
                .map(it -> ((JsonString) it).getString())
                .filter(it -> !it.isBlank())
                .map(it -> ValidationResult.success())
                .orElseGet(() -> ValidationResult.failure(violation(format("mandatory value '%s' is missing or it is blank", path), path.toString())));
    }
}
