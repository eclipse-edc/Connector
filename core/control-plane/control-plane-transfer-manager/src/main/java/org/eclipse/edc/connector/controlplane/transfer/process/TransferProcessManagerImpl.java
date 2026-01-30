/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *       SAP SE - refactoring
 *       Mercedes-Benz Tech Innovation GmbH - connector id removal
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.process;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.eclipse.edc.connector.controlplane.asset.spi.index.DataAddressResolver;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferProcessAck;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.retry.processor.RetryProcessor;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTUP_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING_REQUESTED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_SECRET;
import static org.eclipse.edc.statemachine.retry.processor.Process.futureResult;
import static org.eclipse.edc.statemachine.retry.processor.Process.result;

/**
 * This transfer process manager receives a {@link TransferProcess} and transitions it through its internal state
 * machine (cf {@link TransferProcessStates}. When submitting a new {@link TransferProcess} it gets created and inserted
 * into the {@link TransferProcessStore}, then returns to the caller.
 * <p>
 * All subsequent state transitions happen asynchronously, the {@code AsyncTransferProcessManager#initiate*Request()}
 * will return immediately.
 * <p>
 * A data transfer processes transitions through a series of states, which allows the system to model both terminating
 * and non-terminating (e.g. streaming) transfers. Transitions occur asynchronously, since long-running processes such
 * as resource provisioning may need to be completed before transitioning to a subsequent state. The permissible state
 * transitions are defined by {@link TransferProcessStates}.
 * <p>
 * The transfer manager performs continual iterations, which seek to advance the state of transfer processes, including
 * recovery, in a FIFO state-based ordering. Each iteration will seek to transition a set number of processes for each
 * state to avoid situations where an excessive number of processes in one state block progress of processes in other
 * states.
 * <p>
 * If no processes need to be transitioned, the transfer manager will wait according to the defined {@link WaitStrategy}
 * before conducting the next iteration. A wait strategy may implement a backoff scheme.
 */
