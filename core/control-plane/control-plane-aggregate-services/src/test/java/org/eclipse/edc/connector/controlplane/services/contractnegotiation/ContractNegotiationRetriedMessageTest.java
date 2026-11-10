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

package org.eclipse.edc.connector.controlplane.services.contractnegotiation;

import org.eclipse.edc.connector.controlplane.contract.negotiation.NegotiationProcessorsImpl;
import org.eclipse.edc.connector.controlplane.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.ContractOfferId;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractnegotiation.InMemoryContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.isFinal;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Consumer and provider of a negotiation must end in the same state when a protocol message is delivered twice: the
 * HTTP client re-sends a message when the first attempt times out, even though the counter-party may still be
 * processing it.
 * <p>
 * The provider's state machine ({@link NegotiationProcessorsImpl}) and the consumer's protocol service
 * ({@link ContractNegotiationProtocolServiceImpl}) share one store, as in a runtime hosting both participants. The
 * dispatcher in between delivers the messages in-process and maps the consumer's failures like the DSP HTTP
 * dispatcher maps the HTTP status codes: 4xx is fatal, anything else can be retried.
 */
class ContractNegotiationRetriedMessageTest {

    private static final String CONSUMER_PID = "consumerPid";
    private static final String PROVIDER_PID = "providerPid";

    private final ParticipantContext consumer = ParticipantContext.Builder.newInstance()
            .id("consumer-context").identity("consumer").build();
    private final ContractNegotiationStore store = new InMemoryContractNegotiationStore(Clock.systemUTC(), CriterionOperatorRegistryImpl.ofDefaults());
    private final ConsumerLoopback loopback = new ConsumerLoopback();
    private ContractNegotiationProtocolService consumerService;
    private NegotiationProcessorsImpl providerProcessors;

    @BeforeEach
    void setUp() {
        ContractValidationService validationService = mock();
        when(validationService.validateRequest(any(ParticipantAgent.class), any(ContractNegotiation.class))).thenReturn(Result.success());
        ProtocolTokenValidator tokenValidator = mock();
        when(tokenValidator.verify(any(), any(), any(), any())).thenReturn(ServiceResult.success(new ParticipantAgent("provider", emptyMap(), emptyMap())));

        ProtocolRemoteMessageDispatcher dispatcher = mock();
        when(dispatcher.dispatch(any(), any(), any())).thenAnswer(i -> loopback.deliver(i.getArgument(2)));

        consumerService = new ContractNegotiationProtocolServiceImpl(store, new NoopTransactionContext(), validationService,
                mock(), tokenValidator, new ContractNegotiationObservableImpl(), mock(), mock());
        providerProcessors = new NegotiationProcessorsImpl(mock(), mock(), new ContractNegotiationObservableImpl(), store,
                mock(), Clock.systemUTC(), dispatcher, new EntityRetryProcessConfiguration(1, () -> new ExponentialWaitStrategy(0L)));

        store.save(negotiation(CONSUMER_PID, PROVIDER_PID, CONSUMER, VERIFIED.code(), consumer.getId()));
        store.save(negotiation(PROVIDER_PID, CONSUMER_PID, PROVIDER, FINALIZING.code(), "provider-context"));
    }

    @Test
    void shouldEndInTheSameState_whenRetriedFinalizationArrivesWhileTheFirstDeliveryIsStillProcessed() {
        // the first finalization is still being processed by the consumer, which holds the negotiation's lease, when
        // the provider re-sends the same message
        loopback.onFirstFinalization = message -> store.findByIdAndLease(CONSUMER_PID).getContent();

        providerProcessors.processFinalizing(store.findById(PROVIDER_PID)).join();

        // then the first delivery completes on the consumer side
        store.breakLease(loopback.firstFinalizationInFlight);
        assertThat(consumerService.notifyFinalized(consumer, loopback.firstFinalization, token()).succeeded()).isTrue();

        // and the provider's state machine carries on from wherever the retry left it
        runProviderToFinalState();

        var consumerState = store.findById(CONSUMER_PID).stateAsString();
        var providerState = store.findById(PROVIDER_PID).stateAsString();
        assertThat(providerState)
                .describedAs("provider %s must match consumer %s (provider error: %s)", providerState, consumerState,
                        store.findById(PROVIDER_PID).getErrorDetail())
                .isEqualTo(consumerState)
                .isEqualTo(FINALIZED.name());
    }

