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

package org.eclipse.edc.connector.controlplane.services.query;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class QueryValidatorsTest {

    @Nested
    class PolicyDefinition {

        private final QueryValidator validator = QueryValidators.policyDefinition();

        @Test
        void shouldSucceed_whenQueryIsEmpty() {
            var result = validator.validate(QuerySpec.none());

            assertThat(result).isSucceeded();
        }

        @ParameterizedTest
        @ArgumentsSource(InvalidFilters.class)
        void shouldFail_whenInvalidFilters(Criterion invalidFilter) {
            var query = QuerySpec.Builder.newInstance()
                    .filter(invalidFilter)
                    .build();

            var result = validator.validate(query);

            assertThat(result).isFailed();
        }

        @ParameterizedTest
        @ArgumentsSource(ValidFilters.class)
        void shouldSucceed_whenValidFilters(Criterion validFilter) {
            var query = QuerySpec.Builder.newInstance()
                    .filter(validFilter)
                    .build();

            var result = validator.validate(query);

            assertThat(result).isSucceeded();
        }

        private static class InvalidFilters implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(criterion("policy.permissions.action.constraint.noexist", "=", "123455")), // wrong property
                        arguments(criterion("permissions.action.constraint.leftExpression", "=", "123455")), // missing root
                        arguments(criterion("policy.permissions.action.leftExpression", "=", "123455")) // skips path element
                );
            }
        }

        private static class ValidFilters implements ArgumentsProvider {
            private static final String PRIVATE_PROPERTIES = "privateProperties";
            private static final String EDC_NAMESPACE = "'https://w3id.org/edc/v0.0.1/ns/'";
            private static final String KEY = "key";

            private static final String VALUE = "123455";

            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(criterion(PRIVATE_PROPERTIES, "=", VALUE)), // path element with privateProperties
                        arguments(criterion(PRIVATE_PROPERTIES + "." + KEY, "=", VALUE)), // path element with privateProperties and key
                        arguments(criterion(PRIVATE_PROPERTIES + ".'" + KEY + "'", "=", VALUE)), // path element with privateProperties and 'key'
                        arguments(criterion(PRIVATE_PROPERTIES + "." + EDC_NAMESPACE + KEY, "=", VALUE)) // path element with privateProperties and edc_namespace key
                );
            }
        }
    }

    @Nested
    class TransferProcess {

        private final QueryValidator validator = QueryValidators.transferProcess();

        @Test
        void shouldSucceed_whenQueryIsEmpty() {
            var result = validator.validate(QuerySpec.none());

            assertThat(result).isSucceeded();
        }

        @ParameterizedTest
        @ArgumentsSource(InvalidFilters.class)
        void shouldFail_whenInvalidFilters(Criterion invalidFilter) {
            var query = QuerySpec.Builder.newInstance()
                    .filter(invalidFilter)
                    .build();

            var result = validator.validate(query);

            assertThat(result).isFailed();
        }

        @ParameterizedTest
        @ArgumentsSource(ValidFilters.class)
        void shouldSucceed_whenValidFilters(Criterion validFilter) {
            var query = QuerySpec.Builder.newInstance()
                    .filter(validFilter)
                    .build();

            var result = validator.validate(query);

            assertThat(result).isSucceeded();
        }

        private static class InvalidFilters implements ArgumentsProvider {
            @Override
            @NullMarked
            public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
                return Stream.of(
                        arguments(criterion("contentDataAddress.properties[*].someKey", "=", "someval")) // map types not supported
                );
            }
        }

        private static class ValidFilters implements ArgumentsProvider {
            @Override
            @NullMarked
            public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
                return Stream.of(
                        arguments(criterion("type", "=", "CONSUMER"))
                );
            }
        }

    }

    @Nested
    class ContractDefinition {

        private final QueryValidator validator = QueryValidators.contractDefinition();

        @Test
        void shouldSucceed_whenQueryIsEmpty() {
            var result = validator.validate(QuerySpec.none());

            assertThat(result).isSucceeded();
        }

        @ParameterizedTest
        @ArgumentsSource(InvalidFilters.class)
        void shouldFail_whenInvalidFilters(Criterion invalidFilter) {
            var query = QuerySpec.Builder.newInstance()
                    .filter(invalidFilter)
                    .build();

            var result = validator.validate(query);

            assertThat(result).isFailed();
        }

        @ParameterizedTest
        @ArgumentsSource(ValidFilters.class)
        void shouldSucceed_whenValidFilters(Criterion validFilter) {
            var query = QuerySpec.Builder.newInstance()
                    .filter(validFilter)
                    .build();

            var result = validator.validate(query);

            assertThat(result).isSucceeded();
        }

        private static class InvalidFilters implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(criterion("assetsSelector.leftHand", "=", "foo")), // invalid path
                        arguments(criterion("accessPolicyId'LIKE/**/?/**/LIMIT/**/?/**/OFFSET/**/?;DROP/**/TABLE/**/test/**/--%20", "=", "%20ABC--")) //some SQL injection
                );
            }
        }

        private static class ValidFilters implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(criterion("assetsSelector.operandLeft", "=", "foo")),
                        arguments(criterion("assetsSelector.operator", "=", "LIKE")),
                        arguments(criterion("assetsSelector.operandRight", "=", "bar"))
                );
            }
        }

    }

    @Nested
    class ContractNegotiation {

        private final QueryValidator validator = QueryValidators.contractNegotiation();

        @Test
        void shouldSucceed_whenQueryIsEmpty() {
            var result = validator.validate(QuerySpec.none());

            assertThat(result).isSucceeded();
        }

        @ParameterizedTest
        @ArgumentsSource(InvalidFilters.class)
        void shouldFail_whenInvalidFilters(Criterion invalidFilter) {
            var query = QuerySpec.Builder.newInstance()
                    .filter(invalidFilter)
                    .build();

            var result = validator.validate(query);

            assertThat(result).isFailed();
        }

        @ParameterizedTest
        @ArgumentsSource(ValidFilters.class)
        void shouldSucceed_whenValidFilters(Criterion validFilter) {
            var query = QuerySpec.Builder.newInstance()
                    .filter(validFilter)
                    .build();

            var result = validator.validate(query);

            assertThat(result).isSucceeded();
        }


        private static class InvalidFilters implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(criterion("contractAgreement.contractStartDate.begin", "=", "123455")), // invalid path
                        arguments(criterion("contractOffers.policy.unexistent", "=", "123455")), // invalid path
                        arguments(criterion("contractOffers.policy.assetid", "=", "123455")), // wrong case
                        arguments(criterion("contractOffers.policy.=some-id", "=", "123455")) // incomplete path
                );
            }
        }

        private static class ValidFilters implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
                return Stream.of(
                        arguments(criterion("contractAgreement.assetId", "=", "test-asset")),
                        arguments(criterion("contractAgreement.policy.assignee", "=", "123455"))
                );
            }
        }
    }

}
