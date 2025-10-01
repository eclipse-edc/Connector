/*
 *  Copyright (c) 2021 - 2022 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import org.eclipse.edc.connector.controlplane.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractNegotiationAck;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.ACCEPTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFYING;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsumerContractNegotiationManagerImplTest {

    public static final String PARTICIPANT_CONTEXT_ID = "participantContextId";
    private static final int RETRY_LIMIT = 1;
    private final ContractNegotiationStore store = mock();
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock();
    private final PolicyDefinitionStore policyStore = mock();
    private final ContractNegotiationListener listener = mock();
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry = mock();
    private final String protocolWebhookUrl = "http://protocol.webhook/url";
    private final ContractNegotiationPendingGuard pendingGuard = mock();
    private ConsumerContractNegotiationManagerImpl manager;

    @BeforeEach
    void setUp() {
        var observable = new ContractNegotiationObservableImpl();
        observable.registerListener(listener);

        manager = ConsumerContractNegotiationManagerImpl.Builder.newInstance()
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(mock(Monitor.class))
                .observable(observable)
                .store(store)
                .policyStore(policyStore)
                .entityRetryProcessConfiguration(new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L)))
                .dataspaceProfileContextRegistry(dataspaceProfileContextRegistry)
                .pendingGuard(pendingGuard)
                .build();
    }

    @Test
    void initiate_shouldSaveNewNegotiationInInitialState() {
        var contractOffer = contractOffer();

        var request = ContractRequest.Builder.newInstance()
                .counterPartyAddress("callbackAddress")
                .protocol("protocol")
                .contractOffer(contractOffer)
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                        .uri("local://test")
                        .build()))
                .build();

        var result = manager.initiate(new ParticipantContext(PARTICIPANT_CONTEXT_ID), request);

        assertThat(result.succeeded()).isTrue();
        verify(store).save(argThat(negotiation ->
                negotiation.getState() == INITIAL.code() &&
                        negotiation.getCounterPartyId().equals("providerId") &&
                        negotiation.getCounterPartyAddress().equals(request.getCounterPartyAddress()) &&
                        negotiation.getProtocol().equals(request.getProtocol()) &&
                        negotiation.getCorrelationId() == null &&
                        negotiation.getContractOffers().size() == 1 &&
                        negotiation.getLastContractOffer().equals(contractOffer) &&
                        negotiation.getCallbackAddresses().size() == 1));

        verify(listener).initiated(any());
    }

    @Test
    void initial_shouldTransitionRequesting() {
        var negotiation = contractNegotiationBuilder().state(INITIAL.code()).build();
        when(store.nextNotLeased(anyInt(), stateIs(INITIAL.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());

        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == REQUESTING.code()));
        });
    }

    @Test
    void requesting_shouldSendOfferAndTransitionRequested() {
        var negotiation = contractNegotiationBuilder().correlationId("correlationId").state(REQUESTING.code()).contractOffer(contractOffer()).build();
        when(store.nextNotLeased(anyInt(), stateIs(REQUESTING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        var ack = ContractNegotiationAck.Builder.newInstance().providerPid("providerPid").build();
        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(dataspaceProfileContextRegistry.getWebhook(any())).thenReturn(() -> protocolWebhookUrl);


        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == REQUESTED.code()));
            var captor = ArgumentCaptor.<ContractRequestMessage>captor();
            verify(dispatcherRegistry, only()).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), captor.capture());
            var message = captor.getValue();
            assertThat(message.getProcessId()).isEqualTo("correlationId");
            assertThat(message.getCallbackAddress()).isEqualTo(protocolWebhookUrl);
            assertThat(message.getType()).isEqualTo(ContractRequestMessage.Type.INITIAL);
            verify(listener).requested(any());
        });
    }

    @Test
    void requesting_shouldSendCounterOfferAndTransitionRequested() {
        var negotiation = contractNegotiationBuilder().correlationId("correlationId").state(REQUESTING.code())
                .contractOffer(contractOffer())
                .contractOffer(contractOffer())
                .build();
        when(store.nextNotLeased(anyInt(), stateIs(REQUESTING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        var ack = ContractNegotiationAck.Builder.newInstance().providerPid("providerPid").build();
        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(dataspaceProfileContextRegistry.getWebhook(any())).thenReturn(() -> protocolWebhookUrl);


        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == REQUESTED.code()));
            var captor = ArgumentCaptor.<ContractRequestMessage>captor();
            verify(dispatcherRegistry, only()).dispatch(any(), any(), captor.capture());
            var message = captor.getValue();
            assertThat(message.getProcessId()).isEqualTo("correlationId");
            assertThat(message.getCallbackAddress()).isEqualTo(protocolWebhookUrl);
            assertThat(message.getType()).isEqualTo(ContractRequestMessage.Type.COUNTER_OFFER);
            verify(listener).requested(any());
        });
    }

    @Test
    void requesting_shouldSendMessageWithId_whenCorrelationIdIsNull_toSupportOldProtocolVersion() {
        var negotiation = contractNegotiationBuilder().correlationId(null).state(REQUESTING.code()).contractOffer(contractOffer()).build();
        when(store.nextNotLeased(anyInt(), stateIs(REQUESTING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        var ack = ContractNegotiationAck.Builder.newInstance().providerPid("providerPid").build();
        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(dataspaceProfileContextRegistry.getWebhook(any())).thenReturn(() -> protocolWebhookUrl);

        manager.start();

        await().untilAsserted(() -> {
            var entityCaptor = ArgumentCaptor.<ContractNegotiation>captor();
            verify(store).save(entityCaptor.capture());
            var storedNegotiation = entityCaptor.getValue();
            assertThat(storedNegotiation.getState()).isEqualTo(REQUESTED.code());
            assertThat(storedNegotiation.getCorrelationId()).isEqualTo("providerPid");
            var messageCaptor = ArgumentCaptor.<ContractRequestMessage>captor();
            verify(dispatcherRegistry, only()).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), messageCaptor.capture());
            var message = messageCaptor.getValue();
            assertThat(message.getProcessId()).isEqualTo(negotiation.getId());
            assertThat(message.getCallbackAddress()).isEqualTo(protocolWebhookUrl);
            verify(listener).requested(any());
        });
    }

    @Test
    void requesting_shouldTransitionToTerminated_whenProtocolNotResolved() {
        var negotiation = contractNegotiationBuilder().correlationId("correlationId").state(REQUESTING.code()).contractOffer(contractOffer()).build();
        when(store.nextNotLeased(anyInt(), stateIs(REQUESTING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        var ack = ContractNegotiationAck.Builder.newInstance().providerPid("providerPid").build();
        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(dataspaceProfileContextRegistry.getWebhook(any())).thenReturn(null);


        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
            verifyNoInteractions(dispatcherRegistry);
            verify(listener).terminated(any());
        });
    }

    @Test
    void accepting_shouldSendAcceptedMessageAndTransitionToApproved() {
        var negotiation = contractNegotiationBuilder().state(ACCEPTING.code()).contractOffer(contractOffer()).build();
        when(store.nextNotLeased(anyInt(), stateIs(ACCEPTING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());

        var captor = ArgumentCaptor.forClass(ContractNegotiationEventMessage.class);
        when(dispatcherRegistry.dispatch(any(), any(), captor.capture())).thenReturn(completedFuture(StatusResult.success("any")));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);

        manager.start();

        await().untilAsserted(() -> {
            var message = captor.getValue();
            assertThat(message.getPolicy()).isNotNull();
            verify(store).save(argThat(p -> p.getState() == ACCEPTED.code()));
            verify(dispatcherRegistry, only()).dispatch(any(), any(), any());
            verify(listener).accepted(any());
        });
    }

    @Test
    void agreed_shouldTransitionToVerifying() {
        var negotiation = contractNegotiationBuilder().state(AGREED.code()).build();
        when(store.nextNotLeased(anyInt(), stateIs(AGREED.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(store.findById(negotiation.getId())).thenReturn(negotiation);

        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == VERIFYING.code()));
            verifyNoInteractions(dispatcherRegistry);
        });
    }

    @Test
    void verifying_shouldSendMessageAndTransitionToVerified() {
        var negotiation = contractNegotiationBuilder().state(VERIFYING.code()).contractAgreement(createContractAgreement()).build();
        when(store.nextNotLeased(anyInt(), stateIs(VERIFYING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));

        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == VERIFIED.code()));
            verify(dispatcherRegistry).dispatch(eq(PARTICIPANT_CONTEXT_ID), any(), isA(ContractAgreementVerificationMessage.class));
        });
    }

    @Test
    void terminating_shouldSendRejectionAndTransitionTerminated() {
        var negotiation = contractNegotiationBuilder().state(TERMINATING.code()).contractOffer(contractOffer()).build();
        negotiation.setErrorDetail("an error");
        when(store.nextNotLeased(anyInt(), stateIs(TERMINATING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);

        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
            verify(dispatcherRegistry, only()).dispatch(any(), any(), any());
            verify(listener).terminated(any());
        });
    }

    @Test
    void pendingGuard_shouldSetTheTransferPending_whenPendingGuardMatches() {
        when(pendingGuard.test(any())).thenReturn(true);
        var process = contractNegotiationBuilder().state(VERIFYING.code()).build();
        when(store.nextNotLeased(anyInt(), stateIs(VERIFYING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        await().untilAsserted(() -> {
            verify(pendingGuard).test(any());
            var captor = ArgumentCaptor.forClass(ContractNegotiation.class);
            verify(store).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getState()).isEqualTo(VERIFYING.code());
            assertThat(saved.isPending()).isTrue();
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DispatchFailureArguments.class)
    void dispatchException(ContractNegotiationStates starting, ContractNegotiationStates ending, CompletableFuture<StatusResult<Object>> result, UnaryOperator<ContractNegotiation.Builder> builderEnricher) {
        var negotiation = builderEnricher.apply(contractNegotiationBuilder().state(starting.code())).build();
        when(store.nextNotLeased(anyInt(), stateIs(starting.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.dispatch(any(), any(), any())).thenReturn(result);
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(dataspaceProfileContextRegistry.getWebhook(negotiation.getProtocol())).thenReturn(() -> protocolWebhookUrl);

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(ContractNegotiation.class);
            verify(store).save(captor.capture());
            assertThat(captor.getAllValues()).hasSize(1).first().satisfies(n -> {
                assertThat(n.getState()).isEqualTo(ending.code());
            });
            verify(dispatcherRegistry, only()).dispatch(any(), any(), any());
        });
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{hasState(state), isNotPending(), new Criterion("type", "=", "CONSUMER")});
    }

    private ContractNegotiation.Builder contractNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .correlationId("processId")
                .counterPartyId("connectorId")
                .counterPartyAddress("callbackAddress")
                .protocol("protocol")
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .stateTimestamp(Instant.now().toEpochMilli());
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance().id("id:assetId:random")
                .policy(Policy.Builder.newInstance().assigner("providerId").build())
                .assetId("assetId")
                .build();
    }

    private ContractAgreement createContractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id("contractId")
                .consumerId("consumerId")
                .providerId("providerId")
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private static class DispatchFailureArguments implements ArgumentsProvider {

        private static final int RETRIES_NOT_EXHAUSTED = RETRY_LIMIT;
        private static final int RETRIES_EXHAUSTED = RETRIES_NOT_EXHAUSTED + 1;

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    // retries not exhausted
                    new DispatchFailure(REQUESTING, REQUESTING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(ACCEPTING, ACCEPTING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(VERIFYING, VERIFYING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractAgreement(createContractAgreement())),
                    new DispatchFailure(TERMINATING, TERMINATING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).errorDetail("an error").contractOffer(contractOffer())),
                    // retries exhausted
                    new DispatchFailure(REQUESTING, TERMINATED, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(ACCEPTING, TERMINATING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(VERIFYING, TERMINATING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED).contractAgreement(createContractAgreement())),
                    new DispatchFailure(TERMINATING, TERMINATED, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED).errorDetail("an error").contractOffer(contractOffer())),
                    // fatal error, in this case retry should never be done
                    new DispatchFailure(REQUESTING, TERMINATED, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(ACCEPTING, TERMINATING, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(VERIFYING, TERMINATING, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractAgreement(createContractAgreement())),
                    new DispatchFailure(TERMINATING, TERMINATED, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).errorDetail("an error").contractOffer(contractOffer()))
            );
        }

        private ContractAgreement createContractAgreement() {
            return ContractAgreement.Builder.newInstance()
                    .id("contractId")
                    .consumerId("consumerId")
                    .providerId("providerId")
                    .assetId("assetId")
                    .policy(Policy.Builder.newInstance().build())
                    .build();
        }

        private ContractOffer contractOffer() {
            return ContractOffer.Builder.newInstance().id("id:assetId:random")
                    .policy(Policy.Builder.newInstance().build())
                    .assetId("assetId")
                    .build();
        }
    }


}
