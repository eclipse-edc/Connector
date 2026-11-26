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

package org.eclipse.edc.connector.controlplane.services.transferprocess;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.controlplane.defaults.storage.transferprocess.InMemoryTransferProcessStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.controlplane.transfer.observe.TransferProcessObservableImpl;
import org.eclipse.edc.connector.controlplane.transfer.processors.TransferProcessorsImpl;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.participant.spi.ParticipantAgent;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTUP_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING_REQUESTED;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The transfer process counterpart of {@code ContractNegotiationRetriedMessageTest}: the provider's state machine
 * ({@link TransferProcessorsImpl}) re-sends its completion while the consumer's protocol service
 * ({@link TransferProcessProtocolServiceImpl}) is still processing the first delivery.
 */
class TransferProcessRetriedMessageTest {

    private static final String CONSUMER_PID = "consumerPid";
    private static final String PROVIDER_PID = "providerPid";
    private static final String AGREEMENT_ID = "agreementId";

    private final ParticipantContext consumer = ParticipantContext.Builder.newInstance()
            .id("consumer-context").identity("consumer").build();
    private final TransferProcessStore store = new InMemoryTransferProcessStore(Clock.systemUTC(), CriterionOperatorRegistryImpl.ofDefaults());
    private TransferProcessProtocolService consumerService;
    private TransferProcessorsImpl providerProcessors;
    private TransferCompletionMessage firstCompletion;

    @BeforeEach
    void setUp() {
        ContractNegotiationStore negotiationStore = mock();
        when(negotiationStore.findContractAgreement(any(), any())).thenReturn(ContractAgreement.Builder.newInstance()
                .id(AGREEMENT_ID).providerId("provider").consumerId("consumer").assetId("assetId")
                .policy(Policy.Builder.newInstance().build()).participantContextId(consumer.getId())
                .build());
        ContractValidationService validationService = mock();
        when(validationService.validateRequest(any(ParticipantAgent.class), any(ContractAgreement.class))).thenReturn(Result.success());
        ProtocolTokenValidator tokenValidator = mock();
        when(tokenValidator.verify(any(), any(), any(), any())).thenReturn(ServiceResult.success(new ParticipantAgent("provider", emptyMap(), emptyMap())));
        DataAddressStore dataAddressStore = mock();
        when(dataAddressStore.remove(any())).thenReturn(StoreResult.success());
        when(dataAddressStore.resolve(any())).thenReturn(StoreResult.notFound("no data address"));
        PolicyArchive policyArchive = mock();
        when(policyArchive.findPolicyForContract(any())).thenReturn(Policy.Builder.newInstance().assigner("provider").assignee("consumer").build());

        DataFlowController dataFlowController = mock();
        when(dataFlowController.suspend(any())).thenReturn(StatusResult.success());
        ProtocolRemoteMessageDispatcher dispatcher = mock();
        when(dispatcher.dispatch(any(), any(), any())).thenAnswer(i -> deliver(i.getArgument(2)));

        consumerService = new TransferProcessProtocolServiceImpl(store, new NoopTransactionContext(), negotiationStore, validationService,
                tokenValidator, new TransferProcessObservableImpl(), mock(), mock(), mock(), mock());
        var retryProcessFactory = new EntityRetryProcessFactory(mock(), Clock.systemUTC(),
                new EntityRetryProcessConfiguration(1, () -> new ExponentialWaitStrategy(0L)));
        providerProcessors = new TransferProcessorsImpl(policyArchive, retryProcessFactory, dataFlowController, dataAddressStore,
                new TransferProcessObservableImpl(), store, mock(), mock(), mock(), dispatcher);

        store.save(transferProcess(CONSUMER_PID, PROVIDER_PID, CONSUMER, STARTED.code(), consumer.getId()));
        store.save(transferProcess(PROVIDER_PID, CONSUMER_PID, PROVIDER, COMPLETING.code(), "provider-context"));
    }

