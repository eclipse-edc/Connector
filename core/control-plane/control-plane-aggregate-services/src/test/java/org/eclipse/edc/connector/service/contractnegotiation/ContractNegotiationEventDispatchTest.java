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

package org.eclipse.edc.connector.service.contractnegotiation;

import org.eclipse.edc.connector.contract.spi.negotiation.NegotiationWaitStrategy;
import org.eclipse.edc.connector.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.event.EventRouter;
import org.eclipse.edc.spi.event.EventSubscriber;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationConfirmed;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationRequested;
import org.eclipse.edc.spi.event.contractnegotiation.ContractNegotiationTerminated;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.RemoteMessageDispatcher;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(EdcExtension.class)
class ContractNegotiationEventDispatchTest {

    private static final long CONTRACT_VALIDITY = TimeUnit.HOURS.toSeconds(1);
    private final EventSubscriber eventSubscriber = mock(EventSubscriber.class);
    private final ClaimToken token = ClaimToken.Builder.newInstance().build();

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "edc.negotiation.consumer.send.retry.limit", "0",
                "edc.negotiation.provider.send.retry.limit", "0"
        ));
        extension.registerServiceMock(NegotiationWaitStrategy.class, () -> 1);
    }

    @Test
    void shouldDispatchEventsOnProviderContractNegotiationStateChanges(EventRouter eventRouter, RemoteMessageDispatcherRegistry dispatcherRegistry,
                                                                       ProviderContractNegotiationManager manager, ContractDefinitionStore contractDefinitionStore,
                                                                       PolicyDefinitionStore policyDefinitionStore, AssetIndex assetIndex) {
        dispatcherRegistry.register(succeedingDispatcher());
        eventRouter.register(eventSubscriber);
        var policy = Policy.Builder.newInstance().build();
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("contractDefinitionId")
                .contractPolicyId("policyId")
                .accessPolicyId("policyId")
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .validity(CONTRACT_VALIDITY)
                .build();
        contractDefinitionStore.save(contractDefinition);
        policyDefinitionStore.save(PolicyDefinition.Builder.newInstance().id("policyId").policy(policy).build());
        assetIndex.accept(Asset.Builder.newInstance().id("assetId").build(), DataAddress.Builder.newInstance().type("any").build());

        manager.requested(token, createContractOfferRequest(policy));

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(ContractNegotiationRequested.class));
            verify(eventSubscriber).on(isA(ContractNegotiationConfirmed.class));
        });
    }

    @Test
    void shouldDispatchEventsOnFailedContractNegotiation(ContractNegotiationService service, EventRouter eventRouter,
                                                         RemoteMessageDispatcherRegistry dispatcherRegistry) {
        dispatcherRegistry.register(succeedingDispatcher());
        eventRouter.register(eventSubscriber);
        var policy = Policy.Builder.newInstance().build();

        var negotiation = service.initiateNegotiation(createContractOfferRequest(policy));

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(ContractNegotiationRequested.class));
        });

        service.cancel(negotiation.getId());

        await().untilAsserted(() -> {
            verify(eventSubscriber).on(isA(ContractNegotiationTerminated.class));
        });
    }

    private ContractOfferRequest createContractOfferRequest(Policy policy) {
        var now = ZonedDateTime.now();
        var contractOffer = ContractOffer.Builder.newInstance()
                .id("contractDefinitionId:" + UUID.randomUUID())
                .asset(Asset.Builder.newInstance().id("assetId").build())
                .policy(policy)
                .consumer(URI.create("http://any"))
                .provider(URI.create("http://any"))
                .contractStart(now)
                .contractEnd(now.plusSeconds(CONTRACT_VALIDITY))
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
        when(testDispatcher.send(any(), any())).thenReturn(CompletableFuture.completedFuture("any"));
        return testDispatcher;
    }

    @NotNull
    private RemoteMessageDispatcher failingDispatcher() {
        var testDispatcher = mock(RemoteMessageDispatcher.class);
        when(testDispatcher.protocol()).thenReturn("test");
        when(testDispatcher.send(any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("any")));
        return testDispatcher;
    }
}