public class TransferProcessManagerImpl extends AbstractStateEntityManager<TransferProcess, TransferProcessStore>
        implements TransferProcessManager {
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private DataFlowController dataFlowController;
    private Vault vault;
    private TransferProcessObservable observable;
    private DataAddressResolver addressResolver;
    private PolicyArchive policyArchive;
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;
    private TransferProcessPendingGuard pendingGuard = tp -> false;

    private TransferProcessManagerImpl() {
    }

    /**
     * Initiate a consumer request TransferProcess.
     */
    @WithSpan
    @Override
    public StatusResult<TransferProcess> initiateConsumerRequest(ParticipantContext participantContext, TransferRequest transferRequest) {
        var id = Optional.ofNullable(transferRequest.getId()).orElseGet(() -> UUID.randomUUID().toString());
        var existingTransferProcess = store.findForCorrelationId(id);
        if (existingTransferProcess != null) {
            return StatusResult.success(existingTransferProcess);
        }

        var policy = policyArchive.findPolicyForContract(transferRequest.getContractId());
        if (policy == null) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "No policy found for contract " + transferRequest.getContractId());
        }

        var process = TransferProcess.Builder.newInstance()
                .id(id)
                .assetId(policy.getTarget())
                .dataDestination(transferRequest.getDataDestination())
                .counterPartyAddress(transferRequest.getCounterPartyAddress())
                .contractId(transferRequest.getContractId())
                .protocol(transferRequest.getProtocol())
                .type(CONSUMER)
                .clock(clock)
                .transferType(transferRequest.getTransferType())
                .privateProperties(transferRequest.getPrivateProperties())
                .callbackAddresses(transferRequest.getCallbackAddresses())
                .traceContext(telemetry.getCurrentTraceContext())
                .participantContextId(participantContext.getParticipantContextId())
                .dataplaneMetadata(transferRequest.getDataplaneMetadata())
                .build();

        update(process);
        observable.invokeForEach(l -> l.initiated(process));

        return StatusResult.success(process);
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processConsumerTransfersInState(INITIAL, this::processConsumerInitial))
                .processor(processProviderTransfersInState(INITIAL, this::processProviderInitial))
                .processor(processConsumerTransfersInState(REQUESTING, this::processRequesting))
                .processor(processProviderTransfersInState(STARTING, this::processStarting))
                .processor(processConsumerTransfersInState(STARTUP_REQUESTED, this::processStartupRequested))
                .processor(processTransfersInState(SUSPENDING, this::processSuspending))
                .processor(processTransfersInState(SUSPENDING_REQUESTED, this::processSuspending))
                .processor(processProviderTransfersInState(RESUMING, this::processProviderResuming))
                .processor(processConsumerTransfersInState(RESUMING, this::processConsumerResuming))
                .processor(processTransfersInState(COMPLETING, this::processCompleting))
                .processor(processTransfersInState(COMPLETING_REQUESTED, this::processCompleting))
                .processor(processTransfersInState(TERMINATING, this::processTerminating))
                .processor(processTransfersInState(TERMINATING_REQUESTED, this::processTerminating));
    }

    @WithSpan
    private boolean processConsumerInitial(TransferProcess process) {
        var contractId = process.getContractId();
        var policy = policyArchive.findPolicyForContract(contractId);

        if (policy == null) {
            transitionToTerminated(process, "Policy not found for contract: " + contractId);
            return true;
        }

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("prepare data flow", (t, ignored) -> dataFlowController.prepare(process, policy)))
                .onSuccess((t, response) -> {
                    process.setDataPlaneId(response.getDataPlaneId());
                    if (response.isAsync()) {
                        process.transitionPreparationRequested();
                        observable.invokeForEach(l -> l.preparationRequested(process));
                    } else {
                        process.updateDestination(response.getDataAddress());
                        process.transitionRequesting();
                    }
                    update(process);
                })
                .onFailure((t, throwable) -> transitionToInitial(t))
                .onFinalFailure((t, throwable) -> {
                    // with the upcoming data-plane signaling, the data-plane will be mandatory also on consumer side
                    // so in this case the transfer will retry
                    monitor.warning("Data Flow preparation failed, please note that this phase will become mandatory in " +
                            "the upcoming versions so please ensure that there's a data-plane able to manage the transfer-type " +
                            "%s. Error: %s".formatted(t.getTransferType(), throwable.getMessage()));
                    transitionToRequesting(t);
                })
                .execute();
    }

    @WithSpan
    private boolean processProviderInitial(TransferProcess process) {
        var contractId = process.getContractId();
        var policy = policyArchive.findPolicyForContract(contractId);

        if (policy == null) {
            transitionToTerminated(process, "Policy not found for contract: " + contractId);
            return true;
        }

        eventuallySetContentDataAddress(process);

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("start data flow", (t, ignored) -> dataFlowController.start(process, policy)))
                .onSuccess((t, response) -> {
                    process.setDataPlaneId(response.getDataPlaneId());
                    if (response.isAsync()) {
                        process.transitionStartupRequested();
                    } else {
                        // for the time being put the EDR in the destination field to being able to support both DPS
                        // and legacy protocol will eventually go away
                        var dataPlaneDataAddress = response.getDataAddress();
                        if (dataPlaneDataAddress != null) {
                            process.updateDestination(dataPlaneDataAddress);
                        }
                        process.transitionStarting();
                    }

                    update(process);
                })
                .onFailure((t, throwable) -> transitionToInitial(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();

    }

    /**
     * this is to support the legacy data plane signaling, it will be deleted when the legacy protocol will be dismissed
     *
     * @deprecated can be deleted as soon as the legacy data plane signaling protocol is dismissed.
     */
    @Deprecated(since = "0.16.0")
    private void eventuallySetContentDataAddress(TransferProcess process) {
        var assetId = process.getAssetId();
        var dataAddress = addressResolver.resolveForAsset(assetId);
        if (dataAddress != null) {
            process.setContentDataAddress(dataAddress);
        }
    }

    /**
     * Process REQUESTING transfer<p> If CONSUMER, send request to the provider, should never be PROVIDER
     *
     * @param process the REQUESTING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processRequesting(TransferProcess process) {
        var originalDestination = process.getDataDestination();
        var callbackAddress = dataspaceProfileContextRegistry.getWebhook(process.getProtocol());

        if (callbackAddress != null) {
            var agreementId = policyArchive.getAgreementIdForContract(process.getContractId());

            if (agreementId == null) {
                transitionToTerminated(process, "No agreement found for contract: " + process.getContractId());
                return true;
            }

            var dataDestination = Optional.ofNullable(originalDestination)
                    .map(DataAddress::getKeyName)
                    .map(key -> vault.resolveSecret(process.getParticipantContextId(), key))
                    .map(secret -> originalDestination.toBuilder().property(EDC_DATA_ADDRESS_SECRET, secret).build())
                    .orElse(originalDestination);

            var messageBuilder = TransferRequestMessage.Builder.newInstance()
                    .callbackAddress(callbackAddress.url())
                    .dataDestination(dataDestination)
                    .transferType(process.getTransferType())
                    .contractId(agreementId);

            return entityRetryProcessFactory.retryProcessor(process)
                    .doProcess(futureResult("Dispatch TransferRequestMessage to " + process.getCounterPartyAddress(),
                            (t, c) -> dispatch(messageBuilder, t, TransferProcessAck.class))
                    )
                    .onSuccess(this::transitionToRequested)
                    .onFailure((t, throwable) -> transitionToRequesting(t))
                    .onFinalFailure(this::transitionToTerminated)
                    .execute();

        } else {
            transitionToTerminated(process, "No callback address found for protocol: " + process.getProtocol());
            return true;
        }
    }

    /**
     * Process STARTUP_REQUESTED transfer for consumer<p> Notify data-plane that data flow has been started
     *
     * @param process the STARTUP_REQUESTED transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processStartupRequested(TransferProcess process) {
        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Notify started to data plane " + process.getCounterPartyAddress(), (t, r) ->
                        dataFlowController.started(process))
                )
                .onSuccess((t, c) -> transitionToStarted(t))
                .onFailure((t, throwable) -> transitionToStartupRequested(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    /**
     * Process STARTING transfer<p> If PROVIDER, starts data transfer and send message to consumer, should never be CONSUMER
     *
     * @param process the STARTING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processStarting(TransferProcess process) {
        return startFlow(process, this::transitionToStarting, Function.identity());
    }

    /**
     * Process RESUMING transfer for PROVIDER.
     *
     * @param process the RESUMING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processProviderResuming(TransferProcess process) {
        var policy = policyArchive.findPolicyForContract(process.getContractId());

        Function<RetryProcessor<TransferProcess, ?>, RetryProcessor<TransferProcess, ?>> preProcess = r -> r
                .doProcess(result("Data Plane resume", (t, ignored) -> dataFlowController.start(process, policy)))
                .doProcess(result("Set new data destination", (t, response) -> {
                    t.updateDestination(response.getDataAddress());
                    return StatusResult.success();
                }));
        return startFlow(process, this::transitionToResuming, preProcess);
    }

    private boolean startFlow(TransferProcess process, Consumer<TransferProcess> onFailure, Function<RetryProcessor<TransferProcess, ?>, RetryProcessor<TransferProcess, ?>> preProcessing) {
        return preProcessing.apply(entityRetryProcessFactory.retryProcessor(process))
                .doProcess(futureResult("Dispatch TransferRequestMessage to: " + process.getCounterPartyAddress(), (t, dataFlowResponse) -> {
                    var messageBuilder = TransferStartMessage.Builder.newInstance().dataAddress(t.getDataDestination());
                    return dispatch(messageBuilder, t, Object.class);
                }))
                .onSuccess((t, o) -> {
                    t.transitionStarted();
                    update(t);
                    observable.invokeForEach(l -> l.started(t, TransferProcessStartedData.Builder.newInstance().build()));
                })
                .onFailure((t, throwable) -> onFailure.accept(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    /**
     * Process STARTING transfer that was SUSPENDED
     *
     * @param process the STARTING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processConsumerResuming(TransferProcess process) {
        var messageBuilder = TransferStartMessage.Builder.newInstance();

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(futureResult("Dispatch TransferStartMessage for transfer resume to " + process.getCounterPartyAddress(),
                        (t, dataFlowResponse) -> dispatch(messageBuilder, t, Object.class))
                )
                .onSuccess((t, c) -> transitionToResumed(t))
                .onFailure((t, throwable) -> transitionToResuming(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    /**
     * Process COMPLETING transfer<p> Send COMPLETED message to counter-part
     *
     * @param process the COMPLETING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processCompleting(TransferProcess process) {
        var builder = TransferCompletionMessage.Builder.newInstance();

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(futureResult("Dispatch TransferCompletionMessage to " + process.getCounterPartyAddress(),
                        (t, dataFlowResponse) -> {
                            if (t.completionWasRequestedByCounterParty()) {
                                var result = dataFlowController.completed(t);
                                return completedFuture(result.mapEmpty());
                            } else {
                                return dispatch(builder, t, Object.class);
                            }
                        })
                )
                .onSuccess((t, c) -> transitionToCompleted(t))
                .onFailure((t, throwable) -> transitionToCompleting(t))
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    /**
     * Process SUSPENDING transfer<p>
     * Suspend data flow unless it's CONSUMER and send SUSPENDED message to counter-part.
     *
     * @param process the SUSPENDING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processSuspending(TransferProcess process) {
        var builder = TransferSuspensionMessage.Builder.newInstance()
                .reason(process.getErrorDetail());

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Suspend DataFlow", (t, c) -> dataFlowController.suspend(process)))
                .doProcess(futureResult("Dispatch TransferSuspensionMessage to " + process.getCounterPartyAddress(),
                        (t, dataFlowResponse) -> {
                            if (t.suspensionWasRequestedByCounterParty()) {
                                return completedFuture(StatusResult.success(null));
                            } else {
                                return dispatch(builder, t, Object.class);
                            }
                        })
                )
                .onSuccess((t, content) -> transitionToSuspended(t))
                .onFailure((t, throwable) -> {
                    if (t.suspensionWasRequestedByCounterParty()) {
                        transitionToSuspendingRequested(t, throwable.getMessage());
                    } else {
                        transitionToSuspending(t, throwable.getMessage());
                    }
                })
                .onFinalFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .execute();
    }

    /**
     * Process TERMINATING transfer<p>
     * Stop data flow unless it's CONSUMER and send TERMINATED message to counter-part.
     *
     * @param process the TERMINATING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processTerminating(TransferProcess process) {
        if (process.getType() == CONSUMER && process.getState() < REQUESTED.code()) {
            transitionToTerminated(process);
            return true;
        }

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Terminate DataFlow", (p, i) -> dataFlowController.terminate(process)))
                .doProcess(futureResult("Dispatch TransferTerminationMessage", (t, n) -> {
                    if (t.terminationWasRequestedByCounterParty()) {
                        return completedFuture(StatusResult.success(null));
                    } else {
                        return dispatch(TransferTerminationMessage.Builder.newInstance().reason(t.getErrorDetail()), t, Object.class);
                    }
                }))
                .onSuccess((t, c) -> transitionToTerminated(t))
                .onFailure((t, throwable) -> {
                    if (t.terminationWasRequestedByCounterParty()) {
                        transitionToTerminatingRequested(t, throwable.getMessage());
                    } else {
                        transitionToTerminating(t, throwable.getMessage());
                    }
                })
                .onFinalFailure(this::transitionToTerminated)
                .execute();
    }

    private <T, M extends TransferRemoteMessage, B extends TransferRemoteMessage.Builder<M, B>> CompletableFuture<StatusResult<T>> dispatch(B messageBuilder, TransferProcess process, Class<T> responseType) {

        var contractPolicy = policyArchive.findPolicyForContract(process.getContractId());

        messageBuilder.protocol(process.getProtocol())
                .counterPartyAddress(process.getCounterPartyAddress())
                .processId(Optional.ofNullable(process.getCorrelationId()).orElse(process.getId()))
                .policy(contractPolicy);

        if (process.lastSentProtocolMessage() != null) {
            messageBuilder.id(process.lastSentProtocolMessage());
        }

        if (process.getType() == PROVIDER) {
            messageBuilder.consumerPid(process.getCorrelationId())
                    .providerPid(process.getId())
                    .counterPartyId(contractPolicy.getAssignee());
        } else {
            messageBuilder.consumerPid(process.getId())
                    .providerPid(process.getCorrelationId())
                    .counterPartyId(contractPolicy.getAssigner());
        }

        var message = messageBuilder.build();

        process.lastSentProtocolMessage(message.getId());

        return dispatcherRegistry.dispatch(process.getParticipantContextId(), responseType, message);
    }

    private Processor processConsumerTransfersInState(TransferProcessStates state, Function<TransferProcess, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending(), Criterion.criterion("type", "=", CONSUMER.name()) };
        return createProcessor(function, filter);
    }

    private Processor processProviderTransfersInState(TransferProcessStates state, Function<TransferProcess, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending(), Criterion.criterion("type", "=", PROVIDER.name()) };
        return createProcessor(function, filter);
    }

    private Processor processTransfersInState(TransferProcessStates state, Function<TransferProcess, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()), isNotPending() };
        return createProcessor(function, filter);
    }

    private ProcessorImpl<TransferProcess> createProcessor(Function<TransferProcess, Boolean> function, Criterion[] filter) {
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter))
                .process(telemetry.contextPropagationMiddleware(function))
                .guard(pendingGuard, this::setPending)
                .onNotProcessed(this::breakLease)
                .build();
    }

    private boolean setPending(TransferProcess transferProcess) {
        transferProcess.setPending(true);
        update(transferProcess);
        return true;
    }

    private void transitionToInitial(TransferProcess process) {
        process.transitionInitial();
        update(process);
    }

    private void transitionToRequesting(TransferProcess process) {
        process.transitionRequesting();
        update(process);
    }

    private void transitionToRequested(TransferProcess transferProcess, TransferProcessAck ack) {
        transferProcess.transitionRequested();
        transferProcess.setCorrelationId(ack.getProviderPid());
        update(transferProcess);
        observable.invokeForEach(l -> l.requested(transferProcess));
    }

    private void transitionToStarting(TransferProcess transferProcess) {
        transferProcess.transitionStarting();
        update(transferProcess);
    }

    private void transitionToStarted(TransferProcess transferProcess) {
        transferProcess.transitionStarted();
        update(transferProcess);
        var transferStartedData = TransferProcessStartedData.Builder.newInstance()
                .dataAddress(transferProcess.getContentDataAddress())
                .build();
        observable.invokeForEach(l -> l.started(transferProcess, transferStartedData));
    }

    private void transitionToStartupRequested(TransferProcess transferProcess) {
        transferProcess.transitionStartupRequested();
        update(transferProcess);
    }

    private void transitionToResuming(TransferProcess process) {
        process.transitionResuming();
        update(process);
    }

    private void transitionToResumed(TransferProcess process) {
        process.transitionResumed();
        update(process);
    }

    private void transitionToCompleting(TransferProcess process) {
        process.transitionCompleting();
        update(process);
    }

    private void transitionToCompleted(TransferProcess transferProcess) {
        transferProcess.transitionCompleted();
        update(transferProcess);
        observable.invokeForEach(l -> l.completed(transferProcess));
    }

    private void transitionToSuspending(TransferProcess process, String message) {
        process.transitionSuspending(message);
        update(process);
    }

    private void transitionToSuspendingRequested(TransferProcess process, String message, Throwable... errors) {
        monitor.warning(message, errors);
        process.transitionSuspendingRequested(message);
        update(process);
    }

    private void transitionToSuspended(TransferProcess process) {
        process.transitionSuspended();
        update(process);
        observable.invokeForEach(l -> l.suspended(process));
    }

    private void transitionToTerminating(TransferProcess process, String message, Throwable... errors) {
        monitor.warning(message, errors);
        process.transitionTerminating(message);
        update(process);
    }

    private void transitionToTerminatingRequested(TransferProcess process, String message, Throwable... errors) {
        monitor.warning(message, errors);
        process.transitionTerminatingRequested(message);
        update(process);
    }

    private void transitionToTerminated(TransferProcess process, Throwable throwable) {
        transitionToTerminated(process, throwable.getMessage());
    }

    private void transitionToTerminated(TransferProcess process, String message) {
        process.setErrorDetail(message);
        monitor.warning(message);
        transitionToTerminated(process);
    }

    private void transitionToTerminated(TransferProcess process) {
        process.transitionTerminated();
        update(process);
        observable.invokeForEach(l -> l.terminated(process));
    }

    public static class Builder
            extends AbstractStateEntityManager.Builder<TransferProcess, TransferProcessStore, TransferProcessManagerImpl, Builder> {

        private Builder() {
            super(new TransferProcessManagerImpl());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public TransferProcessManagerImpl build() {
            super.build();
            Objects.requireNonNull(manager.dataFlowController, "dataFlowController cannot be null");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry cannot be null");
            Objects.requireNonNull(manager.observable, "observable cannot be null");
            Objects.requireNonNull(manager.policyArchive, "policyArchive cannot be null");
            Objects.requireNonNull(manager.addressResolver, "addressResolver cannot be null");

            return manager;
        }

        public Builder dataFlowController(DataFlowController dataFlowController) {
            manager.dataFlowController = dataFlowController;
            return this;
        }

        public Builder dispatcherRegistry(RemoteMessageDispatcherRegistry registry) {
            manager.dispatcherRegistry = registry;
            return this;
        }

        public Builder vault(Vault vault) {
            manager.vault = vault;
            return this;
        }

        public Builder observable(TransferProcessObservable observable) {
            manager.observable = observable;
            return this;
        }

        public Builder policyArchive(PolicyArchive policyArchive) {
            manager.policyArchive = policyArchive;
            return this;
        }

        public Builder addressResolver(DataAddressResolver addressResolver) {
            manager.addressResolver = addressResolver;
            return this;
        }

        public Builder dataspaceProfileContextRegistry(DataspaceProfileContextRegistry dataspaceProfileContextRegistry) {
            manager.dataspaceProfileContextRegistry = dataspaceProfileContextRegistry;
            return this;
        }

        public Builder pendingGuard(TransferProcessPendingGuard pendingGuard) {
            manager.pendingGuard = pendingGuard;
            return this;
        }
    }

}
