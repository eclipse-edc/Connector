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
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryArray;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryValue;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_LEFT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERAND_RIGHT;
import static org.eclipse.edc.spi.query.Criterion.CRITERION_OPERATOR;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class CriterionValidator {

    public static Validator<JsonObject> instance(CriterionOperatorRegistry criterionOperatorRegistry) {
        return instance(JsonObjectValidator.newValidator(), criterionOperatorRegistry).build();
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder, CriterionOperatorRegistry criterionOperatorRegistry) {
        return builder
                .verify(CRITERION_OPERAND_LEFT, MandatoryValue::new)
                .verify(CRITERION_OPERATOR, MandatoryValue::new)
                .verify(CRITERION_OPERATOR, path -> new OperatorValidator(path, criterionOperatorRegistry))
                .verify(CRITERION_OPERAND_RIGHT, MandatoryArray.min(1))
                .verify(OperandRightValidator::new);
    }

    private record OperandRightValidator(JsonLdPath path) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var operator = getOperator(input);

            if (operator == null || "in".equals(operator)) {
                return ValidationResult.success();
            }

            return Optional.ofNullable(input.getJsonArray(CRITERION_OPERAND_RIGHT))
                    .filter(it -> it.size() < 2)
                    .map(it -> ValidationResult.success())
                    .orElse(ValidationResult.failure(violation(format("%s cannot contain multiple values as the operator is not 'in'", path.append(CRITERION_OPERAND_RIGHT)), CRITERION_OPERAND_RIGHT)));
        }
    }

    private record OperatorValidator(JsonLdPath path, CriterionOperatorRegistry criterionOperatorRegistry) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var operator = getOperator(input);

            if (operator == null || criterionOperatorRegistry.isSupported(operator)) {
                return ValidationResult.success();
            } else {
                return ValidationResult.failure(violation("Operator %s is not supported".formatted(operator), path.toString(), operator));
            }
        }
    }

    @Nullable
    private static String getOperator(JsonObject input) {
        return Optional.ofNullable(input.getJsonArray(CRITERION_OPERATOR))
                .map(it -> it.getJsonObject(0))
                .map(it -> it.getString(VALUE))
                .orElse(null);
    }
}
