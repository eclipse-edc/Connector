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

package org.eclipse.edc.sql.translation;

import org.eclipse.edc.spi.query.Criterion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriterionToWhereClauseConverterImplTest {

    private final CriterionToWhereClauseConverter converter = new CriterionToWhereClauseConverterImpl(new TestMapping(), true);

    @Test
    void singleExpression_equalsOperator() {
        var criterion = new Criterion("field1", "=", "testid1");

        var condition = converter.convert(criterion);

        assertThat(condition.sql()).isEqualToIgnoringCase("edc_field_1 = ?");
        assertThat(condition.parameters()).containsExactly("testid1");
    }

    @Test
    void singleExpression_notExistentColumn() {
        var criterion = new Criterion("not-existent", "=", "testid1");

        var condition = converter.convert(criterion);

        assertThat(condition.sql()).isEqualToIgnoringCase("0 = ?");
        assertThat(condition.parameters()).containsExactly(1);
    }

    @Test
    void singleExpression_inOperator() {
        var criterion = new Criterion("field1", "in", List.of("id1", "id2", "id3"));

        var condition = converter.convert(criterion);

        assertThat(condition.sql()).isEqualToIgnoringCase("edc_field_1 IN (?,?,?)");
        assertThat(condition.parameters()).containsExactly("id1", "id2", "id3");
    }

    @ParameterizedTest
    @ArgumentsSource(ValidArgs.class)
    void isValidExpression_whenValid(String left, String op, Object right) {
        var criterion = new Criterion(left, op, right);

        assertThatNoException().isThrownBy(() -> converter.convert(criterion));
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidArgs.class)
    void isValidExpression_whenInvalid(String left, String op, Object right) {
        var criterion = new Criterion(left, op, right);

        assertThatThrownBy(() -> converter.convert(criterion)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStatementParameter_keepsValueType() {
        var criterion = new Criterion("field1", "=", 3);

        var condition = converter.convert(criterion);

        assertThat(condition.parameters()).containsExactly(3);
    }

    private static class ValidArgs implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of("description", "=", List.of("foo")),
                    Arguments.of("description", "=", 123),
                    Arguments.of("description", "=", "other"),
                    Arguments.of("description", "like", "other"),
                    Arguments.of("description", "like", "%other"),
                    Arguments.of("description", "like", List.of()),
                    Arguments.of("description", "like", ""),
                    Arguments.of("description", "like", null),
                    Arguments.of("description", "in", List.of("first", "second")),
                    Arguments.of("description", "in", List.of("first")),
                    Arguments.of("description", "in", List.of())
            );
        }
    }

    private static class InvalidArgs implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return Stream.of(
                    Arguments.of("field1", "contains", "value"),
                    Arguments.of("field1", "in", "(item1, item2)"),
                    Arguments.of("field1", "in", "list"),
                    Arguments.of("field1", "in", null)
            );
        }
    }
}
