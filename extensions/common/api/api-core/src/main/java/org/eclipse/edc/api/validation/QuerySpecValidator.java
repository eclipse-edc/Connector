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

package org.eclipse.edc.api.validation;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.validator.jsonobject.JsonLdPath;
import org.eclipse.edc.validator.jsonobject.JsonObjectValidator;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Arrays;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_FILTER_EXPRESSION;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_LIMIT;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_OFFSET;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_SORT_FIELD;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_SORT_ORDER;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class QuerySpecValidator {

    public static Validator<JsonObject> instance() {
        return instance(JsonObjectValidator.newValidator()).build();
    }

    public static JsonObjectValidator.Builder instance(JsonObjectValidator.Builder builder) {
        return builder
                .verify(EDC_QUERY_SPEC_OFFSET, OptionalValueGreaterEqualZero::new)
                .verify(EDC_QUERY_SPEC_LIMIT, OptionalValueGreaterZero::new)
                .verify(EDC_QUERY_SPEC_SORT_ORDER, OptionalValueSortField::new)
                .verify(EDC_QUERY_SPEC_SORT_FIELD, OptionalValueNotBlank::new)
                .verifyObject(EDC_QUERY_SPEC_FILTER_EXPRESSION, CriterionValidator::instance);
    }

    private record OptionalValueGreaterEqualZero(JsonLdPath path) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var value = Optional.ofNullable(input.getJsonArray(path.last()))
                    .map(it -> it.getJsonObject(0))
                    .map(it -> it.getInt(VALUE))
                    .orElse(0);

            if (value < 0) {
                return ValidationResult.failure(violation(format("optional value '%s' must be greater or equal to zero", path), path.toString(), value));
            }

            return ValidationResult.success();
        }
    }

    private record OptionalValueGreaterZero(JsonLdPath path) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var value = Optional.ofNullable(input.getJsonArray(path.last()))
                    .map(it -> it.getJsonObject(0))
                    .map(it -> it.getInt(VALUE))
                    .orElse(1);

            if (value < 1) {
                return ValidationResult.failure(violation(format("optional value '%s' must be greater than zero", path), path.toString(), value));
            }

            return ValidationResult.success();
        }
    }

    private record OptionalValueSortField(JsonLdPath path) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var values = Optional.ofNullable(input.getJsonArray(path.last()))
                    .map(array -> array.stream().map(it -> it.asJsonObject().getString(VALUE)).toList())
                    .orElse(emptyList());

            if (values.size() > 0 && !Arrays.stream(SortOrder.values()).map(Enum::name).toList().contains(values.get(0))) {
                var message = format("optional value '%s' must be one of %s", path, Arrays.toString(SortOrder.values()));
                return ValidationResult.failure(violation(message, path.toString(), values.get(0)));
            }

            return ValidationResult.success();
        }
    }

    private record OptionalValueNotBlank(JsonLdPath path) implements Validator<JsonObject> {

        @Override
        public ValidationResult validate(JsonObject input) {
            var optional = Optional.ofNullable(input.getJsonArray(path.last()))
                    .map(it -> it.getJsonObject(0))
                    .map(it -> it.getString(VALUE));

            if (optional.isEmpty()) {
                return ValidationResult.success();
            }

            return optional
                    .filter(it -> !it.isBlank())
                    .map(it -> ValidationResult.success())
                    .orElseGet(() -> ValidationResult.failure(violation(format("optional value '%s' is blank", path), path.toString())));
        }
    }
}
