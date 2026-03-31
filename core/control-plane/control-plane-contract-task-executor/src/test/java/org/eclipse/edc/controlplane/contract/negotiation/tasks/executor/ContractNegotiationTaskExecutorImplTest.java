/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.controlplane.contract.negotiation.tasks.executor;

import org.eclipse.edc.connector.controlplane.contract.negotiation.NegotiationProcessorsImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationProcessors;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractNegotiationAck;
import org.eclipse.edc.controlplane.contract.spi.negotiation.ContractNegotiationTaskExecutor;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.AgreeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.ContractNegotiationTaskPayload;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.FinalizeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.RequestNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendAgreement;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendFinalizeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendRequestNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendVerificationNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.VerifyNegotiation;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.participantcontext.spi.identity.ParticipantIdentityResolver;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFYING;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractNegotiationTaskExecutorImplTest {

    private final Clock clock = Clock.systemUTC();
    private final Monitor monitor = mock();
    private final ContractNegotiationStore negotiationStore = mock();
    private final TaskService taskService = mock();
    private final ContractNegotiationPendingGuard pendingGuard = mock();
    private final TransactionContext transactionContext = new NoopTransactionContext();
    private final ProtocolWebhookResolver protocolWebhookResolver = mock();
    private final ParticipantIdentityResolver identityResolver = mock();
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock();
    private final EntityRetryProcessConfiguration retryConfig = new EntityRetryProcessConfiguration(1, () -> new ExponentialWaitStrategy(0L));
    private final NegotiationProcessors negotiationProcessors = new NegotiationProcessorsImpl(monitor, protocolWebhookResolver, mock(), negotiationStore, identityResolver, clock, dispatcherRegistry, retryConfig);
    private final String protocolWebhookUrl = "http://protocol.webhook/url";
    private ContractNegotiationTaskExecutor executor;

    @BeforeEach
    void setUp() {

        when(negotiationStore.save(any())).thenReturn(StoreResult.success());
        when(pendingGuard.test(any())).thenReturn(false);
        when(protocolWebhookResolver.getWebhook(any(), any())).thenReturn(() -> protocolWebhookUrl);
        var ack = ContractNegotiationAck.Builder.newInstance().providerPid("providerPid").build();
        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));


        executor = ContractNegotiationTaskExecutorImpl.Builder.newInstance()
                .store(negotiationStore)
                .taskService(taskService)
                .pendingGuard(pendingGuard)
                .transactionContext(transactionContext)
                .negotiationProcessors(negotiationProcessors)
                .monitor(monitor)
                .clock(clock)
                .build();
    }

    @ParameterizedTest
    @ArgumentsSource(StateTransitionProvider.class)
    void handle(ContractNegotiationTaskPayload payload, ContractNegotiationStates expectedState) {

        var negotiation = createContractNegotiation(payload.getProcessId(), ContractNegotiationStates.from(payload.getProcessState()),
                ContractNegotiation.Type.valueOf(payload.getProcessType()));

        when(negotiationStore.findById(payload.getProcessId())).thenReturn(negotiation);


        var result = executor.handle(payload);

        assertThat(result.succeeded()).isTrue();

        var captor = ArgumentCaptor.forClass(ContractNegotiation.class);
        verify(negotiationStore).save(captor.capture());
        var savedNegotiation = captor.getValue();
        assertThat(savedNegotiation.stateAsString()).isEqualTo(expectedState.name());
    }

    @Test
    void handle_shouldSkipWhenNegotiationNotFound() {
        var task = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-123")
                .processState(INITIAL.code())
                .processType(CONSUMER.name())
                .build();

        when(negotiationStore.findById("negotiation-123")).thenReturn(null);

        var result = executor.handle(task);

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("not found");
    }

    @Test
    void handle_shouldSkipWhenNegotiationIsInFinalState() {
        var negotiation = createContractNegotiation("negotiation-123", ContractNegotiationStates.TERMINATED);

        var task = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-123")
                .processState(INITIAL.code())
                .processType(CONSUMER.name())
                .build();

        when(negotiationStore.findById("negotiation-123")).thenReturn(negotiation);

        var result = executor.handle(task);

        assertThat(result.succeeded()).isTrue();
        verify(taskService, never()).create(any());
    }

    @Test
    void handle_shouldSkipWhenStateDoesNotMatch() {
        var negotiation = createContractNegotiation("negotiation-123", REQUESTING);

        var task = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-123")
                .processState(INITIAL.code())
                .processType(CONSUMER.name())
                .build();

        when(negotiationStore.findById("negotiation-123")).thenReturn(negotiation);

        var result = executor.handle(task);

        assertThat(result.succeeded()).isTrue();
        verify(taskService, never()).create(any());
    }

    @Test
    void handle_shouldSkipWhenPendingGuardMatches() {
        var negotiation = createContractNegotiation("negotiation-123", INITIAL);

        var task = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-123")
                .processState(INITIAL.code())
                .processType(CONSUMER.name())
                .build();

        when(negotiationStore.findById("negotiation-123")).thenReturn(negotiation);
        when(pendingGuard.test(negotiation)).thenReturn(true);

        var result = executor.handle(task);

        assertThat(result.succeeded()).isTrue();
        verify(taskService, never()).create(any());
    }

    @Test
    void handle_shouldSkipWhenNegotiationTypeDoesNotMatch() {
        var negotiation = createContractNegotiation("negotiation-123", INITIAL, PROVIDER);

        var task = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-123")
                .processState(INITIAL.code())
                .processType(CONSUMER.name())
                .build();

        when(negotiationStore.findById("negotiation-123")).thenReturn(negotiation);

        var result = executor.handle(task);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void handle_shouldTransitionToTerminatedOnFatalError() {
        var negotiation = createContractNegotiation("negotiation-123", REQUESTING);


        var task = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-123")
                .processState(REQUESTING.code())
                .processType(CONSUMER.name())
                .build();

        when(negotiationStore.findById("negotiation-123")).thenReturn(negotiation);

        var result = executor.handle(task);

        assertThat(result.succeeded()).isTrue();
        verify(negotiationStore).save(any());
    }

    private ContractNegotiation createContractNegotiation(String id, ContractNegotiationStates state) {
        return createContractNegotiation(id, state, CONSUMER);
    }

    private ContractNegotiation createContractNegotiation(String id, ContractNegotiationStates state, ContractNegotiation.Type type) {
        return ContractNegotiation.Builder.newInstance()
                .id(id)
                .type(type)
                .state(state.code())
                .participantContextId("participant-123")
                .protocol("DSP")
                .counterPartyAddress("http://counter-party")
                .counterPartyId("counter-party-id")
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id("offer-123")
                        .assetId("asset-123")
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .contractAgreement(ContractAgreement.Builder.newInstance()
                        .agreementId("agreement-123")
                        .assetId("asset-123")
                        .policy(Policy.Builder.newInstance().build())
                        .consumerId("consumer-123")
                        .providerId("provider-123")
                        .build())
                .build();
    }

    public static class StateTransitionProvider implements ArgumentsProvider {

        protected <T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> B baseBuilder(B builder, String id, ContractNegotiationStates state, ContractNegotiation.Type type) {
            return builder.processId(id)
                    .processState(state.code())
                    .processType(type.name());
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            return Stream.of(
                    arguments(baseBuilder(RequestNegotiation.Builder.newInstance(), "id", INITIAL, CONSUMER).build(), REQUESTING),
                    arguments(baseBuilder(SendRequestNegotiation.Builder.newInstance(), "id", REQUESTING, CONSUMER).build(), REQUESTED),
                    arguments(baseBuilder(VerifyNegotiation.Builder.newInstance(), "id", AGREED, CONSUMER).build(), VERIFYING),
                    arguments(baseBuilder(SendVerificationNegotiation.Builder.newInstance(), "id", VERIFYING, CONSUMER).build(), VERIFIED),
                    arguments(baseBuilder(AgreeNegotiation.Builder.newInstance(), "id", REQUESTED, PROVIDER).build(), AGREEING),
                    arguments(baseBuilder(SendAgreement.Builder.newInstance(), "id", AGREEING, PROVIDER).build(), AGREED),
                    arguments(baseBuilder(FinalizeNegotiation.Builder.newInstance(), "id", VERIFIED, PROVIDER).build(), FINALIZING),
                    arguments(baseBuilder(SendFinalizeNegotiation.Builder.newInstance(), "id", FINALIZING, PROVIDER).build(), FINALIZED)
            );
        }
    }
}
