/*
 *  Copyright (c) 2021 - 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.api.datamanagement.asset.service;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.observe.asset.AssetObservable;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AssetServiceImplTest {

    private final AssetIndex index = mock(AssetIndex.class);
    private final AssetLoader loader = mock(AssetLoader.class);
    private final ContractNegotiationStore contractNegotiationStore = mock(ContractNegotiationStore.class);
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();
    private final AssetObservable observable = mock(AssetObservable.class);

    private final AssetServiceImpl service = new AssetServiceImpl(index, loader, contractNegotiationStore, dummyTransactionContext, observable);

    @Test
    void findById_shouldRelyOnAssetIndex() {
        when(index.findById("assetId")).thenReturn(createAsset("assetId"));

        var asset = service.findById("assetId");

        var assetId = "assetId";
        assertThat(asset).isNotNull().matches(hasId(assetId));
    }

    @Test
    void query_shouldRelyOnAssetIndex() {
        var asset = createAsset("assetId");
        when(index.queryAssets(any(QuerySpec.class))).thenReturn(Stream.of(asset));

        var assets = service.query(QuerySpec.none());

        assertThat(assets).hasSize(1).first().matches(hasId("assetId"));
    }

    @Test
    void createAsset_shouldCreateAssetIfItDoesNotAlreadyExist() {
        var assetId = "assetId";
        var asset = createAsset(assetId);
        when(index.findById(assetId)).thenReturn(null);
        var addressType = "addressType";
        var dataAddress = DataAddress.Builder.newInstance().type(addressType).build();

        var inserted = service.create(asset, dataAddress);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).matches(hasId(assetId));
        verify(loader).accept(argThat(it -> assetId.equals(it.getId())), argThat(it -> addressType.equals(it.getType())));
        verify(observable).invokeForEach(any());
    }

    @Test
    void createAsset_shouldNotCreateAssetIfItAlreadyExists() {
        var asset = createAsset("assetId");
        when(index.findById("assetId")).thenReturn(asset);
        var dataAddress = DataAddress.Builder.newInstance().type("addressType").build();

        var inserted = service.create(asset, dataAddress);

        assertThat(inserted.succeeded()).isFalse();
        verifyNoInteractions(loader);
    }

    @Test
    void delete_shouldDeleteAssetIfItsNotReferencedByAnyNegotiation() {
        when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.empty());
        when(loader.deleteById("assetId")).thenReturn(createAsset("assetId"));

        var deleted = service.delete("assetId");

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId("assetId"));
    }

    @Test
    void delete_shouldNotDeleteIfAssetIsAlreadyPartOfAnAgreement() {
        var asset = createAsset("assetId");
        when(loader.deleteById("assetId")).thenReturn(asset);
        var contractNegotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol("protocol")
                .contractAgreement(ContractAgreement.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .providerAgentId(UUID.randomUUID().toString())
                        .consumerAgentId(UUID.randomUUID().toString())
                        .assetId(asset.getId())
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .build();
        when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.of(contractNegotiation));

        var deleted = service.delete("assetId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(CONFLICT);
        verify(contractNegotiationStore).queryNegotiations(any());
        verifyNoMoreInteractions(contractNegotiationStore);
    }

    @Test
    void delete_shouldFailIfAssetDoesNotExist() {
        when(loader.deleteById("assetId")).thenReturn(null);

        var deleted = service.delete("assetId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }

    @Test
    @DisplayName("Verifies that the query matches the internal data model")
    void delete_verifyCorrectQuery() {
        var deleted = service.delete("test-asset");
        verify(contractNegotiationStore).queryNegotiations(argThat(argument -> argument.getFilterExpression().size() == 1 &&
                argument.getFilterExpression().get(0).getOperandLeft().equals("contractAgreement.assetId")));
    }

    @NotNull
    private Predicate<Asset> hasId(String assetId) {
        return it -> assetId.equals(it.getId());
    }

    private Asset createAsset(String assetId) {
        return Asset.Builder.newInstance().id(assetId).build();
    }
}