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
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private static final String PARTICIPANT_CONTEXT_ID = "participantContextId";

    private final AssetIndex index = mock();
    private final ContractNegotiationStore contractNegotiationStore = mock();
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();
    private final AssetObservable observable = mock();
    private final QueryValidator queryValidator = mock();
    private final Monitor monitor = mock();

    private final AssetService service = new AssetServiceImpl(index, contractNegotiationStore, dummyTransactionContext,
            observable, queryValidator, monitor);

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

    @Nested
    class CreateAsset {
        @Test
        void shouldCreateAssetIfItDoesNotAlreadyExist() {
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
        void shouldCreateAsset_whenDataAddressIsNull() {
            var assetId = "assetId";
            var asset = createAssetBuilder(assetId).dataAddress(null).build();
            when(index.create(asset)).thenReturn(StoreResult.success());

            var inserted = service.create(asset);

            assertThat(inserted).isSucceeded().matches(hasId(assetId));
            verify(index).create(and(isA(Asset.class), argThat(it -> assetId.equals(it.getId()))));
            verifyNoMoreInteractions(index);
            verify(observable).invokeForEach(any());
        }

        @Test
        void shouldNotCreateAssetIfItAlreadyExists() {
            var asset = createAsset("assetId");
            when(index.create(asset)).thenReturn(StoreResult.alreadyExists("test"));

            var inserted = service.create(asset);

            assertThat(inserted).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        }

        @Test
        void shouldLogWarning_whenAssetCatalogAndPropertiesNotSet() {
            var asset = createAssetBuilder("assetId").property(Asset.PROPERTY_IS_CATALOG, "true").build();
            when(index.create(asset)).thenReturn(StoreResult.success());

            service.create(asset);

            verify(monitor).warning(anyString());
        }

        @Test
        void shouldFail_whenPropertiesAreDuplicated() {
            var asset = createAssetBuilder("assetId").property("property", "value").privateProperty("property", "other-value").build();

            var result = service.create(asset);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
            verifyNoInteractions(index);
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDeleteAssetIfNotReferenceByContractAgreement() {
            when(index.findById("assetId")).thenReturn(createAsset("assetId"));
            when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.empty());
            when(index.deleteById("assetId")).thenReturn(StoreResult.success(createAsset("assetId")));

            var deleted = service.delete("assetId");

            assertThat(deleted.succeeded()).isTrue();
            assertThat(deleted.getContent()).matches(hasId("assetId"));
        }

        @Test
        void shouldNotDeleteIfAssetIsAlreadyPartOfAnAgreement() {
            var asset = createAsset("assetId");
            when(index.findById("assetId")).thenReturn(asset);
            when(index.deleteById("assetId")).thenReturn(StoreResult.success(asset));
            var contractNegotiation = ContractNegotiation.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .counterPartyId(UUID.randomUUID().toString())
                    .counterPartyAddress("address")
                    .protocol("protocol")
                    .type(PROVIDER)
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
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
            verify(index, never()).deleteById(any());
        }

        @ParameterizedTest
        @MethodSource("nonFinalStates")
        void shouldNotDeleteIfAssetIsAlreadyPartOfNotFinalNegotiation(ContractNegotiationStates state) {
            var asset = createAsset("assetId");
            when(index.findById("assetId")).thenReturn(asset);
            when(index.deleteById("assetId")).thenReturn(StoreResult.success(asset));
            var contractNegotiation = ContractNegotiation.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .counterPartyId(UUID.randomUUID().toString())
                    .counterPartyAddress("address")
                    .protocol("protocol")
                    .type(PROVIDER)
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
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
            when(index.findById("assetId")).thenReturn(null);

            var deleted = service.delete("assetId");

            assertThat(deleted.failed()).isTrue();
            assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
            verifyNoInteractions(contractNegotiationStore);
            verify(index, never()).deleteById(any());
        }

        @Test
        @DisplayName("Verifies that the query matches the internal data model")
        void verifyCorrectQuery() {
            when(index.findById("test-asset")).thenReturn(createAsset("test-asset"));
            when(index.deleteById(any())).thenReturn(StoreResult.success());

            var deleted = service.delete("test-asset");

            assertThat(deleted.succeeded()).isTrue();
            var captor = ArgumentCaptor.forClass(QuerySpec.class);
            verify(contractNegotiationStore).queryNegotiations(captor.capture());
            assertThat(captor.getValue().getFilterExpression())
                    .containsExactlyInAnyOrder(
                            new Criterion("contractAgreement.assetId", "=", "test-asset"),
                            new Criterion("type", "=", "PROVIDER"),
                            new Criterion("participantContextId", "=", PARTICIPANT_CONTEXT_ID));
        }

        @Test
        void shouldNotBeBlockedByConsumerNegotiationsOnTheSameAssetId() {
            when(index.findById("assetId")).thenReturn(createAsset("assetId"));
            when(index.deleteById("assetId")).thenReturn(StoreResult.success(createAsset("assetId")));
            // the store only returns negotiations matching the query, consumer ones are filtered out by the `type` criterion
            when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.empty());

            var deleted = service.delete("assetId");

            assertThat(deleted).isSucceeded();
            var captor = ArgumentCaptor.forClass(QuerySpec.class);
            verify(contractNegotiationStore).queryNegotiations(captor.capture());
            assertThat(captor.getValue().getFilterExpression())
                    .contains(new Criterion("type", "=", "PROVIDER"));
        }

        @Test
        void shouldFilterNegotiationsByAssetParticipantContext() {
            var asset = createAssetBuilder("assetId").participantContextId("another-context").build();
            when(index.findById("assetId")).thenReturn(asset);
            when(index.deleteById("assetId")).thenReturn(StoreResult.success(asset));
            when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.empty());

            var deleted = service.delete("assetId");

            assertThat(deleted).isSucceeded();
            var captor = ArgumentCaptor.forClass(QuerySpec.class);
            verify(contractNegotiationStore).queryNegotiations(captor.capture());
            assertThat(captor.getValue().getFilterExpression())
                    .contains(new Criterion("participantContextId", "=", "another-context"));
        }

        private static Stream<Arguments> nonFinalStates() {
            return Stream.of(ContractNegotiationStates.values())
                    .filter(state -> !ContractNegotiationStates.isFinal(state.code()))
                    .map(Arguments::of);
        }
    }

    @Nested
    class Update {
        @Test
        void shouldUpdateWhenExists() {
            var asset = createAsset("assetId");
            when(index.updateAsset(asset)).thenReturn(StoreResult.success(asset));

            var updated = service.update(asset);

            assertThat(updated.succeeded()).isTrue();
            verify(index).updateAsset(eq(asset));
            verifyNoMoreInteractions(index);
            verify(observable).invokeForEach(any());
        }

        @Test
        void shouldReturnNotFound_whenNotExists() {
            var asset = createAsset("assetId");
            when(index.updateAsset(eq(asset))).thenReturn(StoreResult.notFound("test"));

            var updated = service.update(asset);

            assertThat(updated.failed()).isTrue();
            assertThat(updated.reason()).isEqualTo(NOT_FOUND);
            verify(index, times(1)).updateAsset(asset);
            verifyNoMoreInteractions(index);
            verify(observable, never()).invokeForEach(any());
        }

        @Test
        void shouldLogWarning_whenAssetCatalogAndPropertiesNotSet() {
            var asset = createAssetBuilder("assetId").property(Asset.PROPERTY_IS_CATALOG, "true").build();
            when(index.updateAsset(asset)).thenReturn(StoreResult.success(asset));

            service.update(asset);

            verify(monitor).warning(anyString());
        }

        @Test
        void shouldFail_whenPropertiesAreDuplicated() {
            var asset = createAssetBuilder("assetId").property("property", "value").privateProperty("property", "other-value").build();

            var result = service.update(asset);

            assertThat(result).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
            verifyNoInteractions(index);
        }
    }

    @NotNull
    private Predicate<Asset> hasId(String assetId) {
        return it -> assetId.equals(it.getId());
    }

    private Asset createAsset(String assetId) {
        return createAssetBuilder(assetId).build();
    }

    private Asset.Builder createAssetBuilder(String assetId) {
        return Asset.Builder.newInstance().id(assetId).participantContextId(PARTICIPANT_CONTEXT_ID)
                .dataAddress(DataAddress.Builder.newInstance().type("any").build());
    }
}