    @Test
    void shouldComplete_whenRetriedCompletionArrivesWhileTheFirstDeliveryIsStillProcessed() {
        // the consumer still holds the lease of the first delivery when the provider re-sends the completion
        var inFlight = store.findByIdAndLease(CONSUMER_PID).getContent();

        providerProcessors.processCompleting(store.findById(PROVIDER_PID)).join();

        // then the first delivery completes on the consumer side
        store.breakLease(inFlight);
        assertThat(consumerService.notifyCompleted(consumer, firstCompletion, token()).succeeded()).isTrue();

        // and the provider's next attempt is ignored by the consumer as already received
        providerProcessors.processCompleting(store.findById(PROVIDER_PID)).join();

        assertThat(store.findById(PROVIDER_PID).stateAsString())
                .describedAs("provider error: %s", store.findById(PROVIDER_PID).getErrorDetail())
                .isEqualTo(COMPLETED.name());
        assertThat(store.findById(CONSUMER_PID).stateAsString()).isEqualTo(COMPLETING_REQUESTED.name());
    }

    @Test
    void shouldStart_whenStartArrivesBeforeTheConsumerReceivedTheAcknowledgementOfItsRequest() {
        // ids of their own: the processes saved by setUp can't be overwritten with a new version
        var consumerPid = "late-consumerPid";
        var providerPid = "late-providerPid";
        // the consumer sent its request, but has not received the acknowledgement yet, when the provider starts
        store.save(transferProcess(consumerPid, providerPid, CONSUMER, REQUESTING.code(), consumer.getId()));
        store.save(transferProcess(providerPid, consumerPid, PROVIDER, STARTING.code(), "provider-context"));

        providerProcessors.processStarting(store.findById(providerPid)).join();

        // the consumer answers that it isn't ready yet: the provider keeps on starting instead of terminating
        assertThat(store.findById(providerPid).stateAsString()).describedAs("provider error: %s, consumer state: %s", store.findById(providerPid).getErrorDetail(), store.findById(consumerPid).stateAsString()).isEqualTo(STARTING.name());
        assertThat(store.findById(consumerPid).stateAsString()).isEqualTo(REQUESTING.name());

        // then the consumer receives the acknowledgement of its request
        var requesting = store.findById(consumerPid);
        requesting.transitionRequested();
        store.save(requesting);

        // and the provider's next attempt is accepted
        providerProcessors.processStarting(store.findById(providerPid)).join();

        assertThat(store.findById(providerPid).stateAsString())
                .describedAs("provider error: %s", store.findById(providerPid).getErrorDetail())
                .isEqualTo(STARTED.name());
        assertThat(store.findById(consumerPid).stateAsString()).isEqualTo(STARTUP_REQUESTED.name());
    }

    @Test
    void shouldComplete_whenCompletionArrivesWhileTheConsumerIsStartingUp() {
        var consumerPid = "startup-consumerPid";
        var providerPid = "startup-providerPid";
        // the consumer accepted the start, but has not notified its data plane yet, when the provider completes
        store.save(transferProcess(consumerPid, providerPid, CONSUMER, STARTUP_REQUESTED.code(), consumer.getId()));
        store.save(transferProcess(providerPid, consumerPid, PROVIDER, COMPLETING.code(), "provider-context"));

        providerProcessors.processCompleting(store.findById(providerPid)).join();

        // the consumer answers that it isn't ready yet: the provider keeps on completing instead of terminating
        assertThat(store.findById(providerPid).stateAsString()).describedAs("provider error: %s", store.findById(providerPid).getErrorDetail()).isEqualTo(COMPLETING.name());
        assertThat(store.findById(consumerPid).stateAsString()).isEqualTo(STARTUP_REQUESTED.name());

        // then the consumer finishes its start-up
        transitionStarted(consumerPid);

        // and the provider's next attempt is accepted
        providerProcessors.processCompleting(store.findById(providerPid)).join();

        assertThat(store.findById(providerPid).stateAsString())
                .describedAs("provider error: %s", store.findById(providerPid).getErrorDetail())
                .isEqualTo(COMPLETED.name());
        assertThat(store.findById(consumerPid).stateAsString()).isEqualTo(COMPLETING_REQUESTED.name());
    }

