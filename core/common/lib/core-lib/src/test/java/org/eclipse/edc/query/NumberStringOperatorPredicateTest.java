/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.query;

import org.eclipse.edc.spi.query.OperatorPredicate;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.query.NumberStringOperatorPredicate.equal;
import static org.eclipse.edc.query.NumberStringOperatorPredicate.greaterThan;
import static org.eclipse.edc.query.NumberStringOperatorPredicate.greaterThanEqual;
import static org.eclipse.edc.query.NumberStringOperatorPredicate.lessThan;
import static org.eclipse.edc.query.NumberStringOperatorPredicate.lessThanEqual;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NumberStringOperatorPredicateTest {

    @ParameterizedTest
    @ArgumentsSource(ValidValues.class)
    void shouldReturnTrue_whenComparisonMatches(OperatorPredicate predicate, Object value, Object comparedTo) {
        assertThat(predicate.test(value, comparedTo)).isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidValues.class)
    void shouldReturnFalse_whenComparisonDoesNotMath(OperatorPredicate predicate, Object value, Object comparedTo) {
        assertThat(predicate.test(value, comparedTo)).isFalse();
    }

    private static class ValidValues implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(equal(), 1, 1),
                    arguments(equal(), 1, 1L),
                    arguments(equal(), 1, 1.0f),
                    arguments(equal(), 1, 1.0d),
                    arguments(equal(), "a", "a"),
                    arguments(lessThan(), 1, 2),
                    arguments(lessThan(), 1, 2L),
                    arguments(lessThan(), 1, 1.01f),
                    arguments(lessThan(), 1, 1.01d),
                    arguments(lessThan(), "a", "b"),
                    arguments(lessThanEqual(), 1, 1),
                    arguments(lessThanEqual(), 1, 1L),
                    arguments(lessThanEqual(), 1, 1.0f),
                    arguments(lessThanEqual(), 1, 1.0d),
                    arguments(lessThanEqual(), "a", "a"),
                    arguments(lessThanEqual(), 1, 2),
                    arguments(lessThanEqual(), 1, 2L),
                    arguments(lessThanEqual(), 1, 1.01f),
                    arguments(lessThanEqual(), 1, 1.01d),
                    arguments(lessThanEqual(), "a", "b"),
                    arguments(greaterThan(), 2, 1),
                    arguments(greaterThan(), 2L, 1L),
                    arguments(greaterThan(), 1.01f, 1),
                    arguments(greaterThan(), 1.01d, 1),
                    arguments(greaterThan(), "b", "a"),
                    arguments(greaterThanEqual(), 1, 1),
                    arguments(greaterThanEqual(), 1, 1L),
                    arguments(greaterThanEqual(), 1, 1.0f),
                    arguments(greaterThanEqual(), 1, 1.0d),
                    arguments(greaterThanEqual(), "a", "a"),
                    arguments(greaterThanEqual(), 2, 1),
                    arguments(greaterThanEqual(), 2L, 1L),
                    arguments(greaterThanEqual(), 1.01f, 1),
                    arguments(greaterThanEqual(), 1.01d, 1),
                    arguments(greaterThanEqual(), "b", "a")
            );
        }
    }

    private static class InvalidValues implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(equal(), 1, 2),
                    arguments(equal(), 1, 2L),
                    arguments(equal(), 1, 1.01f),
                    arguments(equal(), 1, 1.01d),
                    arguments(equal(), "a", "b"),
                    arguments(lessThan(), 1, 1),
                    arguments(lessThan(), 1, 1L),
                    arguments(lessThan(), 1, 1.0f),
                    arguments(lessThan(), 1, 1.0d),
                    arguments(lessThan(), "a", "a"),
                    arguments(lessThanEqual(), 1, 0),
                    arguments(lessThanEqual(), 1, 0),
                    arguments(lessThanEqual(), 1, 0.9f),
                    arguments(lessThanEqual(), 1, 0.9d),
                    arguments(lessThanEqual(), "a", "Z"),
                    arguments(greaterThan(), 1, 1),
                    arguments(greaterThan(), 1, 1L),
                    arguments(greaterThan(), 1, 1.0f),
                    arguments(greaterThan(), 1, 1.0d),
                    arguments(greaterThan(), "a", "a"),
                    arguments(greaterThanEqual(), 1, 2),
                    arguments(greaterThanEqual(), 1, 2L),
                    arguments(greaterThanEqual(), 1, 1.01f),
                    arguments(greaterThanEqual(), 1, 1.01d),
                    arguments(greaterThanEqual(), "a", "b")
            );
        }
    }

}
