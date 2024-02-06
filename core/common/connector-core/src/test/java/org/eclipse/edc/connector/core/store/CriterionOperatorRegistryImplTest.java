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

package org.eclipse.edc.connector.core.store;

import org.eclipse.edc.spi.query.OperatorPredicate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CriterionOperatorRegistryImplTest {

    private final CriterionOperatorRegistryImpl registry = new CriterionOperatorRegistryImpl();

    @Nested
    class IsSupported {
        @Test
        void shouldReturnFalse_whenOperatorIsNotRegistered() {
            var result = registry.isSupported("any");

            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnTrue_whenOperatorIsRegistered() {
            registry.registerOperatorPredicate("operator", mock());

            var result = registry.isSupported("operator");

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnTrue_whenOperatorIsRegisteredWithDifferentCase() {
            registry.registerOperatorPredicate("OpERaTOr", mock());

            var result = registry.isSupported("OPERATOR");

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenOperatorHasBeenUnregistered() {
            registry.registerOperatorPredicate("operator", mock());
            registry.unregister("OPERATOR");

            var result = registry.isSupported("OPERATOR");

            assertThat(result).isFalse();
        }

    }

    @Nested
    class Convert {
        @Test
        void shouldThrowException_whenOperatorIsNotRegistered() {
            assertThatThrownBy(() -> registry.toPredicate(criterion("any", "operator", "any")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldConvertUsingTheRegisteredConverter() {
            OperatorPredicate predicate = mock();
            when(predicate.test(any(), any())).thenReturn(true);
            registry.registerOperatorPredicate("operator", predicate);
            registry.registerPropertyLookup((key, object) -> "propertyValue");
            var criterion = criterion("any", "operator", "operandRight");

            var result = registry.toPredicate(criterion).test("any");

            assertThat(result).isTrue();
            verify(predicate).test("propertyValue", "operandRight");
        }

        @Test
        void shouldIgnoreOperatorCase() {
            OperatorPredicate predicate = mock();
            when(predicate.test(any(), any())).thenReturn(true);
            registry.registerOperatorPredicate("OPerATOr", predicate);
            registry.registerPropertyLookup((key, object) -> "propertyValue");
            var criterion = criterion("any", "OPERATOR", "any");

            var result = registry.toPredicate(criterion);

            assertThat(result.test("any")).isTrue();
            verify(predicate).test(any(), any());
        }

        @Test
        void shouldReturnAlwaysFalsePredicate_whenPropertyCannotBeFound() {
            OperatorPredicate predicate = mock();
            when(predicate.test(any(), any())).thenReturn(true);
            registry.registerOperatorPredicate("operator", predicate);
            registry.registerPropertyLookup((key, object) -> null);
            var criterion = criterion("any", "operator", "operandRight");

            var result = registry.toPredicate(criterion).test("any");

            assertThat(result).isFalse();
            verifyNoInteractions(predicate);
        }
    }

    @Nested
    class RegisterPropertyLookup {

        @Test
        void shouldUseLatestPropertyLookup() {
            registry.registerPropertyLookup((key, object) -> "firstOne");
            registry.registerPropertyLookup((key, object) -> "secondOne");
            OperatorPredicate operatorPredicate = mock();
            registry.registerOperatorPredicate("=", operatorPredicate);

            registry.toPredicate(criterion("any", "=", "value")).test("any");

            verify(operatorPredicate).test("secondOne", "value");
        }

        @Test
        void shouldUseLatestPropertyLookupThatDidNotReturnNull() {
            registry.registerPropertyLookup((key, object) -> "firstOne");
            registry.registerPropertyLookup((key, object) -> null);
            OperatorPredicate operatorPredicate = mock();
            registry.registerOperatorPredicate("=", operatorPredicate);

            registry.toPredicate(criterion("any", "=", "value")).test("any");

            verify(operatorPredicate).test("firstOne", "value");
        }
    }

}
