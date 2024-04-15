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

package org.eclipse.edc.connector.controlplane.services.transferprocess;

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class TransferProcessServiceImplIntegrationTest {

    private final TransferProcessStore store = mock();
    private final TransferProcessManager manager = mock();
    private final TransactionContext transactionContext = spy(new NoopTransactionContext());
    private final DataAddressValidatorRegistry dataAddressValidator = mock();
    private final CommandHandlerRegistry commandHandlerRegistry = mock();

    private final TransferProcessService service = new TransferProcessServiceImpl(store, manager, transactionContext,
            dataAddressValidator, commandHandlerRegistry);

    @ParameterizedTest
    @ArgumentsSource(InvalidFilters.class)
    void search_usingQueryValidator_withInvalidFilter_resultsInFailure(Criterion invalidFilter) {
        var spec = QuerySpec.Builder.newInstance().filter(invalidFilter).build();

        var result = service.search(spec);

        assertThat(result.failed()).isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(ValidFilters.class)
    void search_usingQueryValidator_withValidFilter_transactionExecutedSuccessfully(Criterion validFilter) {
        var spec = QuerySpec.Builder.newInstance().filter(validFilter).build();

        service.search(spec);

        verify(store).findAll(spec);
        verify(transactionContext).execute(any(TransactionContext.ResultTransactionBlock.class));
    }

    private static class InvalidFilters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(criterion("provisionedResourceSet.resources.hastoken", "=", "true")), // wrong case
                    arguments(criterion("resourceManifest.definitions.notexist", "=", "foobar")), // property not exist
                    arguments(criterion("contentDataAddress.properties[*].someKey", "=", "someval")) // map types not supported
            );
        }
    }

    private static class ValidFilters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(criterion("deprovisionedResources.provisionedResourceId", "=", "someval")),
                    arguments(criterion("type", "=", "CONSUMER")),
                    arguments(criterion("provisionedResourceSet.resources.hasToken", "=", "true"))
            );
        }
    }
}
