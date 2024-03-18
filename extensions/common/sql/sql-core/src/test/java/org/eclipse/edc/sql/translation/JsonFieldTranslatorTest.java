/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.types.PathItem;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class JsonFieldTranslatorTest {

    private final JsonFieldTranslator translator = new JsonFieldTranslator("column_name");

    @Nested
    class GetLeftOperand {

        @Test
        void shouldReturnLeftOperand_whenPathHasSingleEntry() {
            var result = translator.getLeftOperand(PathItem.parse("field"), Object.class);

            assertThat(result).isEqualTo("column_name ->> 'field'");
        }

        @Test
        void shouldReturnLeftOperand_whenPathHasMultipleEntries() {
            var result = translator.getLeftOperand(PathItem.parse("nested.field"), Object.class);

            assertThat(result).isEqualTo("column_name -> 'nested' ->> 'field'");
        }

        @Test
        void shouldParseLeftOperand_whenRightOperandIsBoolean() {
            var result = translator.getLeftOperand(PathItem.parse("field"), Boolean.class);

            assertThat(result).isEqualTo("(column_name ->> 'field')::boolean");
        }
    }

    @Nested
    class ToWhereClause {

        @Test
        void shouldReturnWhereClause_whenPathHasSingleEntry() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("json.field", "=", "value");

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo("column_name ->> 'field' = ?");
            assertThat(result.parameters()).containsExactly("value");
        }

        @Test
        void shouldReturnWhereClause_whenPathHasMultipleEntries() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("json.nested.field", "=", "value");

            var result = translator.toWhereClause(PathItem.parse("nested.field"), criterion, operator);

            assertThat(result.sql()).isEqualTo("column_name -> 'nested' ->> 'field' = ?");
            assertThat(result.parameters()).containsExactly("value");
        }

        @Test
        void shouldParseWhereClause_whenRightOperandIsBoolean() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("json.field", "=", true);

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo("(column_name ->> 'field')::boolean = ?");
            assertThat(result.parameters()).containsExactly(true);
        }

        @Test
        void shouldConvertToJsonB_whenOperatorIsContains() {
            var operator = new SqlOperator("??", Object.class);
            var criterion = criterion("json.array", "contains", "value");

            var result = translator.toWhereClause(PathItem.parse("array"), criterion, operator);

            assertThat(result.sql()).isEqualTo("(column_name -> 'array')::jsonb ?? ?");
            assertThat(result.parameters()).containsExactly("value");
        }

    }

}
