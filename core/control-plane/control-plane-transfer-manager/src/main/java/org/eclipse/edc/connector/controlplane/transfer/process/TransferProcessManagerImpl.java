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
import org.eclipse.edc.connector.controlplane.transfer.provision.DeprovisionResponsesHandler;
import org.eclipse.edc.connector.controlplane.transfer.provision.ProvisionResponsesHandler;
import org.eclipse.edc.connector.controlplane.transfer.provision.ResponsesHandler;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.provision.ResourceManifestGenerator;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING_REQUESTED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_SECRET;
import static org.eclipse.edc.statemachine.retry.processor.Process.future;
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
    private ResourceManifestGenerator manifestGenerator;
    private ProvisionManager provisionManager;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private DataFlowManager dataFlowManager;
    private Vault vault;
    private TransferProcessObservable observable;
    private DataAddressResolver addressResolver;
    private PolicyArchive policyArchive;
    private DataspaceProfileContextRegistry dataspaceProfileContextRegistry;
    private ProvisionResponsesHandler provisionResponsesHandler;
    private DeprovisionResponsesHandler deprovisionResponsesHandler;
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

        observable.invokeForEach(l -> l.preCreated(process));
        update(process);
        observable.invokeForEach(l -> l.initiated(process));

        return StatusResult.success(process);
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processTransfersInState(INITIAL, this::processInitial))
                .processor(processTransfersInState(PROVISIONING, this::processProvisioning))
                .processor(processTransfersInState(PROVISIONED, this::processProvisioned))
                .processor(processConsumerTransfersInState(REQUESTING, this::processRequesting))
                .processor(processProviderTransfersInState(STARTING, this::processStarting))
                .processor(processTransfersInState(SUSPENDING, this::processSuspending))
                .processor(processTransfersInState(SUSPENDING_REQUESTED, this::processSuspending))
                .processor(processProviderTransfersInState(RESUMING, this::processProviderResuming))
                .processor(processConsumerTransfersInState(RESUMING, this::processConsumerResuming))
                .processor(processTransfersInState(COMPLETING, this::processCompleting))
                .processor(processTransfersInState(COMPLETING_REQUESTED, this::processCompleting))
                .processor(processTransfersInState(TERMINATING, this::processTerminating))
                .processor(processTransfersInState(TERMINATING_REQUESTED, this::processTerminating))
                .processor(processTransfersInState(DEPROVISIONING, this::processDeprovisioning));
    }

    /**
     * Process INITIAL transfer<p> set it to PROVISIONING
     *
     * @param process the INITIAL transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processInitial(TransferProcess process) {
        var contractId = process.getContractId();
        var policy = policyArchive.findPolicyForContract(contractId);

        if (policy == null) {
            transitionToTerminated(process, "Policy not found for contract: " + contractId);
            return true;
        }

        if (process.getType() == CONSUMER) {
            var manifestResult = manifestGenerator.generateConsumerResourceManifest(process, policy);
            if (manifestResult.failed()) {
                transitionToTerminated(process, format("Resource manifest for process %s cannot be modified to fulfil policy. %s", process.getId(), manifestResult.getFailureMessages()));
                return true;
            }
            var manifest = manifestResult.getContent();

            if (manifest.empty()) {
                var provisioning = dataFlowManager.prepare(process, policy);
                if (provisioning.succeeded()) {
                    var response = provisioning.getContent();
                    if (response.isProvisioning()) {
                        process.setDataPlaneId(response.getDataPlaneId());
                        process.transitionProvisioningRequested();
                    } else {
                        process.setDataPlaneId(null);
                        process.transitionRequesting();
                    }

                    update(process);
                    return true;
                }
            } else {
                monitor.warning("control-plane provisioning has been deprecated, please convert your provision extensions to the data-plane model and deploy them there.");
            }

            process.transitionProvisioning(manifest);

        } else {
            var assetId = process.getAssetId();
            var dataAddress = addressResolver.resolveForAsset(assetId);
            if (dataAddress == null) {
                transitionToTerminating(process, "Asset not found: " + assetId);
                return true;
            }
            // default the content address to the asset address; this may be overridden during provisioning
            process.setContentDataAddress(dataAddress);

            var manifest = manifestGenerator.generateProviderResourceManifest(process, dataAddress, policy);
            if (!manifest.empty()) {
                monitor.warning("control-plane provisioning has been deprecated, please convert your provision extensions to the data-plane model and deploy them there.");
            }
            process.transitionProvisioning(manifest);
        }

        update(process);
        return true;
    }

    /**
     * Process PROVISIONING transfer<p> Launch provision process. On completion, set to PROVISIONED if succeeded, TERMINATED
     * otherwise
     * <p>
     * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a
     * provider, provisioning is initiated when a request is received and map involve preprocessing data or other
     * operations.
     *
     * @param process the PROVISIONING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processProvisioning(TransferProcess process) {
        var policy = policyArchive.findPolicyForContract(process.getContractId());

        var resources = process.getResourcesToProvision();

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(future("Provisioning", (p, c) -> provisionManager.provision(resources, policy)))
                .onSuccess((t, responses) -> handleResult(t, responses, provisionResponsesHandler))
                .onFailure((t, throwable) -> transitionToProvisioning(t))
                .onFinalFailure((t, throwable) -> {
                    if (t.getType() == PROVIDER) {
                        transitionToTerminating(t, format("Error during provisioning: %s", throwable.getMessage()));
                    } else {
                        transitionToTerminated(t, format("Error during provisioning: %s", throwable.getMessage()));
                    }
                })
                .execute();
    }

    /**
     * Process PROVISIONED transfer<p> If CONSUMER, set it to REQUESTING, if PROVIDER set to STARTING
     *
     * @param process the PROVISIONED transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processProvisioned(TransferProcess process) {
        if (CONSUMER == process.getType()) {
            transitionToRequesting(process);
        } else {
            transitionToStarting(process);
        }
        return true;
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
     * Process STARTING transfer<p> If PROVIDER, starts data transfer and send message to consumer, should never be CONSUMER
     *
     * @param process the STARTING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processStarting(TransferProcess process) {
        return startTransferFlow(process, this::transitionToStarting);
    }

    /**
     * Process RESUMING transfer for PROVIDER.
     *
     * @param process the RESUMING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processProviderResuming(TransferProcess process) {
        return startTransferFlow(process, this::transitionToResuming);
    }

    private boolean startTransferFlow(TransferProcess process, Consumer<TransferProcess> onFailure) {
        var policy = policyArchive.findPolicyForContract(process.getContractId());

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(result("Start DataFlow", (t, c) -> dataFlowManager.start(process, policy)))
                .doProcess(futureResult("Dispatch TransferRequestMessage to: " + process.getCounterPartyAddress(), (t, dataFlowResponse) -> {
                    if (dataFlowResponse.isProvisioning()) {
                        return completedFuture(StatusResult.success(dataFlowResponse));
                    }
                    var messageBuilder = TransferStartMessage.Builder.newInstance().dataAddress(dataFlowResponse.getDataAddress());
                    return dispatch(messageBuilder, t, Object.class)
                            .thenApply(result -> result.map(i -> dataFlowResponse));
                }))
                .onSuccess((t, dataFlowResponse) -> {
                    t.setDataPlaneId(dataFlowResponse.getDataPlaneId());
                    if (dataFlowResponse.isProvisioning()) {
                        process.transitionStartupRequested();
                        update(t);

                    } else {
                        process.transitionStarted();
                        observable.invokeForEach(l -> l.preStarted(t));
                        update(t);
                        observable.invokeForEach(l -> l.started(t, TransferProcessStartedData.Builder.newInstance().build()));
                    }

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
                                var result = dataFlowManager.terminate(t);
                                return completedFuture(result.mapEmpty());
                            } else {
                                return dispatch(builder, t, Object.class);
                            }
                        })
                )
                .onSuccess((t, c) -> {
                    transitionToCompleted(t);
                    if (t.getType() == PROVIDER) {
                        transitionToDeprovisioning(t);
                    }
                })
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
                .doProcess(result("Suspend DataFlow", (t, c) -> suspendDataFlow(process)))
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
                .doProcess(result("Terminate DataFlow", (p, i) -> dataFlowManager.terminate(process)))
                .doProcess(futureResult("Dispatch TransferTerminationMessage", (t, n) -> {
                    if (t.terminationWasRequestedByCounterParty()) {
                        return completedFuture(StatusResult.success(null));
                    } else {
                        return dispatch(TransferTerminationMessage.Builder.newInstance().reason(t.getErrorDetail()), t, Object.class);
                    }
                }))
                .onSuccess((t, c) -> {
                    transitionToTerminated(t);
                    if (t.getType() == PROVIDER) {
                        transitionToDeprovisioning(t);
                    }
                })
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

    /**
     * Process DEPROVISIONING transfer<p> Launch deprovision process. On completion, set to DEPROVISIONED if succeeded,
     * DEPROVISIONED otherwise
     *
     * @param process the DEPROVISIONING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processDeprovisioning(TransferProcess process) {
        observable.invokeForEach(l -> l.preDeprovisioning(process)); // TODO: this is called here since it's not callable from the command handler

        var policy = policyArchive.findPolicyForContract(process.getContractId());

        var resourcesToDeprovision = process.getResourcesToDeprovision();

        return entityRetryProcessFactory.retryProcessor(process)
                .doProcess(future("Deprovisioning", (p, c) -> provisionManager.deprovision(resourcesToDeprovision, policy)))
                .onSuccess((t, responses) -> handleResult(t, responses, deprovisionResponsesHandler))
                .onFailure((t, throwable) -> transitionToDeprovisioning(t))
                .onFinalFailure((t, throwable) -> transitionToDeprovisioningError(t, throwable.getMessage()))
                .execute();
    }

    @NotNull
    private StatusResult<Void> suspendDataFlow(TransferProcess process) {
        if (process.getType() == PROVIDER) {
            return dataFlowManager.suspend(process);
        } else {
            return StatusResult.success();
        }
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

    private <T> void handleResult(TransferProcess transferProcess, List<StatusResult<T>> responses, ResponsesHandler<StatusResult<T>> handler) {
        if (handler.handle(transferProcess, responses)) {
            update(transferProcess);
            handler.postActions(transferProcess);
        } else {
            breakLease(transferProcess);
        }
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

    private void transitionToProvisioning(TransferProcess process) {
        process.transitionProvisioning(process.getResourceManifest());
        observable.invokeForEach(l -> l.preProvisioning(process));
        update(process);
    }

    private void transitionToRequesting(TransferProcess process) {
        process.transitionRequesting();
        observable.invokeForEach(l -> l.preRequesting(process));
        update(process);
    }

    private void transitionToRequested(TransferProcess transferProcess, TransferProcessAck ack) {
        transferProcess.transitionRequested();
        transferProcess.setCorrelationId(ack.getProviderPid());
        observable.invokeForEach(l -> l.preRequested(transferProcess));
        update(transferProcess);
        observable.invokeForEach(l -> l.requested(transferProcess));
    }

    private void transitionToStarting(TransferProcess transferProcess) {
        transferProcess.transitionStarting();
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
        observable.invokeForEach(l -> l.preCompleted(transferProcess));
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
        observable.invokeForEach(l -> l.preTerminated(process));
        update(process);
        observable.invokeForEach(l -> l.terminated(process));
    }

    private void transitionToDeprovisioning(TransferProcess process) {
        process.transitionDeprovisioning();
        update(process);
    }

    private void transitionToDeprovisioningError(TransferProcess transferProcess, String message) {
        monitor.severe(message);
        transferProcess.transitionDeprovisioned(message);
        observable.invokeForEach(l -> l.preDeprovisioned(transferProcess));
        update(transferProcess);
        observable.invokeForEach(l -> l.deprovisioned(transferProcess));
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
            Objects.requireNonNull(manager.manifestGenerator, "manifestGenerator cannot be null");
            Objects.requireNonNull(manager.provisionManager, "provisionManager cannot be null");
            Objects.requireNonNull(manager.dataFlowManager, "dataFlowManager cannot be null");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry cannot be null");
            Objects.requireNonNull(manager.observable, "observable cannot be null");
            Objects.requireNonNull(manager.policyArchive, "policyArchive cannot be null");
            Objects.requireNonNull(manager.addressResolver, "addressResolver cannot be null");
            Objects.requireNonNull(manager.provisionResponsesHandler, "provisionResultHandler cannot be null");
            Objects.requireNonNull(manager.deprovisionResponsesHandler, "deprovisionResponsesHandler cannot be null");

            return manager;
        }

        public Builder manifestGenerator(ResourceManifestGenerator manifestGenerator) {
            manager.manifestGenerator = manifestGenerator;
            return this;
        }

        public Builder provisionManager(ProvisionManager provisionManager) {
            manager.provisionManager = provisionManager;
            return this;
        }

        public Builder dataFlowManager(DataFlowManager dataFlowManager) {
            manager.dataFlowManager = dataFlowManager;
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

        public Builder provisionResponsesHandler(ProvisionResponsesHandler provisionResponsesHandler) {
            manager.provisionResponsesHandler = provisionResponsesHandler;
            return this;
        }

        public Builder deprovisionResponsesHandler(DeprovisionResponsesHandler deprovisionResponsesHandler) {
            manager.deprovisionResponsesHandler = deprovisionResponsesHandler;
            return this;
        }

        public Builder pendingGuard(TransferProcessPendingGuard pendingGuard) {
            manager.pendingGuard = pendingGuard;
            return this;
        }
    }

}
