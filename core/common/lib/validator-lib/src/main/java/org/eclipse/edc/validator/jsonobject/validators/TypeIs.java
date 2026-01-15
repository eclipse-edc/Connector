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

import java.util.Collection;
import java.util.Optional;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Verify that the @type node has a certain value
 */
public class TypeIs implements Validator<JsonObject> {

    private final JsonLdPath path;
    private final String expectedType;

    public TypeIs(JsonLdPath path, String expectedType) {
        this.path = path;
        this.expectedType = expectedType;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        var types = Optional.of(input)
                .map(it -> it.getJsonArray(TYPE))
                .stream().flatMap(Collection::stream)
                .filter(it -> it.getValueType() == JsonValue.ValueType.STRING)
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .toList();


        if (types.contains(expectedType)) {
            return ValidationResult.success();
        } else {
            var newPath = path.append(TYPE);

            var violation = violation(
                    "%s was expected to be %s but it was %s".formatted(newPath, expectedType, types),
                    newPath.toString(), types
            );
            return ValidationResult.failure(violation);
        }
    }
}
