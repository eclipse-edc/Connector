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

package org.eclipse.edc.connector.service.asset;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.dataaddress.DataAddressValidator;
import org.eclipse.edc.spi.observe.asset.AssetObservable;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.service.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AssetServiceImplTest {

    private final AssetIndex index = mock(AssetIndex.class);
    private final ContractNegotiationStore contractNegotiationStore = mock(ContractNegotiationStore.class);
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();
    private final AssetObservable observable = mock(AssetObservable.class);
    private final DataAddressValidator dataAddressValidator = mock(DataAddressValidator.class);

    private final AssetServiceImpl service = new AssetServiceImpl(index, contractNegotiationStore, dummyTransactionContext,
            observable, dataAddressValidator);

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

        assertThat(assets.succeeded()).isTrue();
        assertThat(assets.getContent()).hasSize(1).first().matches(hasId("assetId"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            Asset.PROPERTY_ID,
            Asset.PROPERTY_NAME,
            Asset.PROPERTY_DESCRIPTION,
            Asset.PROPERTY_VERSION,
            Asset.PROPERTY_CONTENT_TYPE
    })
    void query_validFilter(String filter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(filter + "=somevalue")
                .build();

        service.query(query);

        verify(index).queryAssets(query);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "asset_prop_id in (foo, bar)", // invalid key
            "customProp=whatever", // no custom properties supported
    })
    void query_invalidFilter(String filter) {
        var query = QuerySpec.Builder.newInstance()
                .filter(filter)
                .build();

        var result = service.query(query);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).hasSize(1);
    }

    @Test
    void createAsset_shouldCreateAssetIfItDoesNotAlreadyExist() {
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());
        var assetId = "assetId";
        var asset = createAsset(assetId);
        when(index.findById(assetId)).thenReturn(null);
        var addressType = "addressType";
        var dataAddress = DataAddress.Builder.newInstance().type(addressType).build();

        var inserted = service.create(asset, dataAddress);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).matches(hasId(assetId));
        verify(index).accept(argThat(it -> assetId.equals(it.getId())), argThat(it -> addressType.equals(it.getType())));
        verify(observable).invokeForEach(any());
    }

    @Test
    void createAsset_shouldNotCreateAssetIfItAlreadyExists() {
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());
        var asset = createAsset("assetId");
        when(index.findById("assetId")).thenReturn(asset);
        var dataAddress = DataAddress.Builder.newInstance().type("addressType").build();

        var inserted = service.create(asset, dataAddress);

        assertThat(inserted.succeeded()).isFalse();
    }

    @Test
    void createAsset_shouldNotCreateAssetIfDataAddressInvalid() {
        var asset = createAsset("assetId");
        var dataAddress = DataAddress.Builder.newInstance().type("addressType").build();
        when(dataAddressValidator.validate(any())).thenReturn(Result.failure("Data address is invalid"));

        var result = service.create(asset, dataAddress);

        assertThat(result).satisfies(ServiceResult::failed)
                .extracting(ServiceResult::reason)
                .isEqualTo(BAD_REQUEST);
        verifyNoInteractions(index);
    }

    @Test
    void delete_shouldDeleteAssetIfItsNotReferencedByAnyNegotiation() {
        when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.empty());
        when(index.deleteById("assetId")).thenReturn(createAsset("assetId"));

        var deleted = service.delete("assetId");

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId("assetId"));
    }

    @Test
    void delete_shouldNotDeleteIfAssetIsAlreadyPartOfAnAgreement() {
        var asset = createAsset("assetId");
        when(index.deleteById("assetId")).thenReturn(asset);
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
        Mockito.verifyNoMoreInteractions(contractNegotiationStore);
    }

    @Test
    void delete_shouldFailIfAssetDoesNotExist() {
        when(index.deleteById("assetId")).thenReturn(null);

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


    @Test
    void updateAsset_shouldUpdateWhenExists() {
        var assetId = "assetId";
        var asset = createAsset(assetId);
        when(index.findById(assetId)).thenReturn(asset);

        var updated = service.update(assetId, asset);

        assertThat(updated.succeeded()).isTrue();
        verify(index).updateAsset(eq(assetId), eq(asset));
        verify(observable).invokeForEach(any());
    }

    @Test
    void updateAsset_shouldReturnBadRequest_whenAssetIdWrong() {
        var assetId = "assetId";
        var asset = createAsset(assetId);
        when(index.findById(assetId)).thenReturn(asset);

        var updated = service.update("another-id", asset);

        assertThat(updated.failed()).isTrue();
        assertThat(updated.reason()).isEqualTo(BAD_REQUEST);
        verifyNoInteractions(index);
        verifyNoInteractions(observable);
    }

    @Test
    void updateAsset_shouldReturnNotFound_whenNotExists() {
        var assetId = "assetId";
        var asset = createAsset(assetId);
        when(index.findById(assetId)).thenReturn(null);

        var updated = service.update(assetId, asset);

        assertThat(updated.failed()).isTrue();
        assertThat(updated.reason()).isEqualTo(NOT_FOUND);
        verify(index, never()).updateAsset(eq(assetId), eq(asset));
        verify(observable, never()).invokeForEach(any());
    }

    @Test
    void updateDataAddress_shouldUpdateWhenExists() {
        when(dataAddressValidator.validate(any())).thenReturn(Result.success());
        var assetId = "assetId";
        var asset = createAsset(assetId);
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
        when(index.findById(assetId)).thenReturn(asset);

        var updated = service.update(assetId, dataAddress);

        assertThat(updated.succeeded()).isTrue();
        verify(index).updateDataAddress(eq(assetId), eq(dataAddress));
        verify(observable).invokeForEach(any());
    }

    @Test
    void updateDataAddress_shouldReturnNotFound_whenNotExists() {
        var assetId = "assetId";
        var dataAddress = DataAddress.Builder.newInstance().type("test-type").build();
        when(index.findById(assetId)).thenReturn(null);

        var updated = service.update(assetId, dataAddress);

        assertThat(updated.failed()).isTrue();
        assertThat(updated.reason()).isEqualTo(NOT_FOUND);
        verify(index, never()).updateDataAddress(eq(assetId), eq(dataAddress));
        verify(observable, never()).invokeForEach(any());
    }

    @NotNull
    private Predicate<Asset> hasId(String assetId) {
        return it -> assetId.equals(it.getId());
    }

    private Asset createAsset(String assetId) {
        return Asset.Builder.newInstance().id(assetId).build();
    }
}