    @Test
    void shouldSuspend_whenSuspensionArrivesWhileTheConsumerIsStartingUp() {
        var consumerPid = "startup-consumerPid";
        var providerPid = "startup-providerPid";
        // the consumer accepted the start, but has not notified its data plane yet, when the provider suspends
        store.save(transferProcess(consumerPid, providerPid, CONSUMER, STARTUP_REQUESTED.code(), consumer.getId()));
        store.save(transferProcess(providerPid, consumerPid, PROVIDER, SUSPENDING.code(), "provider-context"));

        providerProcessors.processSuspending(store.findById(providerPid)).join();

        // the consumer answers that it isn't ready yet: the provider keeps on suspending instead of terminating
        assertThat(store.findById(providerPid).stateAsString()).describedAs("provider error: %s", store.findById(providerPid).getErrorDetail()).isEqualTo(SUSPENDING.name());
        assertThat(store.findById(consumerPid).stateAsString()).isEqualTo(STARTUP_REQUESTED.name());

        // then the consumer finishes its start-up
        transitionStarted(consumerPid);

        // and the provider's next attempt is accepted
        providerProcessors.processSuspending(store.findById(providerPid)).join();

        assertThat(store.findById(providerPid).stateAsString())
                .describedAs("provider error: %s", store.findById(providerPid).getErrorDetail())
                .isEqualTo(SUSPENDED.name());
        assertThat(store.findById(consumerPid).stateAsString()).isEqualTo(SUSPENDING_REQUESTED.name());
    }

    private void transitionStarted(String transferProcessId) {
        var transferProcess = store.findById(transferProcessId);
        transferProcess.transitionStarted();
        store.save(transferProcess);
    }

    /**
     * Delivers the provider's messages to the consumer's protocol service, and maps its failures like
     * {@code DspHttpRemoteMessageDispatcherImpl} maps the HTTP status codes the counter-party answers with.
     */
    private CompletableFuture<StatusResult<Object>> deliver(ProtocolRemoteMessage message) {
        ServiceResult<TransferProcess> result;
        if (message instanceof TransferCompletionMessage completion) {
            if (firstCompletion == null) {
                firstCompletion = completion;
            }
            result = consumerService.notifyCompleted(consumer, completion, token());
        } else if (message instanceof TransferStartMessage start) {
            result = consumerService.notifyStarted(consumer, start, token());
        } else if (message instanceof TransferSuspensionMessage suspension) {
            result = consumerService.notifySuspended(consumer, suspension, token());
        } else {
            throw new IllegalArgumentException("unexpected message " + message.getClass().getSimpleName());
        }
        if (result.succeeded()) {
            return completedFuture(StatusResult.success(null));
        }
        var clientError = switch (result.reason()) {
            case BAD_REQUEST, UNAUTHORIZED, NOT_FOUND, CONFLICT -> true;
            default -> false;
        };
        return completedFuture(StatusResult.failure(clientError ? FATAL_ERROR : ERROR_RETRY,
                "Counter Party responded with %s: %s".formatted(result.reason(), result.getFailureDetail())));
    }

    private TransferProcess transferProcess(String id, String correlationId, TransferProcess.Type type, int state, String participantContextId) {
        return TransferProcess.Builder.newInstance()
                .id(id)
                .correlationId(correlationId)
                .type(type)
                .state(state)
                .contractId(AGREEMENT_ID)
                .assetId("assetId")
                .counterPartyAddress("http://counter-party")
                .protocol("protocol")
                .participantContextId(participantContextId)
                .stateTimestamp(Instant.now().toEpochMilli())
                .build();
    }

    private static TokenRepresentation token() {
        return TokenRepresentation.Builder.newInstance().token("token").build();
    }
}
