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

import org.eclipse.edc.util.reflection.PathItem;
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
        void shouldParseWhereClause_whenRightOperandIsInteger() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("json.field", "=", 100);

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo("(column_name ->> 'field')::integer = ?");
            assertThat(result.parameters()).containsExactly(100);
        }

        @Test
        void shouldParseWhereClause_whenRightOperandIsDouble() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("json.field", "=", 100.0);

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo("(column_name ->> 'field')::double = ?");
            assertThat(result.parameters()).containsExactly(100.0);
        }

        @Test
        void shouldParseWhereClause_whenRightOperandIsFloat() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("json.field", "=", 100.0F);

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo("(column_name ->> 'field')::float = ?");
            assertThat(result.parameters()).containsExactly(100.0F);
        }

        @Test
        void shouldParseWhereClause_whenRightOperandIsLong() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("json.field", "=", 100L);

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo("(column_name ->> 'field')::long = ?");
            assertThat(result.parameters()).containsExactly(100L);
        }

        @Test
        void shouldParseWhereClause_whenRightOperandIsByte() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("json.field", "=", (byte) 1);

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo("(column_name ->> 'field')::byte = ?");
            assertThat(result.parameters()).containsExactly((byte) 1);
        }

        @Test
        void shouldParseWhereClause_whenRightOperandIsShort() {
            var operator = new SqlOperator("=", Object.class);
            var criterion = criterion("json.field", "=", (short) 1);

            var result = translator.toWhereClause(PathItem.parse("field"), criterion, operator);

            assertThat(result.sql()).isEqualTo("(column_name ->> 'field')::short = ?");
            assertThat(result.parameters()).containsExactly((short) 1);
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
