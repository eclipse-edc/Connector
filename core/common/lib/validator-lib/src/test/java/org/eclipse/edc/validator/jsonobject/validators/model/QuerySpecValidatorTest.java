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

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_FILTER_EXPRESSION;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_LIMIT;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_OFFSET;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_SORT_FIELD;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_SORT_ORDER;
import static org.mockito.Mockito.mock;

class QuerySpecValidatorTest {

    private final Validator<JsonObject> validator = QuerySpecValidator.instance(mock());

    @Test
    void shouldSucceed_whenObjectIsValid() {
        var input = Json.createObjectBuilder()
                .build();

        var result = validator.validate(input);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenOffsetIsNegative() {
        var input = Json.createObjectBuilder()
                .add(EDC_QUERY_SPEC_OFFSET, value(-1))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .filteredOn(v -> v.path().equals(EDC_QUERY_SPEC_OFFSET))
                .hasSize(1)
                .first()
                .extracting(Violation::message)
                .asString().contains("greater");
    }

    @Test
    void shouldFail_whenLimitIsLessThanOne() {
        var input = Json.createObjectBuilder()
                .add(EDC_QUERY_SPEC_LIMIT, value(0))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .filteredOn(v -> v.path().equals(EDC_QUERY_SPEC_LIMIT))
                .hasSize(1)
                .first()
                .extracting(Violation::message)
                .asString().contains("greater");
    }

    @Test
    void shouldFail_whenSortOrderNotAscOrDesc() {
        var input = Json.createObjectBuilder()
                .add(EDC_QUERY_SPEC_SORT_ORDER, value("any"))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .filteredOn(v -> v.path().equals(EDC_QUERY_SPEC_SORT_ORDER))
                .hasSize(1)
                .first()
                .extracting(Violation::message)
                .asString().contains("one of");
    }

    @Test
    void shouldFail_whenSortFieldIsBlank() {
        var input = Json.createObjectBuilder()
                .add(EDC_QUERY_SPEC_SORT_FIELD, value(" "))
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .filteredOn(v -> v.path().equals(EDC_QUERY_SPEC_SORT_FIELD))
                .hasSize(1)
                .first()
                .extracting(Violation::message)
                .asString().contains("blank");
    }

    @Test
    void shouldFail_whenFilterExpressionEntryNotValid() {
        var input = Json.createObjectBuilder()
                .add(EDC_QUERY_SPEC_FILTER_EXPRESSION, createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(Criterion.CRITERION_OPERAND_LEFT, value("key"))
                                .add(Criterion.CRITERION_OPERATOR, value("="))
                                .add(Criterion.CRITERION_OPERAND_RIGHT, value("valid criterion"))
                        )
                        .add(createObjectBuilder()
                                .add(Criterion.CRITERION_OPERATOR, value("="))
                                .add(Criterion.CRITERION_OPERAND_RIGHT, value("invalid criterion"))
                        )
                )
                .build();

        var result = validator.validate(input);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations).asInstanceOf(list(Violation.class))
                .filteredOn(v -> v.path().startsWith(EDC_QUERY_SPEC_FILTER_EXPRESSION))
                .hasSizeGreaterThan(0);
    }

    private JsonArrayBuilder value(int value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }

    private JsonArrayBuilder value(String value) {
        return createArrayBuilder().add(createObjectBuilder().add(VALUE, value));
    }
}
