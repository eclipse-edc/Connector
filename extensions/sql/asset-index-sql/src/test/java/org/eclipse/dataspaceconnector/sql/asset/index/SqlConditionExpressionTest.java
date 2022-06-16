/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.asset.index;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.sql.translation.SqlConditionExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SqlConditionExpressionTest {


    public static Stream<Arguments> validArgs() {
        return Stream.of(
                Arguments.of("something", "=", List.of("foo")),
                Arguments.of("something", "=", 123),
                Arguments.of("something", "=", "other"),
                Arguments.of("something", "like", "other"),
                Arguments.of("something", "like", "%other"),
                Arguments.of("something", "like", List.of()),
                Arguments.of("something", "like", ""),
                Arguments.of("something", "like", null),
                Arguments.of("something", "in", List.of("first", "second")),
                Arguments.of("something", "in", List.of("first")),
                Arguments.of("something", "in", List.of())
        );
    }

    public static Stream<Arguments> invalidArgs() {
        return Stream.of(
                Arguments.of("something", "contains", "value"),
                Arguments.of("something", "in", "(item1, item2)"),
                Arguments.of("something", "in", "list"),
                Arguments.of("something", "in", null)
        );
    }

    @ParameterizedTest
    @MethodSource("validArgs")
    void isValidExpression_whenValid(String left, String op, Object right) {
        var e = new SqlConditionExpression(new Criterion(left, op, right));
        assertThat(e.isValidExpression().succeeded()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidArgs")
    void isValidExpression_whenInvalid(String left, String op, Object right) {
        var e = new SqlConditionExpression(new Criterion(left, op, right));
        assertThat(e.isValidExpression().succeeded()).isFalse();
    }

    @Test
    void toValuePlaceholder() {
        var e = new SqlConditionExpression(new Criterion("key", "=", "value"));
        assertThat(e.toValuePlaceholder()).isEqualTo("?");

        var e2 = new SqlConditionExpression(new Criterion("key", "in", List.of("item1", "item2")));
        assertThat(e2.toValuePlaceholder()).matches("\\(\\?,.*\\?\\)");
    }

    @Test
    void toStatementParameter() {
        var e = new SqlConditionExpression(new Criterion("key", "=", "value"));
        assertThat(e.toStatementParameter()).containsExactly("key", "value");

        var e2 = new SqlConditionExpression(new Criterion("key", "in", List.of("item1", "item2")));
        assertThat(e2.toStatementParameter()).containsExactly("key", "item1", "item2");
    }
}