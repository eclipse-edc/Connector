/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.service;

import org.eclipse.dataspaceconnector.contract.common.ContractId;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.junit.extensions.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.NegotiationWaitStrategy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ProviderContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.event.EventSubscriber;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationApproved;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationConfirmed;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationFailed;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationInitiated;
import org.eclipse.dataspaceconnector.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcher;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
public class ContractNegotiationEventDispatchTest {

    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);
    private final ClaimToken token = ClaimToken.Builder.newInstance().build();

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "edc.negotiation.consumer.send.retry.limit", "0",
                "edc.negotiation.provider.send.retry.limit", "0"
        ));
        extension.registerServiceMock(NegotiationWaitStrategy.class, () -> 1);
    }

    @Test
    void shouldDispatchEventsOnConsumerContractNegotiationStateChanges(ContractNegotiationService service, EventRouter eventRouter,
                                                                       RemoteMessageDispatcherRegistry dispatcherRegistry, ConsumerContractNegotiationManager manager) {
        dispatcherRegistry.register(succeedingDispatcher());
        eventRouter.register(eventSubscriber);
        var policy = Policy.Builder.newInstance().build();
        var contractOfferRequest = createContractOfferRequest(policy);

        var initiateResult = service.initiateNegotiation(contractOfferRequest);

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(ContractNegotiationInitiated.class));
            verify(eventSubscriber).on(isA(ContractNegotiationRequested.class));
        });

        manager.offerReceived(token, initiateResult.getId(), contractOfferRequest.getContractOffer(), "any");

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(ContractNegotiationApproved.class));
        });

        manager.confirmed(token, initiateResult.getId(), createContractAgreement(policy), policy);

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(ContractNegotiationConfirmed.class));
        });
    }

    @Test
    void shouldDispatchEventsOnProviderContractNegotiationStateChanges(EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry,
                                                                       ProviderContractNegotiationManager manager, ContractDefinitionStore contractDefinitionStore,
                                                                       PolicyDefinitionStore policyDefinitionStore, AssetIndex assetIndex) {
        dispatcherRegistry.register(succeedingDispatcher());
        eventRouter.register(eventSubscriber);
        var policy = Policy.Builder.newInstance().build();
        contractDefinitionStore.save(ContractDefinition.Builder.newInstance().id("contractDefinitionId").contractPolicyId("policyId").accessPolicyId("policyId").selectorExpression(AssetSelectorExpression.SELECT_ALL).build());
        policyDefinitionStore.save(PolicyDefinition.Builder.newInstance().uid("policyId").policy(policy).build());
        ((AssetLoader) assetIndex).accept(Asset.Builder.newInstance().id("assetId").build(), DataAddress.Builder.newInstance().type("any").build());

        manager.requested(token, createContractOfferRequest(policy));

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(ContractNegotiationRequested.class));
            verify(eventSubscriber).on(isA(ContractNegotiationConfirmed.class));
        });
    }

    @Test
    void shouldDispatchEventsOnFailedContractNegotiation(ContractNegotiationService service, EventRouter eventRouter,
                                                         RemoteMessageDispatcherRegistry dispatcherRegistry) {
        dispatcherRegistry.register(failingDispatcher());
        eventRouter.register(eventSubscriber);
        var policy = Policy.Builder.newInstance().build();

        service.initiateNegotiation(createContractOfferRequest(policy));

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(ContractNegotiationFailed.class));
        });
    }

    private ContractAgreement createContractAgreement(Policy policy) {
        return ContractAgreement.Builder.newInstance()
                .id(ContractId.createContractId("1"))
                .providerAgentId("any")
                .consumerAgentId("any")
                .assetId("assetId")
                .policy(policy)
                .build();
    }

    private ContractOfferRequest createContractOfferRequest(Policy policy) {
        var contractOffer = ContractOffer.Builder.newInstance()
                .id("contractDefinitionId:" + UUID.randomUUID())
                .asset(Asset.Builder.newInstance().id("assetId").build())
                .policy(policy)
                .consumer(URI.create("http://any"))
                .provider(URI.create("http://any"))
                .build();

        return ContractOfferRequest.Builder.newInstance()
                .protocol("test")
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .contractOffer(contractOffer)
                .correlationId("correlationId")
                .build();
    }

    @NotNull
    private RemoteMessageDispatcher succeedingDispatcher() {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture("any"));
        return testDispatcher;
    }

    @NotNull
    private RemoteMessageDispatcher failingDispatcher() {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.send(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("any")));
        return testDispatcher;
    }

}
