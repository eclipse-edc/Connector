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
 *
 */
package org.eclipse.dataspaceconnector.contract.negotiation;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_APPROVED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_APPROVING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_OFFERED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONSUMER_OFFERING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.UNSAVED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerContractNegotiationManagerImplTest {

    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final CommandQueue<ContractNegotiationCommand> queue = mock(CommandQueue.class);
    private final CommandRunner<ContractNegotiationCommand> runner = mock(CommandRunner.class);
    private ConsumerContractNegotiationManagerImpl manager;

    @BeforeEach
    void setUp() throws Exception {
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());

        manager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(mock(Monitor.class))
                .commandQueue(queue)
                .commandRunner(runner)
                .observable(mock(ContractNegotiationObservable.class))
                .store(store)
                .build();
    }

    @Test
    void initiate_shouldSaveNewNegotiationInInitialState() {
        var contractOffer = contractOffer();

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .build();

        var result = manager.initiate(request);

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
    }

    @Test
    void testOfferReceivedInvalidId() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();

        var result = manager.offerReceived(token, "not a valid id", contractOffer, "hash");

        assertThat(result.getFailure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testOfferReceivedConfirmOffer() {
        var negotiationRequested = createContractNegotiationRequested();
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.success(contractOffer));
        when(store.find(negotiationRequested.getId())).thenReturn(negotiationRequested);

        var result = manager.offerReceived(token, negotiationRequested.getId(), contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == CONSUMER_APPROVING.code() &&
                negotiation.getContractOffers().size() == 2 &&
                negotiation.getContractOffers().get(1).equals(contractOffer)
        ));
        verify(validationService).validate(eq(token), eq(contractOffer), any(ContractOffer.class));
    }

    @Test
    void testOfferReceivedDeclineOffer() {
        var negotiationRequested = createContractNegotiationRequested();
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.failure("error"));
        when(store.find(negotiationRequested.getId())).thenReturn(negotiationRequested);

        var result = manager.offerReceived(token, negotiationRequested.getId(), contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == DECLINING.code() &&
                negotiation.getContractOffers().size() == 2 &&
                negotiation.getContractOffers().get(1).equals(contractOffer)
        ));
        verify(validationService).validate(eq(token), eq(contractOffer), any(ContractOffer.class));
    }

    @Test
    void testConfirmedInvalidId() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);

        var result = manager.confirmed(token, "not a valid id", contractAgreement, "hash");

        assertThat(result.getFailure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testConfirmedConfirmAgreement() {
        var negotiationConsumerOffered = createContractNegotiationConsumerOffered();
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);
        when(store.find(negotiationConsumerOffered.getId())).thenReturn(negotiationConsumerOffered);
        when(validationService.validate(eq(token), eq(contractAgreement), any(ContractOffer.class))).thenReturn(true);

        var result = manager.confirmed(token, negotiationConsumerOffered.getId(), contractAgreement, "hash");

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == CONFIRMED.code() &&
                negotiation.getContractAgreement() == contractAgreement
        ));
        verify(validationService).validate(eq(token), eq(contractAgreement), any(ContractOffer.class));
    }

    @Test
    void testConfirmedDeclineAgreement() {
        var negotiationConsumerOffered = createContractNegotiationConsumerOffered();
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);
        when(store.find(negotiationConsumerOffered.getId())).thenReturn(negotiationConsumerOffered);
        when(validationService.validate(eq(token), eq(contractAgreement), any(ContractOffer.class))).thenReturn(false);

        var result = manager.confirmed(token, negotiationConsumerOffered.getId(), contractAgreement, "hash");

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == DECLINING.code() &&
                negotiation.getContractAgreement() == null
        ));
        verify(validationService).validate(eq(token), eq(contractAgreement), any(ContractOffer.class));
    }

    @Test
    void testDeclined() {
        var negotiationConsumerOffered = createContractNegotiationConsumerOffered();
        var token = ClaimToken.Builder.newInstance().build();
        when(store.find(negotiationConsumerOffered.getId())).thenReturn(negotiationConsumerOffered);

        var result = manager.declined(token, negotiationConsumerOffered.getId());

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation -> negotiation.getState() == DECLINED.code()));
    }

    @Test
    void shouldRequestAnInitialNegotiation() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(INITIAL.code()).contractOffers(List.of(contractOffer())).build();
        var latch = new CountDownLatch(2);
        when(store.nextForState(eq(INITIAL.code()), anyInt())).thenReturn(List.of(negotiation), Collections.emptyList());
        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(store).save(any());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture("result"));
        when(store.find(negotiation.getId())).thenReturn(contractNegotiationBuilder().state(REQUESTING.code()).build());

        manager.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == REQUESTING.code()));
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == REQUESTED.code()));
    }

    @Test
    void shouldOfferConsumerOffering() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_OFFERING.code()).contractOffers(List.of(contractOffer())).build();
        var latch = new CountDownLatch(1);
        when(store.nextForState(eq(CONSUMER_OFFERING.code()), anyInt())).thenReturn(List.of(negotiation), Collections.emptyList());
        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(store).save(any());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture("result"));
        when(store.find(negotiation.getId())).thenReturn(contractNegotiationBuilder().state(CONSUMER_OFFERING.code()).build());

        manager.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == CONSUMER_OFFERED.code()));
    }

    @Test
    void shouldApproveConsumerApproving() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_APPROVING.code()).contractOffers(List.of(contractOffer())).build();
        var latch = new CountDownLatch(2);
        when(store.nextForState(eq(CONSUMER_APPROVING.code()), anyInt())).thenReturn(List.of(negotiation), Collections.emptyList());
        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(store).save(any());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture("result"));
        when(store.find(negotiation.getId())).thenReturn(contractNegotiationBuilder().state(CONSUMER_APPROVING.code()).build());

        manager.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == CONSUMER_APPROVING.code()));
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == CONSUMER_APPROVED.code()));
    }

    @Test
    void shouldDeclineDeclining() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(DECLINING.code()).contractOffers(List.of(contractOffer())).errorDetail("error").build();
        var latch = new CountDownLatch(2);
        when(store.nextForState(eq(DECLINING.code()), anyInt())).thenReturn(List.of(negotiation), Collections.emptyList());
        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(store).save(any());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture("result"));
        when(store.find(negotiation.getId())).thenReturn(contractNegotiationBuilder().state(DECLINING.code()).build());

        manager.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == DECLINING.code()));
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == DECLINED.code()));
    }

    private ContractNegotiation createContractNegotiationRequested() {
        var lastOffer = contractOffer();

        return contractNegotiationBuilder()
                .state(REQUESTED.code())
                .contractOffers(List.of(lastOffer))
                .build();
    }

    private ContractNegotiation createContractNegotiationConsumerOffered() {
        var lastOffer = contractOffer();

        return contractNegotiationBuilder()
                .state(CONSUMER_OFFERED.code())
                .contractOffers(List.of(lastOffer))
                .build();
    }

    private ContractNegotiation.Builder contractNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId("correlationId")
                .counterPartyId("connectorId")
                .counterPartyAddress("connectorAddress")
                .protocol("protocol")
                .state(UNSAVED.code())
                .stateTimestamp(Instant.now().toEpochMilli());
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance().id("id:id")
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().build())
                .build();
    }

}
