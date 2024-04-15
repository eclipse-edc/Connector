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

package org.eclipse.edc.connector.controlplane.services.contractnegotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractNegotiationServiceImplIntegrationTest {
    private final ContractNegotiationStore store = mock();
    private final ConsumerContractNegotiationManager consumerManager = mock();
    private final CommandHandlerRegistry commandHandlerRegistry = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final QueryValidator queryValidator = mock();
    private final ContractNegotiationService service = new ContractNegotiationServiceImpl(store, consumerManager, transactionContext, commandHandlerRegistry);

    @ParameterizedTest
    @ArgumentsSource(InvalidFilters.class)
    void search_usingQueryValidator_withInvalidFilter_resultsInFailure(Criterion invalidFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(invalidFilter)
                .build();
        when(queryValidator.validate(query)).thenReturn(Result.failure("Test"));

        var result = service.search(query);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(ValidFilters.class)
    void search_usingQueryValidator_withValidFilter_executeStoreQuerySuccessfully(Criterion validFilter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(validFilter)
                .build();
        when(queryValidator.validate(query)).thenReturn(Result.success());

        var result = service.search(query);

        assertThat(result).isSucceeded();
        verify(store).queryNegotiations(query);
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
