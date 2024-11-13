/*
 *  Copyright (c) 2024 Cofinity-X
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
import static org.junit.jupiter.params.provider.Arguments.arguments;

class LessThanOperatorPredicateTest {

    private final OperatorPredicate predicate = new LessThanOperatorPredicate();

    @ParameterizedTest
    @ArgumentsSource(ValidValues.class)
    void shouldReturnTrue_whenOperandAreNumbers(Object value, Object comparedTo) {
        assertThat(predicate.test(value, comparedTo)).isTrue();
    }

    private static class ValidValues implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(1, 2),
                    arguments(1, 2L),
                    arguments(1, 1.01f),
                    arguments(1, 1.01d),
                    arguments("a", "b")
            );
        }
    }
}
