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

package org.eclipse.edc.connector.controlplane.services.asset;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.asset.spi.observe.AssetObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AssetServiceImplIntegrationTest {
    private final AssetIndex index = mock();
    private final ContractNegotiationStore contractNegotiationStore = mock();
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();
    private final AssetObservable observable = mock();
    private final DataAddressValidatorRegistry dataAddressValidator = mock();
    private final AssetService service = new AssetServiceImpl(index, contractNegotiationStore, dummyTransactionContext, observable, dataAddressValidator);

    @ParameterizedTest
    @ValueSource(strings = { Asset.PROPERTY_ID, Asset.PROPERTY_NAME, Asset.PROPERTY_DESCRIPTION, Asset.PROPERTY_VERSION, Asset.PROPERTY_CONTENT_TYPE })
    void search_usingQueryValidator_withValidFilter_queryAssetsSuccessfully(String filter) {
        var query = QuerySpec.Builder.newInstance().filter(criterion(filter, "=", "somevalue")).build();

        service.search(query);

        verify(index).queryAssets(query);
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidFilters.class)
    void search_usingQueryValidator_withInvalidFilter_resultsInFailure(Criterion filter) {
        var query = QuerySpec.Builder.newInstance().filter(filter).build();

        var result = service.search(query);

        assertThat(result.failed()).isTrue();
    }

    private static class InvalidFilters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(arguments(criterion("  asset_prop_id", "in", "(foo, bar)")), // invalid leading whitespace
                    arguments(criterion(".customProp", "=", "whatever"))  // invalid leading dot
            );
        }
    }
}
