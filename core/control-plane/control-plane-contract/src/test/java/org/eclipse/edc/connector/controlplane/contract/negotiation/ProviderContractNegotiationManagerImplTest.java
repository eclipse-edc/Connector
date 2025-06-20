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

package org.eclipse.edc.connector.controlplane.contract.negotiation;

import org.eclipse.edc.connector.controlplane.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.types.protocol.ContractNegotiationAck;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.protocol.ProtocolWebhookRegistry;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
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
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.OFFERED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.OFFERING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.policy.model.PolicyType.CONTRACT;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProviderContractNegotiationManagerImplTest {

    private static final String PROVIDER_ID = "provider";
    private static final int RETRY_LIMIT = 1;
    private final ContractNegotiationStore store = mock();
    private final RemoteMessageDispatcherRegistry dispatcherRegistry = mock();
    private final PolicyDefinitionStore policyStore = mock();
    private final ContractNegotiationListener listener = mock();
    private final ContractNegotiationPendingGuard pendingGuard = mock();
    private final ProtocolWebhookRegistry protocolWebhookRegistry = mock();
    private ProviderContractNegotiationManagerImpl manager;

    @BeforeEach
    void setUp() {
        var observable = new ContractNegotiationObservableImpl();
        observable.registerListener(listener);
        manager = ProviderContractNegotiationManagerImpl.Builder.newInstance()
                .participantId(PROVIDER_ID)
                .dispatcherRegistry(dispatcherRegistry)
                .monitor(mock())
                .observable(observable)
                .store(store)
                .policyStore(policyStore)
                .entityRetryProcessConfiguration(new EntityRetryProcessConfiguration(RETRY_LIMIT, () -> new ExponentialWaitStrategy(0L)))
                .pendingGuard(pendingGuard)
                .protocolWebhookRegistry(protocolWebhookRegistry)
                .build();
    }

    @Test
    void offering_shouldSendOfferAndTransitionToOffered() {
        var negotiation = contractNegotiationBuilder().state(OFFERING.code()).contractOffer(contractOffer()).build();
        when(store.nextNotLeased(anyInt(), stateIs(OFFERING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        var ack = ContractNegotiationAck.Builder.newInstance().consumerPid("consumerPid").build();
        when(dispatcherRegistry.dispatch(any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(protocolWebhookRegistry.resolve(negotiation.getProtocol())).thenReturn(() -> "http://callback.address");

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(ContractNegotiation.class);
            verify(store).save(captor.capture());
            var storedNegotiation = captor.getValue();
            assertThat(storedNegotiation.getState()).isEqualTo(OFFERED.code());
            assertThat(storedNegotiation.getCorrelationId()).isEqualTo("consumerPid");
            var messageCaptor = ArgumentCaptor.forClass(ContractOfferMessage.class);
            verify(dispatcherRegistry, only()).dispatch(any(), messageCaptor.capture());
            var message = messageCaptor.getValue();
            assertThat(message.getCallbackAddress()).isEqualTo("http://callback.address");
            assertThat(message.getPolicy().getType()).isEqualTo(PolicyType.OFFER);
            verify(listener).offered(any());
        });
    }

    @Test
    void offering_shouldTransitionToTerminated_whenProtocolNotResolved() {
        var negotiation = contractNegotiationBuilder().state(OFFERING.code()).contractOffer(contractOffer()).build();
        when(store.nextNotLeased(anyInt(), stateIs(OFFERING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        var ack = ContractNegotiationAck.Builder.newInstance().consumerPid("consumerPid").build();
        when(dispatcherRegistry.dispatch(any(), any())).thenReturn(completedFuture(StatusResult.success(ack)));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(protocolWebhookRegistry.resolve(negotiation.getProtocol())).thenReturn(null);

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(ContractNegotiation.class);
            verify(store).save(captor.capture());
            var storedNegotiation = captor.getValue();
            assertThat(storedNegotiation.getState()).isEqualTo(TERMINATED.code());
            assertThat(storedNegotiation.getErrorDetail()).isNotNull();
            verifyNoInteractions(dispatcherRegistry);
            verify(listener).terminated(any());
        });
    }

    @Test
    void requested_shouldTransitionToAgreeing() {
        var negotiation = contractNegotiationBuilder().state(REQUESTED.code()).build();
        when(store.nextNotLeased(anyInt(), stateIs(REQUESTED.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(store.findById(negotiation.getId())).thenReturn(negotiation);

        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == AGREEING.code()));
            verifyNoInteractions(dispatcherRegistry);
        });
    }

    @Test
    void accepted_shouldTransitionToAgreeing() {
        var negotiation = contractNegotiationBuilder().state(ACCEPTED.code()).build();
        when(store.nextNotLeased(anyInt(), stateIs(ACCEPTED.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(store.findById(negotiation.getId())).thenReturn(negotiation);

        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == AGREEING.code()));
            verifyNoInteractions(dispatcherRegistry);
        });
    }

    @Test
    void verified_shouldTransitionToFinalizing() {
        var negotiation = contractNegotiationBuilder().state(VERIFIED.code()).build();
        when(store.nextNotLeased(anyInt(), stateIs(VERIFIED.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(store.findById(negotiation.getId())).thenReturn(negotiation);

        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == FINALIZING.code()));
            verifyNoInteractions(dispatcherRegistry);
        });
    }

    @Test
    void agreeing_shouldSendAgreementAndTransitionToConfirmed() {
        var negotiation = contractNegotiationBuilder()
                .state(AGREEING.code())
                .contractOffer(contractOffer())
                .build();
        when(store.nextNotLeased(anyInt(), stateIs(AGREEING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.dispatch(any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(policyStore.findById(any())).thenReturn(PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).id("policyId").build());
        when(protocolWebhookRegistry.resolve(negotiation.getProtocol())).thenReturn(() -> "http://callback.address");

        manager.start();

        await().untilAsserted(() -> {
            var messageCaptor = ArgumentCaptor.forClass(ContractAgreementMessage.class);
            verify(dispatcherRegistry, only()).dispatch(any(), messageCaptor.capture());
            assertThat(messageCaptor.getValue().getPolicy().getType()).isEqualTo(CONTRACT);
            var storedCaptor = ArgumentCaptor.forClass(ContractNegotiation.class);
            verify(store).save(storedCaptor.capture());
            var stored = storedCaptor.getValue();
            assertThat(stored.getState()).isEqualTo(AGREED.code());
            assertThat(stored.getContractAgreement().getPolicy().getType()).isEqualTo(CONTRACT);
            assertThat(stored.getContractAgreement().getPolicy().getAssignee()).isEqualTo(stored.getContractAgreement().getConsumerId());
            assertThat(stored.getContractAgreement().getPolicy().getAssigner()).isEqualTo(stored.getContractAgreement().getProviderId());
            verify(listener).agreed(any());
        });
    }

    @Test
    void finalizing_shouldSendMessageAndTransitionToFinalized() {
        var negotiation = contractNegotiationBuilder().state(FINALIZING.code()).contractOffer(contractOffer()).contractAgreement(contractAgreementBuilder().build()).build();
        when(store.nextNotLeased(anyInt(), stateIs(FINALIZING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(dispatcherRegistry.dispatch(any(), any())).thenReturn(completedFuture(StatusResult.success("any")));

        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == FINALIZED.code()));
            verify(dispatcherRegistry).dispatch(any(), and(isA(ContractNegotiationEventMessage.class), argThat(it -> it.getType() == ContractNegotiationEventMessage.Type.FINALIZED)));
            verify(listener).finalized(negotiation);
        });
    }

    @Test
    void terminating_shouldSendMessageAndTransitionTerminated() {
        var negotiation = contractNegotiationBuilder().state(TERMINATING.code()).contractOffer(contractOffer()).errorDetail("an error").build();
        when(store.nextNotLeased(anyInt(), stateIs(TERMINATING.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.dispatch(any(), any())).thenReturn(completedFuture(StatusResult.success("any")));
        when(store.findById(negotiation.getId())).thenReturn(negotiation);

        manager.start();

        await().untilAsserted(() -> {
            verify(store).save(argThat(p -> p.getState() == TERMINATED.code()));
            verify(dispatcherRegistry, only()).dispatch(any(), any());
            verify(listener).terminated(any());
        });
    }

    @Test
    void pendingGuard_shouldSetTheTransferPending_whenPendingGuardMatches() {
        when(pendingGuard.test(any())).thenReturn(true);
        var process = contractNegotiationBuilder().state(AGREEING.code()).build();
        when(store.nextNotLeased(anyInt(), stateIs(AGREEING.code()))).thenReturn(List.of(process)).thenReturn(emptyList());

        manager.start();

        await().untilAsserted(() -> {
            verify(pendingGuard).test(any());
            var captor = ArgumentCaptor.forClass(ContractNegotiation.class);
            verify(store).save(captor.capture());
            var saved = captor.getValue();
            assertThat(saved.getState()).isEqualTo(AGREEING.code());
            assertThat(saved.isPending()).isTrue();
        });
    }

    @ParameterizedTest
    @ArgumentsSource(DispatchFailureArguments.class)
    void dispatchException(ContractNegotiationStates starting, ContractNegotiationStates ending, CompletableFuture<StatusResult<Object>> result, UnaryOperator<ContractNegotiation.Builder> builderEnricher) {
        var negotiation = builderEnricher.apply(contractNegotiationBuilder().state(starting.code())).build();
        when(store.nextNotLeased(anyInt(), stateIs(starting.code()))).thenReturn(List.of(negotiation)).thenReturn(emptyList());
        when(dispatcherRegistry.dispatch(any(), any())).thenReturn(result);
        when(store.findById(negotiation.getId())).thenReturn(negotiation);
        when(protocolWebhookRegistry.resolve(negotiation.getProtocol())).thenReturn(() -> "http://callback.address");

        manager.start();

        await().untilAsserted(() -> {
            var captor = ArgumentCaptor.forClass(ContractNegotiation.class);
            verify(store).save(captor.capture());
            assertThat(captor.getAllValues()).hasSize(1).first().satisfies(n -> {
                assertThat(n.getState()).isEqualTo(ending.code());
            });
            verify(dispatcherRegistry, only()).dispatch(any(), any());
        });
    }

    private ContractNegotiation.Builder contractNegotiationBuilder() {
        return ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(ContractNegotiation.Type.PROVIDER)
                .correlationId("processId")
                .counterPartyId("connectorId")
                .counterPartyAddress("callbackAddress")
                .protocol("protocol")
                .state(400)
                .stateTimestamp(Instant.now().toEpochMilli());
    }

    private ContractAgreement.Builder contractAgreementBuilder() {
        return ContractAgreement.Builder.newInstance()
                .id(ContractOfferId.create(UUID.randomUUID().toString(), "test-asset-id").toString())
                .providerId("any")
                .consumerId("any")
                .assetId("default")
                .policy(Policy.Builder.newInstance().build());
    }

    private ContractOffer contractOffer() {
        return ContractOffer.Builder.newInstance()
                .id(ContractOfferId.create("1", "test-asset-id").toString())
                .policy(Policy.Builder.newInstance().type(PolicyType.OFFER).build())
                .assetId("assetId")
                .build();
    }

    private Criterion[] stateIs(int state) {
        return aryEq(new Criterion[]{hasState(state), isNotPending(), new Criterion("type", "=", "PROVIDER")});
    }

    private static class DispatchFailureArguments implements ArgumentsProvider {

        private static final int RETRIES_NOT_EXHAUSTED = RETRY_LIMIT;
        private static final int RETRIES_EXHAUSTED = RETRIES_NOT_EXHAUSTED + 1;

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    // retries not exhausted
                    new DispatchFailure(OFFERING, OFFERING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(AGREEING, AGREEING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(FINALIZING, FINALIZING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer()).contractAgreement(createContractAgreement())),
                    new DispatchFailure(TERMINATING, TERMINATING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).errorDetail("an error").contractOffer(contractOffer())),
                    // retries exhausted
                    new DispatchFailure(OFFERING, TERMINATING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(AGREEING, TERMINATING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(FINALIZING, TERMINATING, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED).contractOffer(contractOffer()).contractAgreement(createContractAgreement())),
                    new DispatchFailure(TERMINATING, TERMINATED, failedFuture(new EdcException("error")), b -> b.stateCount(RETRIES_EXHAUSTED).errorDetail("an error").contractOffer(contractOffer())),
                    // fatal error, in this case retry should never be done
                    new DispatchFailure(OFFERING, TERMINATING, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(AGREEING, TERMINATING, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer())),
                    new DispatchFailure(FINALIZING, TERMINATING, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).contractOffer(contractOffer()).contractAgreement(createContractAgreement())),
                    new DispatchFailure(TERMINATING, TERMINATED, completedFuture(StatusResult.failure(FATAL_ERROR)), b -> b.stateCount(RETRIES_NOT_EXHAUSTED).errorDetail("an error").contractOffer(contractOffer()))
            );
        }

        private ContractOffer contractOffer() {
            return ContractOffer.Builder.newInstance().id("id:assetId:random")
                    .policy(Policy.Builder.newInstance().build())
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

    }

}
