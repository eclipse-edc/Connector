/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Daimler TSS GmbH - fixed contract dates to epoch seconds
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import kotlin.NotImplementedError;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Provides({ AssetIndex.class, TransferProcessStore.class, ContractDefinitionStore.class, IdentityService.class, ContractNegotiationManager.class,
        ConsumerContractNegotiationManager.class, ProviderContractNegotiationManager.class, ContractOfferService.class, ContractValidationService.class })
class IdsApiMultipartDispatcherV1IntegrationTestServiceExtension implements ServiceExtension {
    private final List<Asset> assets;

    private final IdentityService identityService;

    public IdsApiMultipartDispatcherV1IntegrationTestServiceExtension(List<Asset> assets, IdentityService identityService) {
        this.assets = Objects.requireNonNull(assets);
        this.identityService = identityService;
    }

    private static ContractNegotiation fakeContractNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .counterPartyId("test-counterparty-1")
                .counterPartyAddress("test-counterparty-address")
                .protocol("test-protocol")
                .stateCount(1)
                .contractAgreement(ContractAgreement.Builder.newInstance().id("1")
                        .providerAgentId("provider")
                        .consumerAgentId("consumer")
                        .asset(Asset.Builder.newInstance().build())
                        .policy(Policy.Builder.newInstance().build())
                        .contractSigningDate(Instant.now().getEpochSecond())
                        .contractStartDate(Instant.now().getEpochSecond())
                        .contractEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond())
                        .id("1:2").build())
                .state(ContractNegotiationStates.CONFIRMED.code())
                .build();
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(IdentityService.class, identityService);
        context.registerService(TransferProcessStore.class, new FakeTransferProcessStore());
        context.registerService(RemoteMessageDispatcherRegistry.class, new FakeRemoteMessageDispatcherRegistry());
        context.registerService(AssetIndex.class, new FakeAssetIndex(assets));
        context.registerService(ContractOfferService.class, new FakeContractOfferService(assets));
        context.registerService(ContractDefinitionStore.class, new FakeContractDefinitionStore());
        context.registerService(ContractValidationService.class, new FakeContractValidationService());
        context.registerService(ProviderContractNegotiationManager.class, new FakeProviderContractNegotiationManager());
        context.registerService(ConsumerContractNegotiationManager.class, new FakeConsumerContractNegotiationManager());
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
                            .policy(createEverythingAllowedPolicy())
                            .id("1")
                            .asset(asset)
                            .build());
        }

        private Policy createEverythingAllowedPolicy() {
            var policyBuilder = Policy.Builder.newInstance();
            var permissionBuilder = Permission.Builder.newInstance();
            var actionBuilder = Action.Builder.newInstance();

            policyBuilder.type(PolicyType.CONTRACT);
            actionBuilder.type("USE");

            permissionBuilder.action(actionBuilder.build());
            policyBuilder.permission(permissionBuilder.build());
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
            return Collections.EMPTY_LIST;
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

        public FakeContractDefinitionStore() {
            Policy publicPolicy = Policy.Builder.newInstance()
                    .permission(Permission.Builder.newInstance()
                            .target("2")
                            .action(Action.Builder.newInstance()
                                    .type("USE")
                                    .build())
                            .build())
                    .build();

            ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                    .id("1")
                    .accessPolicy(publicPolicy)
                    .contractPolicy(publicPolicy)
                    .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals("id", "1").build())
                    .build();

            contractDefinitions.add(contractDefinition);
        }

        @Override
        public @NotNull Collection<ContractDefinition> findAll() {
            return contractDefinitions;
        }

        @Override
        public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
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

    private static class FakeProviderContractNegotiationManager implements ProviderContractNegotiationManager {

        @Override
        public NegotiationResult declined(ClaimToken token, String negotiationId) {
            return NegotiationResult.success(fakeContractNegotiation());
        }

        @Override
        public void enqueueCommand(ContractNegotiationCommand command) {
        }

        @Override
        public NegotiationResult requested(ClaimToken token, ContractOfferRequest request) {
            return NegotiationResult.success(fakeContractNegotiation());
        }

        @Override
        public NegotiationResult offerReceived(ClaimToken token, String correlationId, ContractOffer offer, String hash) {
            return NegotiationResult.success(fakeContractNegotiation());
        }

        @Override
        public NegotiationResult consumerApproved(ClaimToken token, String correlationId, ContractAgreement agreement, String hash) {
            return NegotiationResult.success(fakeContractNegotiation());
        }
    }

    private static class FakeConsumerContractNegotiationManager implements ConsumerContractNegotiationManager {

        @Override
        public NegotiationResult initiate(ContractOfferRequest contractOffer) {
            return NegotiationResult.success(fakeContractNegotiation());
        }

        @Override
        public NegotiationResult offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash) {
            return NegotiationResult.success(fakeContractNegotiation());
        }

        @Override
        public NegotiationResult confirmed(ClaimToken token, String negotiationId, ContractAgreement contract, String hash) {
            return NegotiationResult.success(fakeContractNegotiation());
        }

        @Override
        public NegotiationResult declined(ClaimToken token, String negotiationId) {
            return NegotiationResult.success(fakeContractNegotiation());
        }

        @Override
        public void enqueueCommand(ContractNegotiationCommand command) {
        }
    }
}
