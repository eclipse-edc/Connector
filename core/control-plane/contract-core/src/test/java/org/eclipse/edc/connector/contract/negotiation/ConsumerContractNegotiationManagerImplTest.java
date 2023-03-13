/*
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.contract.negotiation;

import org.eclipse.edc.connector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.command.ContractNegotiationCommand;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.statemachine.retry.SendRetryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_AGREED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_AGREEING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_REQUESTED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.CONSUMER_REQUESTING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.DECLINED;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.ERROR;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsumerContractNegotiationManagerImplTest {

    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final PolicyDefinitionStore policyStore = mock(PolicyDefinitionStore.class);
    private final ContractNegotiationListener listener = mock(ContractNegotiationListener.class);
    private final SendRetryManager sendRetryManager = mock(SendRetryManager.class);
    private ConsumerContractNegotiationManagerImpl negotiationManager;

    @BeforeEach
    void setUp() {
        CommandQueue<ContractNegotiationCommand> queue = mock(CommandQueue.class);
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());

        CommandRunner<ContractNegotiationCommand> commandRunner = mock(CommandRunner.class);

        var observable = new ContractNegotiationObservableImpl();
        observable.registerListener(listener);

        negotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(mock(Monitor.class))
                .commandQueue(queue)
                .commandRunner(commandRunner)
                .observable(observable)
                .store(store)
                .policyStore(policyStore)
                .sendRetryManager(sendRetryManager)
                .build();
    }

    @Test
    void initiateShouldSaveNewNegotiationInInitialState() {
        var contractOffer = contractOffer();
        var request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .build();

        var result = negotiationManager.initiate(request);

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == INITIAL.code() &&
                        negotiation.getCounterPartyId().equals(request.getConnectorId()) &&
                        negotiation.getCounterPartyAddress().equals(request.getConnectorAddress()) &&
                        negotiation.getProtocol().equals(request.getProtocol()) &&
                        negotiation.getCorrelationId() == null &&
                        negotiation.getContractOffers().size() == 1 &&
                        negotiation.getLastContractOffer().equals(contractOffer))
        );
        verify(listener).initiated(any());
    }

    @Test
    void testConfirmedInvalidId() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);
        var policy = Policy.Builder.newInstance().build();

        var result = negotiationManager.confirmed(token, "not a valid id", contractAgreement, policy);

        assertThat(result.fatalError()).isTrue();
        verify(policyStore, never()).save(any());
        verify(store, never()).save(any());
        verifyNoInteractions(listener);
    }

    @Test
    void testConfirmedConfirmAgreement() {
        var negotiationConsumerRequested = createContractNegotiationConsumerRequested();
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);
        var def = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
        when(store.find(negotiationConsumerRequested.getId())).thenReturn(negotiationConsumerRequested);
        when(validationService.validateConfirmed(eq(contractAgreement), any(ContractOffer.class))).thenReturn(Result.success());

        var result = negotiationManager.confirmed(token, negotiationConsumerRequested.getId(), contractAgreement, def.getPolicy());

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == CONFIRMED.code() &&
                        negotiation.getContractAgreement() == contractAgreement
        ));
        verify(validationService).validateConfirmed(eq(contractAgreement), any(ContractOffer.class));
        verify(listener).confirmed(any());
    }

    @Test
    void testConfirmedDeclineAgreement() {
        var negotiationConsumerOffered = createContractNegotiationConsumerRequested();
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);
        var policy = Policy.Builder.newInstance().build();
        when(store.find(negotiationConsumerOffered.getId())).thenReturn(negotiationConsumerOffered);
        when(validationService.validateConfirmed(eq(contractAgreement), any(ContractOffer.class))).thenReturn(Result.failure("error"));

        var result = negotiationManager.confirmed(token, negotiationConsumerOffered.getId(), contractAgreement, policy);

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == DECLINING.code() &&
                        negotiation.getContractAgreement() == null
        ));
        verify(validationService).validateConfirmed(eq(contractAgreement), any(ContractOffer.class));
    }

    @Test
    void testDeclined() {
        var negotiationConsumerOffered = createContractNegotiationConsumerRequested();
        var token = ClaimToken.Builder.newInstance().build();
        when(store.find(negotiationConsumerOffered.getId())).thenReturn(negotiationConsumerOffered);

        var result = negotiationManager.declined(token, negotiationConsumerOffered.getId());

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation -> negotiation.getState() == DECLINED.code()));
        verify(listener).declined(any());
    }

    @Test
    void initial_shouldTransitionRequesting() {
        var negotiation = contractNegotiationBuilder().state(INITIAL.code()).build();
        when(store.nextForState(eq(INITIAL.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == CONSUMER_REQUESTING.code()));
        });
    }

    @Test
    void requesting_shouldSendOfferAndTransitionRequested() {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_REQUESTING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_REQUESTING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == CONSUMER_REQUESTED.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
            verify(listener).requested(any());
        });
    }

    @Test
    void requesting_shouldTransitionRequestingIfSendFails_andRetriesNotExhausted() {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_REQUESTING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_REQUESTING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(false);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == CONSUMER_REQUESTING.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
        });
    }

    @Test
    void requesting_shouldTransitionErrorIfSendFails_andRetriesExhausted() {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_REQUESTING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_REQUESTING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(true);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == ERROR.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
            verify(listener).failed(any());
        });
    }

    @Test
    void consumerApproving_shouldSendAgreementAndTransitionApproved() {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_AGREEING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_AGREEING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == CONSUMER_AGREED.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
            verify(listener).approved(any());
        });
    }

    @Test
    void consumerApproving_shouldTransitionApprovingIfSendFails_andRetriesNotExhausted() {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_AGREEING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_AGREEING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(false);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == CONSUMER_AGREEING.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
        });
    }

    @Test
    void consumerApproving_shouldTransitionErrorIfSendFails_andRetriesExhausted() {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_AGREEING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_AGREEING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(true);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == ERROR.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
            verify(listener).failed(any());
        });
    }

    @Test
    void declining_shouldSendRejectionAndTransitionDeclined() {
        var negotiation = contractNegotiationBuilder().state(DECLINING.code()).contractOffer(contractOffer()).build();
        negotiation.setErrorDetail("an error");
        when(store.nextForState(eq(DECLINING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == DECLINED.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
            verify(listener).declined(any());
        });
    }

    @Test
    void declining_shouldTransitionDecliningIfSendFails_andRetriesNotExhausted() {
        var negotiation = contractNegotiationBuilder().state(DECLINING.code()).contractOffer(contractOffer()).build();
        negotiation.setErrorDetail("an error");
        when(store.nextForState(eq(DECLINING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(false);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == DECLINING.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
        });
    }

    @Test
    void declining_shouldTransitionErrorIfSendFails_andRetriesExhausted() {
        var negotiation = contractNegotiationBuilder().state(DECLINING.code()).contractOffer(contractOffer()).build();
        negotiation.setErrorDetail("an error");
        when(store.nextForState(eq(DECLINING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        when(sendRetryManager.retriesExhausted(any())).thenReturn(true);

        negotiationManager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == ERROR.code()));
            verify(dispatcherRegistry, only()).send(any(), any());
            verify(listener).failed(any());
        });
    }

    private ContractNegotiation createContractNegotiationConsumerRequested() {
        var lastOffer = contractOffer();

        return contractNegotiationBuilder()
                .state(CONSUMER_REQUESTED.code())
                .contractOffer(lastOffer)
                .build();
    }

    private ContractNegotiation.Builder contractNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId("correlationId")
                .counterPartyId("connectorId")
                .counterPartyAddress("connectorAddress")
                .protocol("protocol")
                .stateTimestamp(Instant.now().toEpochMilli());
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance().id("id:id")
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().id("assetId").build())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now())
                .build();
    }

}
