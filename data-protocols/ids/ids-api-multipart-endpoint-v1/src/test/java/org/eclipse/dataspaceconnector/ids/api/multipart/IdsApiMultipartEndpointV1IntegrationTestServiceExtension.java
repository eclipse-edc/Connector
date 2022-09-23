/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - extend tests
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *       Fraunhofer Institute for Software and Systems Engineering - remove ObjectMapperFactory
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import kotlin.NotImplementedError;
import org.eclipse.dataspaceconnector.common.util.junit.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenParameters;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyArchive;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.AssetEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;

@ComponentTest
@Provides({
        AssetIndex.class,
        DataAddressResolver.class,
        ContractDefinitionStore.class,
        IdentityService.class,
        TransferProcessStore.class,
        ContractOfferService.class,
        ContractValidationService.class,
        PolicyArchive.class
})
class IdsApiMultipartEndpointV1IntegrationTestServiceExtension implements ServiceExtension {
    private final List<Asset> assets;

    IdsApiMultipartEndpointV1IntegrationTestServiceExtension(List<Asset> assets) {
        this.assets = Objects.requireNonNull(assets);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(IdentityService.class, new FakeIdentityService());
        context.registerService(TransferProcessStore.class, new FakeTransferProcessStore());
        context.registerService(RemoteMessageDispatcherRegistry.class, new FakeRemoteMessageDispatcherRegistry());
        var assetIndex = new FakeAssetIndex(assets);
        context.registerService(AssetIndex.class, assetIndex);
        context.registerService(DataAddressResolver.class, assetIndex);
        context.registerService(ContractDefinitionStore.class, new FakeContractDefinitionStore());
        context.registerService(ContractValidationService.class, new FakeContractValidationService());
        context.registerService(PolicyArchive.class, mock(PolicyArchive.class));
    }

    private static class FakeIdentityService implements IdentityService {
        @Override
        public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
            return Result.success(TokenRepresentation.Builder.newInstance().build());
        }

        @Override
        public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {
            return Result.success(ClaimToken.Builder.newInstance().build());
        }
    }

    private static class FakeAssetIndex implements AssetIndex {
        private final List<Asset> assets;

        private FakeAssetIndex(List<Asset> assets) {
            this.assets = Objects.requireNonNull(assets);
        }

        @Override
        public Stream<Asset> queryAssets(AssetSelectorExpression expression) {
            return assets.stream();
        }

        @Override
        public Stream<Asset> queryAssets(QuerySpec querySpec) {
            throw new UnsupportedOperationException("Filtering/Paging not supported");
        }

        @Override
        public Asset findById(String assetId) {
            return assets.stream().filter(a -> a.getId().equals(assetId)).findFirst().orElse(null);
        }

        @Override
        public void accept(AssetEntry item) {

        }

        @Override
        public Asset deleteById(String assetId) {
            return null;
        }

        @Override
        public DataAddress resolveForAsset(String assetId) {
            var asset = findById(assetId);
            if (asset == null) {
                return null;
            }
            return DataAddress.Builder.newInstance().type("test").build();
        }
    }

    private static class FakeTransferProcessStore implements TransferProcessStore {
        @Override
        public TransferProcess find(String id) {
            return null;
        }

        @Override
        public @Nullable String processIdForTransferId(String id) {
            return null;
        }

        @Override
        public void create(TransferProcess process) {
        }

        @Override
        public void update(TransferProcess process) {
        }

        @Override
        public void delete(String processId) {
        }

        @Override
        public Stream<TransferProcess> findAll(QuerySpec querySpec) {
            return null;
        }

        @Override
        public @NotNull List<TransferProcess> nextForState(int state, int max) {
            return emptyList();
        }

    }

    private static class FakeRemoteMessageDispatcherRegistry implements RemoteMessageDispatcherRegistry {

        @Override
        public void register(RemoteMessageDispatcher dispatcher) {
        }

        @Override
        public <T> CompletableFuture<T> send(Class<T> responseType, RemoteMessage message, MessageContext context) {
            return null;
        }
    }

    private static class FakeContractDefinitionStore implements ContractDefinitionStore {

        private final List<ContractDefinition> contractDefinitions = new ArrayList<>();

        @Override
        public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
            return contractDefinitions.stream();
        }

        @Override
        public ContractDefinition findById(String definitionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(Collection<ContractDefinition> definitions) {
            contractDefinitions.addAll(definitions);
        }

        @Override
        public void save(ContractDefinition definition) {
            contractDefinitions.add(definition);
        }

        @Override
        public void update(ContractDefinition definition) {
            throw new NotImplementedError();
        }

        @Override
        public ContractDefinition deleteById(String id) {
            throw new NotImplementedError();
        }

        @Override
        public void reload() {
            throw new NotImplementedError();
        }

        @Override
        public void accept(ContractDefinition item) {

        }
    }

    private static class FakeContractValidationService implements ContractValidationService {

        @Override
        public @NotNull Result<ContractOffer> validate(ClaimToken token, ContractOffer offer) {
            return Result.success(ContractOffer.Builder.newInstance().build());
        }

        @Override
        public @NotNull Result<ContractOffer> validate(ClaimToken token, ContractOffer offer, ContractOffer latestOffer) {
            return Result.success(offer);
        }

        @Override
        public boolean validate(ClaimToken token, ContractAgreement agreement) {
            return true;
        }

        @Override
        public boolean validate(ClaimToken token, ContractAgreement agreement, ContractOffer latestOffer) {
            return false;
        }
    }

}
