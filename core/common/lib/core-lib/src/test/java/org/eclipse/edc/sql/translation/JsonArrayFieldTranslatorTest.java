/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.util.reflection.PathItem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class JsonArrayFieldTranslatorTest {

    private final JsonArrayFieldTranslator translator = new JsonArrayFieldTranslator("contract_offers");

    @Nested
    class GetLeftOperand {

        @Test
        void shouldResolveFieldOnArrayElement_whenPathHasSingleEntry() {
            var result = translator.getLeftOperand(PathItem.parse("field"), Object.class);

            assertThat(result).isEqualTo("element.value ->> 'field'");
        }

        @Test
        void shouldResolveFieldOnArrayElement_whenPathHasMultipleEntries() {
            var result = translator.getLeftOperand(PathItem.parse("nested.field"), Object.class);

            assertThat(result).isEqualTo("element.value -> 'nested' ->> 'field'");
        }
    }

    @Nested
    class ToWhereClause {

        @Test
        void shouldWrapElementPredicateInExists_whenPathHasSingleEntry() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("contractOffers.field", "=", "value");

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo(
                    "EXISTS (SELECT 1 FROM json_array_elements(contract_offers) AS element WHERE element.value ->> 'field' = ?)");
            assertThat(result.parameters()).containsExactly("value");
        }

        @Test
        void shouldWrapElementPredicateInExists_whenPathHasMultipleEntries() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("contractOffers.nested.field", "=", "value");

            var result = translator.toWhereClause(PathItem.parse("nested.field"), criterion, operator);

            assertThat(result.sql()).isEqualTo(
                    "EXISTS (SELECT 1 FROM json_array_elements(contract_offers) AS element WHERE element.value -> 'nested' ->> 'field' = ?)");
            assertThat(result.parameters()).containsExactly("value");
        }

        @Test
        void shouldPassOperatorThrough_whenOperatorIsNotEquals() {
            var operator = new SqlOperator("like", String.class);
            var criterion = criterion("contractOffers.field", "like", "%value%");

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo(
                    "EXISTS (SELECT 1 FROM json_array_elements(contract_offers) AS element WHERE element.value ->> 'field' like ?)");
            assertThat(result.parameters()).containsExactly("%value%");
        }

        @Test
        void shouldCastElementField_whenRightOperandIsInteger() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("contractOffers.field", "=", 100);

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo(
                    "EXISTS (SELECT 1 FROM json_array_elements(contract_offers) AS element WHERE (element.value ->> 'field')::integer = ?)");
            assertThat(result.parameters()).containsExactly(100);
        }
    }

}
