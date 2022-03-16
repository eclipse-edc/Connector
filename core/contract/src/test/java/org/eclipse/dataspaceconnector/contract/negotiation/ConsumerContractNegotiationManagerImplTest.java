/*
<<<<<<< HEAD
 *  Copyright (c) 2021-2022 Fraunhofer Institute for Software and Systems Engineering
=======
 *  Copyright (c) 2021 - 2022 Fraunhofer Institute for Software and Systems Engineering
>>>>>>> 9d998cb09 (core: add configuration setting to set state machine batch sizes)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */
package org.eclipse.dataspaceconnector.contract.negotiation;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.EdcException;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerContractNegotiationManagerImplTest {

    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private ConsumerContractNegotiationManagerImpl negotiationManager;

    @BeforeEach
    void setUp() {
        CommandQueue<ContractNegotiationCommand> queue = mock(CommandQueue.class);
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());

        CommandRunner<ContractNegotiationCommand> commandRunner = mock(CommandRunner.class);

        negotiationManager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .validationService(validationService)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(mock(Monitor.class))
                .commandQueue(queue)
                .commandRunner(commandRunner)
                .observable(mock(ContractNegotiationObservable.class))
                .store(store)
                .build();
    }

    @Test
    void initiateShouldSaveNewNegotiationInInitialState() {
        var contractOffer = contractOffer();

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
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
    }

    @Test
    void testOfferReceivedInvalidId() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();

        var result = negotiationManager.offerReceived(token, "not a valid id", contractOffer, "hash");

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

        var result = negotiationManager.offerReceived(token, negotiationRequested.getId(), contractOffer, "hash");

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

        var result = negotiationManager.offerReceived(token, negotiationRequested.getId(), contractOffer, "hash");

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

        var result = negotiationManager.confirmed(token, "not a valid id", contractAgreement, "hash");

        assertThat(result.getFailure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testConfirmedConfirmAgreement() {
        var negotiationConsumerOffered = createContractNegotiationConsumerOffered();
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = mock(ContractAgreement.class);
        when(store.find(negotiationConsumerOffered.getId())).thenReturn(negotiationConsumerOffered);
        when(validationService.validate(eq(token), eq(contractAgreement), any(ContractOffer.class))).thenReturn(true);

        var result = negotiationManager.confirmed(token, negotiationConsumerOffered.getId(), contractAgreement, "hash");

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

        var result = negotiationManager.confirmed(token, negotiationConsumerOffered.getId(), contractAgreement, "hash");

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

        var result = negotiationManager.declined(token, negotiationConsumerOffered.getId());

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation -> negotiation.getState() == DECLINED.code()));
    }

    @Test
    void initial_shouldTransitionRequesting() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(INITIAL.code()).build();
        when(store.nextForState(eq(INITIAL.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        var latch = countDownOnUpdateLatch();

        negotiationManager.start();

        assertThat(latch.await(5, SECONDS)).isTrue();
        verify(store).save(argThat(p -> p.getState() == REQUESTING.code()));
    }

    @Test
    void requesting_shouldSendOfferAndTransitionRequested() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(REQUESTING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        var latch = countDownOnUpdateLatch();

        negotiationManager.start();

        assertThat(latch.await(5, SECONDS)).isTrue();
        verify(store).save(argThat(p -> p.getState() == REQUESTED.code()));
        verify(dispatcherRegistry, only()).send(any(), any(), any());
    }

    @Test
    void requesting_shouldTransitionInitialIfSendFails() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(REQUESTING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(REQUESTING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        var latch = countDownOnUpdateLatch();

        negotiationManager.start();

        assertThat(latch.await(5, SECONDS)).isTrue();
        verify(store).save(argThat(p -> p.getState() == INITIAL.code()));
        verify(dispatcherRegistry, only()).send(any(), any(), any());
    }

    @Test
    void consumerOffering_shouldSendOfferAndTransitionOffered() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_OFFERING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_OFFERING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        var latch = countDownOnUpdateLatch();

        negotiationManager.start();

        assertThat(latch.await(5, SECONDS)).isTrue();
        verify(store).save(argThat(p -> p.getState() == CONSUMER_OFFERED.code()));
        verify(dispatcherRegistry, only()).send(any(), any(), any());
    }

    @Test
    void consumerOffering_shouldTransitionOfferingIfSendFails() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_OFFERING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_OFFERING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        var latch = countDownOnUpdateLatch();

        negotiationManager.start();

        assertThat(latch.await(5, SECONDS)).isTrue();
        verify(store).save(argThat(p -> p.getState() == CONSUMER_OFFERING.code()));
        verify(dispatcherRegistry, only()).send(any(), any(), any());
    }

    @Test
    void consumerApproving_shouldSendAgreementAndTransitionApproved() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_APPROVING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_APPROVING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        var latch = countDownOnUpdateLatch();

        negotiationManager.start();

        assertThat(latch.await(5, SECONDS)).isTrue();
        verify(store).save(argThat(p -> p.getState() == CONSUMER_APPROVED.code()));
        verify(dispatcherRegistry, only()).send(any(), any(), any());
    }

    @Test
    void consumerApproving_shouldTransitionApprovingIfSendFails() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(CONSUMER_APPROVING.code()).contractOffer(contractOffer()).build();
        when(store.nextForState(eq(CONSUMER_APPROVING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        var latch = countDownOnUpdateLatch();

        negotiationManager.start();

        assertThat(latch.await(5, SECONDS)).isTrue();
        verify(store).save(argThat(p -> p.getState() == CONSUMER_APPROVING.code()));
        verify(dispatcherRegistry, only()).send(any(), any(), any());
    }

    @Test
    void declining_shouldSendRejectionAndTransitionDeclined() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(DECLINING.code()).contractOffer(contractOffer()).build();
        negotiation.setErrorDetail("an error");
        when(store.nextForState(eq(DECLINING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture(null));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        var latch = countDownOnUpdateLatch();

        negotiationManager.start();

        assertThat(latch.await(5, SECONDS)).isTrue();
        verify(store).save(argThat(p -> p.getState() == DECLINED.code()));
        verify(dispatcherRegistry, only()).send(any(), any(), any());
    }

    @Test
    void declining_shouldTransitionDecliningIfSendFails() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(DECLINING.code()).contractOffer(contractOffer()).build();
        negotiation.setErrorDetail("an error");
        when(store.nextForState(eq(DECLINING.code()), anyInt())).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(failedFuture(new EdcException("error")));
        when(store.find(negotiation.getId())).thenReturn(negotiation);
        var latch = countDownOnUpdateLatch();

        negotiationManager.start();

        assertThat(latch.await(5, SECONDS)).isTrue();
        verify(store).save(argThat(p -> p.getState() == DECLINING.code()));
        verify(dispatcherRegistry, only()).send(any(), any(), any());
    }

    private CountDownLatch countDownOnUpdateLatch() {
        var latch = new CountDownLatch(1);

        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(store).save(any());

        return latch;
    }

    private ContractNegotiation createContractNegotiationRequested() {
        var lastOffer = contractOffer();

        return contractNegotiationBuilder()
                .state(REQUESTED.code())
                .contractOffer(lastOffer)
                .build();
    }

    private ContractNegotiation createContractNegotiationConsumerOffered() {
        var lastOffer = contractOffer();

        return contractNegotiationBuilder()
                .state(CONSUMER_OFFERED.code())
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
                .build();
    }

}
