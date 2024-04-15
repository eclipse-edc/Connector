/*
 *  Copyright (c) 2024 Encho Belezirev (Digital Lights)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Encho Belezirev (Digital Lights) - refactor(test): improve QueryValidator testing strategy
 *
 */

package org.eclipse.edc.connector.controlplane.services.policydefinition;

import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.observe.PolicyDefinitionObservable;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

class PolicyDefinitionServiceImplIntegrationTest {
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final ContractDefinitionStore contractDefinitionStore = mock(ContractDefinitionStore.class);
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();
    private final PolicyDefinitionObservable observable = mock(PolicyDefinitionObservable.class);

    private final PolicyDefinitionServiceImpl policyServiceImpl = new PolicyDefinitionServiceImpl(dummyTransactionContext, policyStore, contractDefinitionStore, observable);

    @ParameterizedTest
    @ArgumentsSource(InvalidFilters.class)
    void search_usingQueryValidator_withInvalidExpression_resultsInFailure(Criterion invalidFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(invalidFilter)
                .build();

        var result = policyServiceImpl.search(query);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(ValidFilters.class)
    void search_usingQueryValidator_usingValidExpression_privateProperties_resultsInSuccess(Criterion validFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(validFilter)
                .build();

        var result = policyServiceImpl.search(query);

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
