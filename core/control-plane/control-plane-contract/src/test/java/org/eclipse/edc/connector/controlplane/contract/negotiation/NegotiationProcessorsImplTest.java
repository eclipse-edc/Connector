/*
 *  Copyright (c) 2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import org.eclipse.edc.connector.controlplane.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractNegotiationAck;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.OFFERED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.OFFERING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFYING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NegotiationProcessorsImplTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participantContextId";
    private static final int RETRY_LIMIT = 1;

    private final ContractNegotiationStore store = mock();
    private final ProtocolRemoteMessageDispatcher messageDispatcher = mock();
    private final ContractNegotiationListener listener = mock();
    private final ProtocolWebhookResolver protocolWebhookResolver = mock();
    private final ParticipantIdentityResolver identityResolver = mock();

    private NegotiationProcessorsImpl processors;

    @BeforeEach
    void setUp() {
        when(store.save(any())).thenReturn(StoreResult.success());
        var observable = new ContractNegotiationObservableImpl();
        observable.registerListener(listener);
        processors = new NegotiationProcessorsImpl(mock(), protocolWebhookResolver, observable, store,
                identityResolver, Clock.systemDefaultZone(), messageDispatcher,
                new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L)));
    }

    @Nested
    class ProcessInitial {

        @Test
        void shouldTransitionToRequesting() {
            var negotiation = consumerNegotiationBuilder().state(INITIAL.code()).build();

            processors.processInitial(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == REQUESTING.code()));
        }
    }

    @Nested
    class ProcessRequesting {

        @Test
        void shouldSendRequestAndTransitionToRequested() {
            var negotiation = consumerNegotiationBuilder().state(REQUESTING.code()).correlationId("correlationId").contractOffer(contractOffer()).build();
            var ack = ContractNegotiationAck.Builder.newInstance().providerPid("providerPid").build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback.url");
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processRequesting(negotiation).join();

            var captor = ArgumentCaptor.<ContractRequestMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), captor.capture());
            var message = captor.getValue();
            assertThat(message.getCallbackAddress()).isEqualTo("http://callback.url");
            assertThat(message.getType()).isEqualTo(ContractRequestMessage.Type.INITIAL);
            assertThat(message.getProcessId()).isEqualTo("correlationId");
            verify(store).save(argThat(n -> n.getState() == REQUESTED.code() && "providerPid".equals(n.getCorrelationId())));
            verify(listener).requested(negotiation);
        }

        @Test
        void shouldSendCounterOfferRequest_whenMoreThanOneOffer() {
            var negotiation = consumerNegotiationBuilder().state(REQUESTING.code()).correlationId("correlationId")
                    .contractOffer(contractOffer()).contractOffer(contractOffer()).build();
            var ack = ContractNegotiationAck.Builder.newInstance().providerPid("providerPid").build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback.url");
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processRequesting(negotiation).join();

            var captor = ArgumentCaptor.<ContractRequestMessage>captor();
            verify(messageDispatcher).dispatch(any(), any(), captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(ContractRequestMessage.Type.COUNTER_OFFER);
        }

        @Test
        void shouldTransitionToTerminated_whenNoWebhookFound() {
            var negotiation = consumerNegotiationBuilder().state(REQUESTING.code()).contractOffer(contractOffer()).build();
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(null);

            processors.processRequesting(negotiation).join();

            verifyNoInteractions(messageDispatcher);
            verify(store).save(argThat(n -> n.getState() == TERMINATED.code()));
            verify(listener).terminated(negotiation);
        }

        @Test
        void shouldTransitionToTerminated_whenRetriesExhausted() {
            var negotiation = consumerNegotiationBuilder().state(REQUESTING.code()).stateCount(RETRY_LIMIT + 1).contractOffer(contractOffer()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback.url");
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processRequesting(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == TERMINATED.code()));
        }

        @Test
        void shouldTransitionToRequesting_whenRetriesNotExhausted() {
            var negotiation = consumerNegotiationBuilder().state(REQUESTING.code()).stateCount(RETRY_LIMIT).contractOffer(contractOffer()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback.url");
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processRequesting(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == REQUESTING.code()));
        }
    }

    @Nested
    class ProcessAccepting {

        @Test
        void shouldSendAcceptedEventAndTransitionToAccepted() {
            var negotiation = consumerNegotiationBuilder().state(ACCEPTING.code()).contractOffer(contractOffer()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processAccepting(negotiation).join();

            var captor = ArgumentCaptor.<ContractNegotiationEventMessage>captor();
            verify(messageDispatcher).dispatch(any(), any(), captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(ContractNegotiationEventMessage.Type.ACCEPTED);
            assertThat(captor.getValue().getPolicy()).isNotNull();
            verify(store).save(argThat(n -> n.getState() == ACCEPTED.code()));
            verify(listener).accepted(negotiation);
        }

        @Test
        void shouldTransitionToTerminating_whenRetriesExhausted() {
            var negotiation = consumerNegotiationBuilder().state(ACCEPTING.code()).stateCount(RETRY_LIMIT + 1).contractOffer(contractOffer()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processAccepting(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessAgreed {

        @Test
        void shouldTransitionToVerifying() {
            var negotiation = consumerNegotiationBuilder().state(AGREED.code()).contractAgreement(contractAgreement()).build();

            processors.processAgreed(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == VERIFYING.code()));
            verifyNoInteractions(messageDispatcher);
        }
    }

    @Nested
    class ProcessVerifying {

        @Test
        void shouldSendVerificationAndTransitionToVerified() {
            var negotiation = consumerNegotiationBuilder().state(VERIFYING.code()).contractAgreement(contractAgreement()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processVerifying(negotiation).join();

            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), isA(ContractAgreementVerificationMessage.class));
            verify(store).save(argThat(n -> n.getState() == VERIFIED.code()));
            verify(listener).verified(negotiation);
        }

        @Test
        void shouldTransitionToTerminating_whenRetriesExhausted() {
            var negotiation = consumerNegotiationBuilder().state(VERIFYING.code()).stateCount(RETRY_LIMIT + 1).contractAgreement(contractAgreement()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processVerifying(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessOffering {

        @Test
        void shouldSendOfferAndTransitionToOffered() {
            var negotiation = providerNegotiationBuilder().state(OFFERING.code()).contractOffer(contractOffer()).build();
            var ack = ContractNegotiationAck.Builder.newInstance().consumerPid("consumerPid").build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback.url");
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processOffering(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == OFFERED.code() && "consumerPid".equals(n.getCorrelationId())));
            verify(listener).offered(negotiation);
        }

        @Test
        void shouldTransitionToTerminated_whenNoWebhookFound() {
            var negotiation = providerNegotiationBuilder().state(OFFERING.code()).contractOffer(contractOffer()).build();
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(null);

            processors.processOffering(negotiation).join();

            verifyNoInteractions(messageDispatcher);
            verify(store).save(argThat(n -> n.getState() == TERMINATED.code()));
            verify(listener).terminated(negotiation);
        }

        @Test
        void shouldTransitionToTerminating_whenRetriesExhausted() {
            var negotiation = providerNegotiationBuilder().state(OFFERING.code()).stateCount(RETRY_LIMIT + 1).contractOffer(contractOffer()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback.url");
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processOffering(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessRequested {

        @Test
        void shouldTransitionToAgreeing() {
            var negotiation = providerNegotiationBuilder().state(REQUESTED.code()).build();

            processors.processRequested(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == AGREEING.code()));
            verifyNoInteractions(messageDispatcher);
        }
    }

    @Nested
    class ProcessAccepted {

        @Test
        void shouldTransitionToAgreeing() {
            var negotiation = providerNegotiationBuilder().state(ACCEPTED.code()).build();

            processors.processAccepted(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == AGREEING.code()));
            verifyNoInteractions(messageDispatcher);
        }
    }

    @Nested
    class ProcessAgreeing {

        @Test
        void shouldCreateAgreementAndSendMessageAndTransitionToAgreed() {
            var negotiation = providerNegotiationBuilder().state(AGREEING.code()).contractOffer(contractOffer()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback.url");
            when(identityResolver.getParticipantId(any(), any())).thenReturn("providerId");
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processAgreeing(negotiation).join();

            var captor = ArgumentCaptor.<ContractAgreementMessage>captor();
            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), captor.capture());
            var message = captor.getValue();
            assertThat(message.getContractAgreement()).isNotNull();
            assertThat(message.getCallbackAddress()).isEqualTo("http://callback.url");
            var savedCaptor = ArgumentCaptor.<ContractNegotiation>captor();
            verify(store).save(savedCaptor.capture());
            var saved = savedCaptor.getValue();
            assertThat(saved.getState()).isEqualTo(AGREED.code());
            assertThat(saved.getContractAgreement()).isNotNull();
            assertThat(saved.getContractAgreement().getPolicy().getType()).isEqualTo(PolicyType.CONTRACT);
            verify(listener).agreed(negotiation);
        }

        @Test
        void shouldReuseExistingAgreement_whenAlreadySet() {
            var agreement = contractAgreement();
            var negotiation = providerNegotiationBuilder().state(AGREEING.code()).contractOffer(contractOffer()).contractAgreement(agreement).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback.url");
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processAgreeing(negotiation).join();

            var captor = ArgumentCaptor.<ContractAgreementMessage>captor();
            verify(messageDispatcher).dispatch(any(), any(), captor.capture());
            assertThat(captor.getValue().getContractAgreement()).isSameAs(agreement);
        }

        @Test
        void shouldTransitionToTerminated_whenNoWebhookFound() {
            var negotiation = providerNegotiationBuilder().state(AGREEING.code()).contractOffer(contractOffer()).build();
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(null);

            processors.processAgreeing(negotiation).join();

            verifyNoInteractions(messageDispatcher);
            verify(store).save(argThat(n -> n.getState() == TERMINATED.code()));
            verify(listener).terminated(negotiation);
        }

        @Test
        void shouldTransitionToTerminating_whenRetriesExhausted() {
            var negotiation = providerNegotiationBuilder().state(AGREEING.code()).stateCount(RETRY_LIMIT + 1).contractOffer(contractOffer()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> "http://callback.url");
            when(identityResolver.getParticipantId(any(), any())).thenReturn("providerId");
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processAgreeing(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessVerified {

        @Test
        void shouldTransitionToFinalizing() {
            var negotiation = providerNegotiationBuilder().state(VERIFIED.code()).build();

            processors.processVerified(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == FINALIZING.code()));
            verifyNoInteractions(messageDispatcher);
        }
    }

    @Nested
    class ProcessFinalizing {

        @Test
        void shouldSendFinalizedEventAndTransitionToFinalized() {
            var negotiation = providerNegotiationBuilder().state(FINALIZING.code()).contractOffer(contractOffer()).contractAgreement(contractAgreement()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processFinalizing(negotiation).join();

            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(),
                    argThat(m -> m instanceof ContractNegotiationEventMessage e && e.getType() == ContractNegotiationEventMessage.Type.FINALIZED));
            verify(store).save(argThat(n -> n.getState() == FINALIZED.code()));
            verify(listener).finalized(negotiation);
        }

        @Test
        void shouldTransitionToTerminating_whenRetriesExhausted() {
            var negotiation = providerNegotiationBuilder().state(FINALIZING.code()).stateCount(RETRY_LIMIT + 1).contractOffer(contractOffer()).contractAgreement(contractAgreement()).build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processFinalizing(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == TERMINATING.code()));
        }
    }

    @Nested
    class ProcessTerminating {

        @Test
        void shouldSendTerminationAndTransitionToTerminated_forConsumer() {
            var negotiation = consumerNegotiationBuilder().state(TERMINATING.code()).contractOffer(contractOffer()).errorDetail("an error").build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processTerminating(negotiation).join();

            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), isA(ContractNegotiationTerminationMessage.class));
            verify(store).save(argThat(n -> n.getState() == TERMINATED.code()));
            verify(listener).terminated(negotiation);
        }

        @Test
        void shouldSendTerminationAndTransitionToTerminated_forProvider() {
            var negotiation = providerNegotiationBuilder().state(TERMINATING.code()).contractOffer(contractOffer()).errorDetail("an error").build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processTerminating(negotiation).join();

            verify(messageDispatcher).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), isA(ContractNegotiationTerminationMessage.class));
            verify(store).save(argThat(n -> n.getState() == TERMINATED.code()));
            verify(listener).terminated(negotiation);
        }

        @Test
        void shouldTransitionToTerminated_whenRetriesExhausted() {
            var negotiation = consumerNegotiationBuilder().state(TERMINATING.code()).stateCount(RETRY_LIMIT + 1).contractOffer(contractOffer()).errorDetail("an error").build();
            when(messageDispatcher.dispatch(any(), any(), any())).thenReturn(failedFuture(new RuntimeException("error")));
            when(store.findById(negotiation.getId())).thenReturn(negotiation);

            processors.processTerminating(negotiation).join();

            verify(store).save(argThat(n -> n.getState() == TERMINATED.code()));
        }
    }

    private ContractNegotiation.Builder consumerNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(ContractNegotiation.Type.CONSUMER)
                .correlationId("correlationId")
                .counterPartyId("counterPartyId")
                .counterPartyAddress("http://counter.party/address")
                .protocol("protocol")
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .stateTimestamp(Instant.now().toEpochMilli());
    }

    private ContractNegotiation.Builder providerNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(ContractNegotiation.Type.PROVIDER)
                .correlationId("correlationId")
                .counterPartyId("counterPartyId")
                .counterPartyAddress("http://counter.party/address")
                .protocol("protocol")
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .stateTimestamp(Instant.now().toEpochMilli());
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id("id:assetId:random")
                .policy(Policy.Builder.newInstance().type(PolicyType.OFFER).assigner("providerId").build())
                .assetId("assetId")
                .build();
    }

    private ContractAgreement contractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id("contractId")
                .consumerId("consumerId")
                .providerId("providerId")
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }
}
