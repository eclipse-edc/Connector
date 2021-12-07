/*
 *  Copyright (c) 2021 Daimler TSS GmbH
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
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import kotlin.NotImplementedError;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResponse;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

class IdsApiMultipartEndpointV1IntegrationTestServiceExtension implements ServiceExtension {
    private final List<Asset> assets;

    public IdsApiMultipartEndpointV1IntegrationTestServiceExtension(List<Asset> assets) {
        this.assets = Objects.requireNonNull(assets);
    }

    @Override
    public Set<String> provides() {
        return Set.of("edc:iam", "edc:core:contract", "dataspaceconnector:transferprocessstore", AssetIndex.FEATURE, ContractDefinitionStore.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(IdentityService.class, new FakeIdentityService());
        context.registerService(TransferProcessStore.class, new FakeTransferProcessStore());
        context.registerService(RemoteMessageDispatcherRegistry.class, new FakeRemoteMessageDispatcherRegistry());
        context.registerService(AssetIndex.class, new FakeAssetIndex(assets));
        context.registerService(ContractOfferService.class, new FakeContractOfferService(assets));
        context.registerService(ContractDefinitionStore.class, new FakeContractDefinitionStore());
        context.registerService(ContractValidationService.class, new FakeContractValidationService());
        context.registerService(ContractNegotiationStore.class, new FakeContractNegotiationStore());
        context.registerService(ProviderContractNegotiationManager.class, new FakeProviderContractNegotiationManager());
        context.registerService(ConsumerContractNegotiationManager.class, new FakeConsumerContractNegotiationManager());
    }

    private static class FakeIdentityService implements IdentityService {
        @Override
        public Result<TokenRepresentation> obtainClientCredentials(String scope) {
            return Result.success(TokenRepresentation.Builder.newInstance().build());
        }

        @Override
        public Result<ClaimToken> verifyJwtToken(String token, String audience) {
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
        public Stream<Asset> queryAssets(List<Criterion> criteria) {
            return null;
        }

        @Override
        public Asset findById(String assetId) {
            return assets.stream().filter(a -> a.getId().equals(assetId)).findFirst().orElse(null);
        }
    }

    private static class FakeContractOfferService implements ContractOfferService {
        private final List<Asset> assets;

        private FakeContractOfferService(List<Asset> assets) {
            this.assets = assets;
        }

        @Override
        @NotNull
        public Stream<ContractOffer> queryContractOffers(ContractOfferQuery query) {
            return assets.stream().map(asset ->
                    ContractOffer.Builder.newInstance()
                            .id("1")
                            .policy(createEverythingAllowedPolicy())
                            .asset(asset)
                            .build()
            );
        }

        private Policy createEverythingAllowedPolicy() {
            var policyBuilder = Policy.Builder.newInstance();
            var permissionBuilder = Permission.Builder.newInstance();
            var actionBuilder = Action.Builder.newInstance();

            policyBuilder.type(PolicyType.CONTRACT);
            actionBuilder.type("USE");
            permissionBuilder.target("1");

            permissionBuilder.action(actionBuilder.build());
            policyBuilder.permission(permissionBuilder.build());

            policyBuilder.target("1");
            return policyBuilder.build();
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
        public @NotNull List<TransferProcess> nextForState(int state, int max) {
            return emptyList();
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
        public void createData(String processId, String key, Object data) {
        }

        @Override
        public void updateData(String processId, String key, Object data) {
        }

        @Override
        public void deleteData(String processId, String key) {
        }

        @Override
        public void deleteData(String processId, Set<String> keys) {
        }

        @Override
        public <T> T findData(Class<T> type, String processId, String resourceDefinitionId) {
            return null;
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
        public @NotNull Collection<ContractDefinition> findAll() {
            return contractDefinitions;
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
        public void delete(String id) {
            throw new NotImplementedError();
        }

        @Override
        public void reload() {
            throw new NotImplementedError();
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
        public boolean validate(ClaimToken token, ContractAgreement agreement, ContractOffer latestOffer) {
            return false;
        }

        @Override
        public boolean validate(ClaimToken token, ContractAgreement agreement) {
            return true;
        }
    }

    private static class FakeContractNegotiationStore implements ContractNegotiationStore {

        @Override
        public @Nullable ContractNegotiation find(String negotiationId) {
            return null;
        }

        @Override
        public @Nullable ContractNegotiation findForCorrelationId(String correlationId) {
            return null;
        }

        @Override
        public @Nullable ContractAgreement findContractAgreement(String contractId) {
            return null;
        }

        @Override
        public void save(ContractNegotiation negotiation) {

        }

        @Override
        public void delete(String negotiationId) {

        }

        @Override
        public @NotNull List<ContractNegotiation> nextForState(int state, int max) {
            return null;
        }
    }

    private static class FakeProviderContractNegotiationManager implements ProviderContractNegotiationManager {

        @Override
        public NegotiationResponse declined(ClaimToken token, String negotiationId) {
            return new NegotiationResponse(NegotiationResponse.Status.OK);
        }

        @Override
        public NegotiationResponse requested(ClaimToken token, ContractOfferRequest request) {
            return new NegotiationResponse(NegotiationResponse.Status.OK);
        }

        @Override
        public NegotiationResponse offerReceived(ClaimToken token, String correlationId, ContractOffer offer, String hash) {
            return new NegotiationResponse(NegotiationResponse.Status.OK);
        }

        @Override
        public NegotiationResponse consumerApproved(ClaimToken token, String correlationId, ContractAgreement agreement, String hash) {
            return new NegotiationResponse(NegotiationResponse.Status.OK);
        }
    }

    private static class FakeConsumerContractNegotiationManager implements ConsumerContractNegotiationManager {

        @Override
        public NegotiationResponse initiate(ContractOfferRequest contractOffer) {
            return new NegotiationResponse(NegotiationResponse.Status.OK);
        }

        @Override
        public NegotiationResponse offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash) {
            return new NegotiationResponse(NegotiationResponse.Status.OK);
        }

        @Override
        public NegotiationResponse confirmed(ClaimToken token, String negotiationId, ContractAgreement contract, String hash) {
            return new NegotiationResponse(NegotiationResponse.Status.OK);
        }

        @Override
        public NegotiationResponse declined(ClaimToken token, String negotiationId) {
            return new NegotiationResponse(NegotiationResponse.Status.OK);
        }
    }
}
