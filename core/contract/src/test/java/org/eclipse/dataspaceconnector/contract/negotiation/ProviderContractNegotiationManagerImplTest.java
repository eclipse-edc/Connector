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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.CONFIRMING_SENT;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.DECLINING;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.PROVIDER_OFFERED;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates.PROVIDER_OFFERING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderContractNegotiationManagerImplTest {

    private final String correlationId = "correlationId";
    private final ContractValidationService validationService = mock(ContractValidationService.class);
    private final ContractNegotiationStore store = mock(ContractNegotiationStore.class);
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock(RemoteMessageDispatcherRegistry.class);
    private final CommandQueue<ContractNegotiationCommand> queue = mock(CommandQueue.class);
    private final CommandRunner<ContractNegotiationCommand> runner = mock(CommandRunner.class);
    private ProviderContractNegotiationManagerImpl manager;

    @BeforeEach
    void setUp() throws Exception {
        when(queue.dequeue(anyInt())).thenReturn(new ArrayList<>());

        manager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
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
    void testRequestedConfirmOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validate(token, contractOffer)).thenReturn(Result.success(contractOffer));

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .correlationId("correlationId")
                .build();

        var result = manager.requested(token, request);

        assertThat(result.succeeded()).isTrue();
        verify(store, atLeastOnce()).save(argThat(negotiation ->
                negotiation.getState() == CONFIRMING.code() &&
                negotiation.getCounterPartyId().equals(request.getConnectorId()) &&
                negotiation.getCounterPartyAddress().equals(request.getConnectorAddress()) &&
                negotiation.getProtocol().equals(request.getProtocol()) &&
                negotiation.getCorrelationId().equals(request.getCorrelationId()) &&
                negotiation.getContractOffers().size() == 1 &&
                negotiation.getLastContractOffer().equals(contractOffer))
        );

        verify(validationService).validate(token, contractOffer);
    }

    @Test
    void testRequestedDeclineOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(validationService.validate(token, contractOffer)).thenReturn(Result.failure("error"));

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .correlationId("correlationId")
                .build();

        var result = manager.requested(token, request);

        assertThat(result.succeeded()).isTrue();
        verify(store, atLeastOnce()).save(argThat(negotiation ->
                negotiation.getState() == DECLINING.code() &&
                negotiation.getCounterPartyId().equals(request.getConnectorId()) &&
                negotiation.getCounterPartyAddress().equals(request.getConnectorAddress()) &&
                negotiation.getProtocol().equals(request.getProtocol()) &&
                negotiation.getCorrelationId().equals(request.getCorrelationId()) &&
                negotiation.getContractOffers().size() == 1 &&
                negotiation.getLastContractOffer().equals(contractOffer))
        );
        verify(validationService).validate(token, contractOffer);
    }

    @Test
    @Disabled
    void testRequestedCounterOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        var counterOffer = contractOffer();
        when(validationService.validate(token, contractOffer)).thenReturn(Result.success(null));

        ContractOfferRequest request = ContractOfferRequest.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("connectorAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .correlationId("correlationId")
                .build();

        var result = manager.requested(token, request);

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == PROVIDER_OFFERING.code() &&
                negotiation.getCounterPartyId().equals(request.getConnectorId()) &&
                negotiation.getCounterPartyAddress().equals(request.getConnectorAddress()) &&
                negotiation.getProtocol().equals(request.getProtocol()) &&
                negotiation.getCorrelationId().equals(request.getCorrelationId()) &&
                negotiation.getContractOffers().size() == 2 &&
                negotiation.getContractOffers().get(0).equals(contractOffer) &&
                negotiation.getContractOffers().get(1).equals(counterOffer) &&
                negotiation.getLastContractOffer().equals(contractOffer))
        );
        verify(validationService).validate(token, contractOffer);
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
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(store.findForCorrelationId(correlationId)).thenReturn(contractNegotiationBuilder().contractOffers(List.of(contractOffer)).build());
        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.success(contractOffer));

        var result = manager.offerReceived(token, correlationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        verify(store, atLeastOnce()).save(argThat(negotiation ->
                negotiation.getState() == CONFIRMING.code() &&
                negotiation.getContractOffers().size() == 2 &&
                negotiation.getContractOffers().get(0).equals(contractOffer)
        ));
        verify(validationService).validate(eq(token), eq(contractOffer), any(ContractOffer.class));
    }

    @Test
    void testOfferReceivedDeclineOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        when(store.findForCorrelationId(correlationId)).thenReturn(contractNegotiationBuilder().contractOffers(List.of(contractOffer)).build());
        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.failure("error"));

        var result = manager.offerReceived(token, correlationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == DECLINING.code() &&
                negotiation.getContractOffers().size() == 2 &&
                negotiation.getContractOffers().get(0).equals(contractOffer)
        ));
        verify(validationService).validate(eq(token), eq(contractOffer), any(ContractOffer.class));
    }

    @Test
    @Disabled
    void testOfferReceivedCounterOffer() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractOffer = contractOffer();
        var counterOffer = contractOffer();
        when(validationService.validate(eq(token), eq(contractOffer), any(ContractOffer.class)))
                .thenReturn(Result.success(null));

        var result = manager.offerReceived(token, correlationId, contractOffer, "hash");

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == PROVIDER_OFFERING.code() &&
                negotiation.getContractOffers().size() == 2 &&
                negotiation.getContractOffers().get(0).equals(contractOffer) &&
                negotiation.getContractOffers().get(1).equals(counterOffer)
        ));
        verify(validationService).validate(eq(token), eq(contractOffer), any(ContractOffer.class));
    }

    @Test
    void testConsumerApprovedInvalidId() {
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = (ContractAgreement) mock(ContractAgreement.class);

        var result = manager.consumerApproved(token, "not a valid id", contractAgreement, "hash");

        assertThat(result.getFailure().getStatus()).isEqualTo(NegotiationResult.Status.FATAL_ERROR);
    }

    @Test
    void testConsumerApprovedConfirmAgreement() {
        when(store.findForCorrelationId(correlationId)).thenReturn(createContractNegotiation());
        var token = ClaimToken.Builder.newInstance().build();
        var contractAgreement = (ContractAgreement) mock(ContractAgreement.class);

        var result = manager.consumerApproved(token, correlationId, contractAgreement, "hash");

        assertThat(result.succeeded()).isTrue();
        verify(store, atLeastOnce()).save(argThat(negotiation ->
                negotiation.getState() == CONFIRMING.code() &&
                negotiation.getContractAgreement() == null
        ));
    }

    @Test
    void testDeclined() {
        when(store.findForCorrelationId(correlationId)).thenReturn(createContractNegotiation());
        var token = ClaimToken.Builder.newInstance().build();

        var result = manager.declined(token, correlationId);

        assertThat(result.succeeded()).isTrue();
        verify(store, atLeastOnce()).save(argThat(negotiation -> negotiation.getState() == DECLINED.code()));
    }

    @Test
    void shouldOfferProviderOffering() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(PROVIDER_OFFERING.code()).contractOffers(List.of(contractOffer())).build();
        var latch = new CountDownLatch(2);
        when(store.nextForState(eq(PROVIDER_OFFERING.code()), anyInt())).thenReturn(List.of(negotiation), Collections.emptyList());
        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(store).save(any());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture("result"));
        when(store.find(negotiation.getId())).thenReturn(contractNegotiationBuilder().state(PROVIDER_OFFERING.code()).build());

        manager.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == PROVIDER_OFFERING.code()));
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == PROVIDER_OFFERED.code()));
    }

    @Test
    void shouldApproveConsumerApproving() throws InterruptedException {
        var negotiation = contractNegotiationBuilder().state(CONFIRMING.code()).contractOffers(List.of(contractOffer())).build();
        var latch = new CountDownLatch(2);
        when(store.nextForState(eq(CONFIRMING.code()), anyInt())).thenReturn(List.of(negotiation), Collections.emptyList());
        doAnswer(i -> {
            latch.countDown();
            return null;
        }).when(store).save(any());
        when(dispatcherRegistry.send(any(), any(), any())).thenReturn(completedFuture("result"));
        when(store.find(negotiation.getId())).thenReturn(contractNegotiationBuilder().state(CONFIRMING_SENT.code()).build());

        manager.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == CONFIRMING_SENT.code()));
        verify(store, atLeastOnce()).save(argThat(n -> n.getState() == CONFIRMED.code()));
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

    private ContractNegotiation createContractNegotiation() {
        var lastOffer = contractOffer();

        ContractNegotiation.Builder builder = contractNegotiationBuilder();
        return builder
                .contractOffers(List.of(lastOffer))
                .build();
    }

    private ContractNegotiation.Builder contractNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .counterPartyId("connectorId")
                .counterPartyAddress("connectorAddress")
                .protocol("protocol")
                .state(400)
                .stateCount(0)
                .stateTimestamp(Instant.now().toEpochMilli())
                .type(ContractNegotiation.Type.PROVIDER);
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id("id:id")
                .policy(Policy.Builder.newInstance().build())
                .asset(Asset.Builder.newInstance().build())
                .build();
    }

}
