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

package org.eclipse.edc.connector.controlplane.services.asset;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.asset.spi.observe.AssetObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.connector.controlplane.services.spi.asset.AssetService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AssetServiceImplTest {

    private final AssetIndex index = mock();
    private final ContractNegotiationStore contractNegotiationStore = mock();
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();
    private final AssetObservable observable = mock();
    private final DataAddressValidatorRegistry dataAddressValidator = mock();
    private final QueryValidator queryValidator = mock();

    private final AssetService service = new AssetServiceImpl(index, contractNegotiationStore, dummyTransactionContext,
            observable, dataAddressValidator, queryValidator);

    @Test
    void findById_shouldRelyOnAssetIndex() {
        when(index.findById("assetId")).thenReturn(createAsset("assetId"));

        var asset = service.findById("assetId");

        var assetId = "assetId";
        assertThat(asset).isNotNull().matches(hasId(assetId));
    }

    @Test
    void search_shouldRelyOnAssetIndex() {
        var asset = createAsset("assetId");
        when(index.queryAssets(any(QuerySpec.class))).thenReturn(Stream.of(asset));
        when(queryValidator.validate(any())).thenReturn(Result.success());

        var assets = service.search(QuerySpec.none());

        assertThat(assets).isSucceeded().asInstanceOf(list(Asset.class))
                .hasSize(1).first().matches(hasId("assetId"));
    }

    @Test
    void search_shouldFail_whenQueryIsNotValid() {
        when(queryValidator.validate(any())).thenReturn(Result.failure("not valid"));

        var assets = service.search(QuerySpec.none());

        assertThat(assets).isFailed();
        verifyNoInteractions(contractNegotiationStore);
    }

    @Test
    void createAsset_shouldCreateAssetIfItDoesNotAlreadyExist() {
        when(dataAddressValidator.validateSource(any())).thenReturn(ValidationResult.success());
        var assetId = "assetId";
        var asset = createAsset(assetId);
        when(index.create(asset)).thenReturn(StoreResult.success());

        var inserted = service.create(asset);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).matches(hasId(assetId));
        verify(index).create(and(isA(Asset.class), argThat(it -> assetId.equals(it.getId()))));
        verifyNoMoreInteractions(index);
        verify(observable).invokeForEach(any());
    }

    @Test
    void createAsset_shouldNotCreateAssetIfItAlreadyExists() {
        when(dataAddressValidator.validateSource(any())).thenReturn(ValidationResult.success());
        var asset = createAsset("assetId");
        when(index.create(asset)).thenReturn(StoreResult.alreadyExists("test"));

        var inserted = service.create(asset);

        assertThat(inserted).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
    }

    @Test
    void createAsset_shouldNotCreateAssetIfDataAddressInvalid() {
        var asset = createAsset("assetId");
        when(dataAddressValidator.validateSource(any())).thenReturn(ValidationResult.failure(violation("Data address is invalid", "path")));

        var result = service.create(asset);

        Assertions.assertThat(result).satisfies(ServiceResult::failed).extracting(ServiceResult::reason).isEqualTo(BAD_REQUEST);
        verifyNoInteractions(index);
    }

    @Test
    void createAsset_shouldFail_whenPropertiesAreDuplicated() {
        var asset = createAssetBuilder("assetId").property("property", "value").privateProperty("property", "other-value").build();
        when(dataAddressValidator.validateSource(any())).thenReturn(ValidationResult.success());

        var result = service.create(asset);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verifyNoInteractions(index);
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteAssetIfNotReferenceByContractAgreement() {
            when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.empty());
            when(index.deleteById("assetId")).thenReturn(StoreResult.success(createAsset("assetId")));

            var deleted = service.delete("assetId");

            assertThat(deleted.succeeded()).isTrue();
            assertThat(deleted.getContent()).matches(hasId("assetId"));
        }

        @Test
        void shouldNotDeleteIfAssetIsAlreadyPartOfAnAgreement() {
            var asset = createAsset("assetId");
            when(index.deleteById("assetId")).thenReturn(StoreResult.success(asset));
            var contractNegotiation = ContractNegotiation.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .counterPartyId(UUID.randomUUID().toString())
                    .counterPartyAddress("address")
                    .protocol("protocol")
                    .contractAgreement(ContractAgreement.Builder.newInstance()
                            .id(UUID.randomUUID().toString())
                            .providerId(UUID.randomUUID().toString())
                            .consumerId(UUID.randomUUID().toString())
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

        @ParameterizedTest
        @MethodSource("nonFinalStates")
        void shouldNotDeleteIfAssetIsAlreadyPartOfNotFinalNegotiation(ContractNegotiationStates state) {
            var asset = createAsset("assetId");
            when(index.deleteById("assetId")).thenReturn(StoreResult.success(asset));
            var contractNegotiation = ContractNegotiation.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .counterPartyId(UUID.randomUUID().toString())
                    .counterPartyAddress("address")
                    .protocol("protocol")
                    .state(state.code())
                    .build();
            when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.of(contractNegotiation));

            var deleted = service.delete("assetId");

            assertThat(deleted.failed()).isTrue();
            assertThat(deleted.getFailure().getReason()).isEqualTo(CONFLICT);
            verify(contractNegotiationStore).queryNegotiations(any());
            verifyNoMoreInteractions(contractNegotiationStore);
        }

        @Test
        void shouldFailIfAssetDoesNotExist() {
            when(index.deleteById("assetId")).thenReturn(StoreResult.notFound("test"));

            var deleted = service.delete("assetId");

            assertThat(deleted.failed()).isTrue();
            assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
        }

        @Test
        @DisplayName("Verifies that the query matches the internal data model")
        void verifyCorrectQuery() {
            when(index.deleteById(any())).thenReturn(StoreResult.success());

            var deleted = service.delete("test-asset");
            assertThat(deleted.succeeded()).isTrue();
            verify(contractNegotiationStore).queryNegotiations(argThat(argument -> argument.getFilterExpression().size() == 1 && argument.getFilterExpression().get(0).getOperandLeft().equals("contractAgreement.assetId")));
        }

        private static Stream<Arguments> nonFinalStates() {
            return Stream.of(ContractNegotiationStates.values())
                    .filter(state -> !ContractNegotiationStates.isFinal(state.code()))
                    .map(Arguments::of);
        }
    }

    @Test
    void updateAsset_shouldUpdateWhenExists() {
        var asset = createAsset("assetId");
        when(dataAddressValidator.validateSource(any())).thenReturn(ValidationResult.success());
        when(index.updateAsset(asset)).thenReturn(StoreResult.success(asset));

        var updated = service.update(asset);

        assertThat(updated.succeeded()).isTrue();
        verify(index).updateAsset(eq(asset));
        verifyNoMoreInteractions(index);
        verify(observable).invokeForEach(any());
    }

    @Test
    void updateAsset_shouldReturnNotFound_whenNotExists() {
        var asset = createAsset("assetId");
        when(dataAddressValidator.validateSource(any())).thenReturn(ValidationResult.success());
        when(index.updateAsset(eq(asset))).thenReturn(StoreResult.notFound("test"));

        var updated = service.update(asset);

        assertThat(updated.failed()).isTrue();
        assertThat(updated.reason()).isEqualTo(NOT_FOUND);
        verify(index, times(1)).updateAsset(asset);
        verifyNoMoreInteractions(index);
        verify(observable, never()).invokeForEach(any());
    }

    @Test
    void updateAsset_shouldFail_whenPropertiesAreDuplicated() {
        var asset = createAssetBuilder("assetId").property("property", "value").privateProperty("property", "other-value").build();

        var result = service.update(asset);

        assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verifyNoInteractions(index);
    }

    @NotNull
    private Predicate<Asset> hasId(String assetId) {
        return it -> assetId.equals(it.getId());
    }

    private Asset createAsset(String assetId) {
        return createAssetBuilder(assetId).build();
    }

    private Asset.Builder createAssetBuilder(String assetId) {
        return Asset.Builder.newInstance().id(assetId).dataAddress(DataAddress.Builder.newInstance().type("any").build());
    }
}
