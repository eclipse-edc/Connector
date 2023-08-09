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

package org.eclipse.edc.validator.jsonobject.validators.model;

import jakarta.json.JsonObject;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERATOR;

public class CriterionValidator {

    public static Validator<JsonObject> instance() {
        return instance(JsonObjectValidator.newValidator()).build();
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
        return builder
                .verify(CRITERION_OPERAND_LEFT, MandatoryValue::new)
                .verify(CRITERION_OPERATOR, MandatoryValue::new)
                .verify(OperandRightValidator::new);
    }

    private record OperandRightValidator(JsonLdPath path) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var operator = Optional.ofNullable(input.getJsonArray(CRITERION_OPERATOR))
                    .map(it -> it.getJsonObject(0))
                    .map(it -> it.getString(VALUE))
                    .orElse(null);

            if (operator == null || "in".equals(operator)) {
                return ValidationResult.success();
            }

            return Optional.ofNullable(input.getJsonArray(CRITERION_OPERAND_RIGHT))
                    .filter(it -> it.size() == 1)
                    .map(it -> ValidationResult.success())
                    .orElse(ValidationResult.failure(Violation.violation(format("%s cannot contain multiple values as the operator is not 'in'", path.toString()), CRITERION_OPERAND_RIGHT)));
        }
    }
}
