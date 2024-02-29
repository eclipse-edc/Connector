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

package org.eclipse.edc.connector.api.management.policy.validation;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.jsonobject.validators.MandatoryObject;
import org.eclipse.edc.validator.jsonobject.validators.OptionalIdNotBlank;
import org.eclipse.edc.validator.jsonobject.validators.TypeIs;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.connector.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_POLICY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ACTION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSEQUENCE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_CONSTRAINT_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_DUTY_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LEFT_OPERAND_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_LOGICAL_CONSTRAINT_TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OPERATOR_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_REMEDY_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_RIGHT_OPERAND_ATTRIBUTE;

public class PolicyDefinitionValidator {
    public static Validator<JsonObject> instance() {
        return JsonObjectValidator.newValidator()
                .verifyId(OptionalIdNotBlank::new)
                .verify(EDC_POLICY_DEFINITION_POLICY, MandatoryObject::new)
                .verifyObject(EDC_POLICY_DEFINITION_POLICY, PolicyValidator::instance)
                .build();
    }

    private static class PolicyValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(path -> new TypeIs(path, ODRL_POLICY_TYPE_SET))
                    .verifyArrayItem(ODRL_PERMISSION_ATTRIBUTE, PermissionValidator::instance)
                    .verifyArrayItem(ODRL_OBLIGATION_ATTRIBUTE, DutyValidator::instance)
                    .verifyArrayItem(ODRL_PROHIBITION_ATTRIBUTE, ProhibitionValidator::instance);
        }

    }

    private static class PermissionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {

            return builder
                    .verify(ActionValidator::new)
                    .verifyArrayItem(ODRL_DUTY_ATTRIBUTE, DutyValidator::instance)
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, ConstraintValidatorWrapper::instance);
        }

    }

    private static class DutyValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {

            return builder
                    .verify(ActionValidator::new)
                    .verifyArrayItem(ODRL_CONSEQUENCE_ATTRIBUTE, ConsequenceValidator::instance)
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, ConstraintValidatorWrapper::instance);
        }

    }

    private static class ProhibitionValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {

            return builder
                    .verify(ActionValidator::new)
                    .verifyArrayItem(ODRL_REMEDY_ATTRIBUTE, DutyValidator::instance)
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, ConstraintValidatorWrapper::instance);
        }

    }

    private static class ConsequenceValidator {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {

            return builder
                    .verify(ActionValidator::new)
                    .verifyArrayItem(ODRL_CONSTRAINT_ATTRIBUTE, ConstraintValidatorWrapper::instance);
        }

    }

    private record ActionValidator(JsonLdPath path) implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            return Optional.of(input.containsKey(ODRL_ACTION_ATTRIBUTE))
                    .filter(it -> input.get(ODRL_ACTION_ATTRIBUTE) != null)
                    .map(it -> ValidationResult.success())
                    .orElse(ValidationResult.failure(Violation.violation(format("%s is mandatory but missing or null", path.append(ODRL_ACTION_ATTRIBUTE)), ODRL_ACTION_ATTRIBUTE)));
        }

    }

    private static class ConstraintValidatorWrapper {
        public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
            return builder
                    .verify(ConstraintValidator::new);
        }

    }

    private record ConstraintValidator(JsonLdPath path) implements Validator<JsonObject> {
        @Override
        public ValidationResult validate(JsonObject input) {
            var types = Optional.of(input)
                    .map(it -> it.getJsonArray(TYPE))
                    .stream().flatMap(Collection::stream)
                    .filter(it -> it.getValueType() == JsonValue.ValueType.STRING)
                    .map(JsonString.class::cast)
                    .map(JsonString::getString)
                    .toList();

            if (types.contains(ODRL_LOGICAL_CONSTRAINT_TYPE)) {
                return ValidationResult.success();
            }

            var violations = Stream.of(ODRL_LEFT_OPERAND_ATTRIBUTE, ODRL_OPERATOR_ATTRIBUTE, ODRL_RIGHT_OPERAND_ATTRIBUTE)
                    .map(it -> {
                        var jsonValue = input.get(it);
                        if (jsonValue == null) {
                            return Violation.violation(format("%s is mandatory but missing or null", path.append(it)), it);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (violations.isEmpty()) {
                return ValidationResult.success();
            }
            return ValidationResult.failure(violations);
        }

    }

}