    @Test
    void shouldEndInTheSameState_whenRetriedFinalizationArrivesAfterTheFirstDeliveryCompleted() {
        // the first finalization is processed completely before the provider re-sends it
        loopback.onFirstFinalization = message -> {
            assertThat(consumerService.notifyFinalized(consumer, message, token()).succeeded()).isTrue();
            return null;
        };

        runProviderToFinalState();

        assertThat(store.findById(CONSUMER_PID).stateAsString()).isEqualTo(FINALIZED.name());
        assertThat(store.findById(PROVIDER_PID).stateAsString()).isEqualTo(FINALIZED.name());
    }

    private void runProviderToFinalState() {
        for (var i = 0; i < 5 && !isFinal(store.findById(PROVIDER_PID).getState()); i++) {
            var provider = store.findById(PROVIDER_PID);
            if (provider.getState() == FINALIZING.code()) {
                providerProcessors.processFinalizing(provider).join();
            } else if (provider.getState() == TERMINATING.code()) {
                providerProcessors.processTerminating(provider).join();
            } else {
                throw new IllegalStateException("unexpected provider state " + provider.stateAsString());
            }
        }
    }

    private ContractNegotiation negotiation(String id, String correlationId, ContractNegotiation.Type type, int state, String participantContextId) {
        return ContractNegotiation.Builder.newInstance()
                .id(id)
                .correlationId(correlationId)
                .type(type)
                .state(state)
                .counterPartyId("counter-party")
                .counterPartyAddress("http://counter-party")
                .protocol("protocol")
                .participantContextId(participantContextId)
                .contractOffer(ContractOffer.Builder.newInstance()
                        .id(ContractOfferId.create("1", "assetId").toString())
                        .policy(Policy.Builder.newInstance().build())
                        .assetId("assetId")
                        .build())
                .contractAgreement(ContractAgreement.Builder.newInstance()
                        .providerId("provider").consumerId("consumer").assetId("assetId")
                        .policy(Policy.Builder.newInstance().build())
                        .participantContextId(participantContextId)
                        .build())
                .stateTimestamp(Instant.now().toEpochMilli())
                .build();
    }

    private static TokenRepresentation token() {
        return TokenRepresentation.Builder.newInstance().token("token").build();
    }

    /**
     * Delivers the provider's messages to the consumer's protocol service, in place of the provider's
     * {@link ProtocolRemoteMessageDispatcher}, and maps its failures like {@code DspHttpRemoteMessageDispatcherImpl}
     * maps the HTTP status codes the counter-party answers with (see {@code DspRequestHandlerImpl}).
     */
    private class ConsumerLoopback {

        private Function<ContractNegotiationEventMessage, ContractNegotiation> onFirstFinalization = m -> null;
        private ContractNegotiationEventMessage firstFinalization;
        private ContractNegotiation firstFinalizationInFlight;

        CompletableFuture<StatusResult<Object>> deliver(ProtocolRemoteMessage message) {
            ServiceResult<?> result;
            if (message instanceof ContractNegotiationEventMessage finalization) {
                if (firstFinalization == null) {
                    firstFinalization = finalization;
                    firstFinalizationInFlight = onFirstFinalization.apply(finalization);
                }
                result = consumerService.notifyFinalized(consumer, finalization, token());
            } else if (message instanceof ContractNegotiationTerminationMessage termination) {
                result = consumerService.notifyTerminated(consumer, termination, token());
            } else {
                throw new IllegalArgumentException("unexpected message " + message.getClass().getSimpleName());
            }
            return completedFuture(toStatusResult(result));
        }

        private StatusResult<Object> toStatusResult(ServiceResult<?> result) {
            if (result.succeeded()) {
                return StatusResult.success(null);
            }
            var clientError = switch (result.reason()) {
                case BAD_REQUEST, UNAUTHORIZED, NOT_FOUND, CONFLICT -> true;
                default -> false;
            };
            return StatusResult.failure(clientError ? FATAL_ERROR : ERROR_RETRY,
                    "Counter Party responded with %s: %s".formatted(result.reason(), result.getFailureDetail()));
        }
    }
}
