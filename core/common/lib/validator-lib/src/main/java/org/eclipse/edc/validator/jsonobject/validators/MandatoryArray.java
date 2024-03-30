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

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Verifies that an array is present with optional constraint on min size.
 */
public class MandatoryArray implements Validator<JsonObject> {
    private final JsonLdPath path;
    private final Integer min;

    public MandatoryArray(JsonLdPath path) {
        this(path, null);
    }

    public MandatoryArray(JsonLdPath path, Integer min) {
        this.path = path;
        this.min = min;
    }

    public static Function<JsonLdPath, Validator<JsonObject>> min(Integer min) {
        return path -> new MandatoryArray(path, min);
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        return Optional.ofNullable(input.getJsonArray(path.last()))
                .map(this::validateMin)
                .orElseGet(() -> ValidationResult.failure(violation(format("mandatory array '%s' is missing", path), path.toString())));
    }

    private ValidationResult validateMin(JsonArray array) {
        if (min == null || (array.size() >= min)) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(violation(format("array '%s' should at least contains '%s' elements", path, min), path.toString()));
    }

}
