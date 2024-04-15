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

package org.eclipse.edc.connector.controlplane.services.contractdefinition;

import org.eclipse.edc.connector.controlplane.contract.spi.definition.observe.ContractDefinitionObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.definition.observe.ContractDefinitionObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractDefinitionServiceImplIntegrationTest {
    private final ContractDefinitionStore store = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ContractDefinitionObservable observable = new ContractDefinitionObservableImpl();
    private final ContractDefinitionService service = new ContractDefinitionServiceImpl(store, transactionContext, observable);

    @ParameterizedTest
    @ArgumentsSource(InvalidFilters.class)
    void search_usingQueryValidator_withInvalidFilter_resultsInFailure(Criterion invalidFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(invalidFilter)
                .build();

        var result = service.search(query);

        assertThat(result.failed()).isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(ValidFilters.class)
    void search_usingQueryValidator_withValidFilter_findsAllSuccessfully(Criterion validFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(validFilter)
                .build();
        when(store.findAll(query)).thenReturn(Stream.empty());

        service.search(query);

        verify(store).findAll(query);
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
