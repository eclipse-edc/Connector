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

package org.eclipse.edc.validator.jsonobject;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Verify that the @id field is not blank. It can be null, though.
 */
public class OptionalIdNotBlank implements Validator<JsonObject> {

    private final JsonLdPath path;

    public OptionalIdNotBlank(JsonLdPath path) {
        this.path = path;
    }

    @Override
    public ValidationResult validate(JsonObject input) {
        if (input.containsKey(ID) && input.getString(ID).isBlank()) {
            return ValidationResult.failure(violation(format("%s optional %s cannot be blank", path, ID), path.toString()));
        } else {
            return ValidationResult.success();
        }
    }
}
