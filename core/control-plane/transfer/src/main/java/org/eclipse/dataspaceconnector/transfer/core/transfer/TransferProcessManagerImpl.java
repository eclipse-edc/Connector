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
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.transfer;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.dataspaceconnector.common.statemachine.StateMachineManager;
import org.eclipse.dataspaceconnector.common.statemachine.StateProcessorImpl;
import org.eclipse.dataspaceconnector.common.statemachine.retry.SendRetryManager;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.command.CommandProcessor;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.entity.StatefulEntity;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyArchive;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedContentResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataAddressResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommand;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ERROR;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTING;

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
 * transitions are defined by {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates}.
 * <p>
 * The transfer manager performs continual iterations, which seek to advance the state of transfer processes, including
 * recovery, in a FIFO state-based ordering. Each iteration will seek to transition a set number of processes for each
 * state to avoid situations where an excessive number of processes in one state block progress of processes in other
 * states.
 * <p>
 * If no processes need to be transitioned, the transfer manager will wait according to the defined {@link WaitStrategy}
 * before conducting the next iteration. A wait strategy may implement a backoff scheme.
 */
public class TransferProcessManagerImpl implements TransferProcessManager, ProvisionCallbackDelegate {
    private int batchSize = 5;
    private WaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds
    private ResourceManifestGenerator manifestGenerator;
    private ProvisionManager provisionManager;
    private TransferProcessStore transferProcessStore;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private DataFlowManager dataFlowManager;
    private StatusCheckerRegistry statusCheckerRegistry;
    private Vault vault;
    private TypeManager typeManager;
    private TransferProcessObservable observable;
    private CommandQueue<TransferProcessCommand> commandQueue;
    private CommandRunner<TransferProcessCommand> commandRunner;
    private CommandProcessor<TransferProcessCommand> commandProcessor;
    private Monitor monitor;
    private Telemetry telemetry;
    private ExecutorInstrumentation executorInstrumentation;
    private StateMachineManager stateMachineManager;
    private DataAddressResolver addressResolver;
    private PolicyArchive policyArchive;
    private SendRetryManager<StatefulEntity> sendRetryManager;
    private Clock clock;

    private TransferProcessManagerImpl() {
    }

    @NotNull
    private static Result<Void> toFatalError(StatusResult<?> result) {
        if (result.fatalError()) {
            return Result.failure(result.getFailureMessages());
        } else {
            return Result.success();
        }
    }

    public void start() {
        stateMachineManager = StateMachineManager.Builder.newInstance("transfer-process", monitor, executorInstrumentation, waitStrategy)
                .processor(processTransfersInState(INITIAL, this::processInitial))
                .processor(processTransfersInState(PROVISIONING, this::processProvisioning))
                .processor(processTransfersInState(PROVISIONED, this::processProvisioned))
                .processor(processTransfersInState(REQUESTING, this::processRequesting))
                .processor(processTransfersInState(REQUESTED, this::processRequested))
                .processor(processTransfersInState(IN_PROGRESS, this::processInProgress))
                .processor(processTransfersInState(DEPROVISIONING, this::processDeprovisioning))
                .processor(processTransfersInState(DEPROVISIONED, this::processDeprovisioned))
                .processor(onCommands(this::processCommand))
                .build();
        stateMachineManager.start();
    }

    public void stop() {
        if (stateMachineManager != null) {
            stateMachineManager.stop();
        }
    }

    /**
     * Initiate a consumer request TransferProcess.
     */
    @WithSpan
    @Override
    public StatusResult<String> initiateConsumerRequest(DataRequest dataRequest) {
        return initiateRequest(CONSUMER, dataRequest);

    }

    /**
     * Initiate a provider request TransferProcess.
     */
    @WithSpan
    @Override
    public StatusResult<String> initiateProviderRequest(DataRequest dataRequest) {
        return initiateRequest(PROVIDER, dataRequest);
    }

    @Override
    public void enqueueCommand(TransferProcessCommand command) {
        commandQueue.enqueue(command);
    }

    @Override
    public void handleProvisionResult(String processId, List<StatusResult<ProvisionResponse>> responses) {
        var transferProcess = transferProcessStore.find(processId);
        if (transferProcess == null) {
            monitor.severe("TransferProcessManager: no TransferProcess found for provisioned resources");
            return;
        }

        if (transferProcess.getState() == ERROR.code()) {
            monitor.severe(format("TransferProcessManager: transfer process %s is in ERROR state, so provisioning could not be completed", transferProcess.getId()));
            return;
        }

        var validationResult = responses.stream()
                .map(result -> result.succeeded()
                        ? storeProvisionedSecrets(transferProcess.getId(), result.getContent())
                        : toFatalError(result)
                )
                .filter(AbstractResult::failed)
                .reduce(Result::merge)
                .orElse(Result.success());

        if (validationResult.failed()) {
            var message = format("Transitioning transfer process %s to ERROR state due to fatal provisioning errors: \n%s", transferProcess.getId(), validationResult.getFailureDetail());
            transitionToError(transferProcess, message);
            return;
        }

        var provisionResponses = responses.stream()
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .collect(Collectors.toList());

        handleProvisionResponses(transferProcess, provisionResponses);
    }

    @Override
    public void handleDeprovisionResult(String processId, List<StatusResult<DeprovisionedResource>> responses) {
        var transferProcess = transferProcessStore.find(processId);
        if (transferProcess == null) {
            monitor.severe("TransferProcessManager: no TransferProcess found for deprovisioned resources");
            return;
        }

        if (transferProcess.getState() == ERROR.code()) {
            monitor.severe(format("TransferProcessManager: transfer process %s is in ERROR state, so deprovisioning could not be processed", transferProcess.getId()));
            return;
        }

        var validationResult = responses.stream()
                .filter(AbstractResult::failed)
                .map(TransferProcessManagerImpl::toFatalError)
                .filter(AbstractResult::failed)
                .reduce(Result::merge)
                .orElse(Result.success());

        if (validationResult.failed()) {
            var message = format("Transitioning transfer process %s to ERROR state due to fatal deprovisioning errors: \n%s", transferProcess.getId(), validationResult.getFailureDetail());
            transitionToError(transferProcess, message);
            return;
        }

        var deprovisionResponses = responses.stream()
                .filter(AbstractResult::succeeded)
                .map(AbstractResult::getContent)
                .collect(Collectors.toList());

        handleDeprovisionResponses(transferProcess, deprovisionResponses);
    }

    private StatusResult<String> initiateRequest(TransferProcess.Type type, DataRequest dataRequest) {
        // make the request idempotent: if the process exists, return
        var processId = transferProcessStore.processIdForDataRequestId(dataRequest.getId());
        if (processId != null) {
            return StatusResult.success(processId);
        }
        var id = randomUUID().toString();
        var process = TransferProcess.Builder.newInstance()
                .id(id)
                .dataRequest(dataRequest)
                .type(type)
                .createdAt(clock.millis())
                .traceContext(telemetry.getCurrentTraceContext())
                .build();
        if (process.getState() == TransferProcessStates.UNSAVED.code()) {
            process.transitionInitial();
        }
        observable.invokeForEach(l -> l.preCreated(process));
        transferProcessStore.create(process);
        observable.invokeForEach(l -> l.initiated(process));
        return StatusResult.success(process.getId());
    }

    /**
     * Process INITIAL transfer<p> set it to PROVISIONING
     *
     * @param process the INITIAL transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processInitial(TransferProcess process) {
        var dataRequest = process.getDataRequest();

        var policy = policyArchive.findPolicyForContract(dataRequest.getContractId());

        ResourceManifest manifest;
        if (process.getType() == CONSUMER) {
            var manifestResult = manifestGenerator.generateConsumerResourceManifest(dataRequest, policy);
            if (manifestResult.failed()) {
                monitor.severe(format("Transitioning transfer process %s to ERROR state. Resource manifest cannot be modified to fulfil policy: %s",
                        process.getId(), manifestResult.getFailureMessages()));
                process.transitionError(format("Resource manifest for process %s cannot be modified to fulfil policy.", process.getId()));
                updateTransferProcess(process, l -> l.preError(process));
                return true;
            }
            manifest = manifestResult.getContent();
        } else {
            var assetId = process.getDataRequest().getAssetId();
            var dataAddress = addressResolver.resolveForAsset(assetId);
            if (dataAddress == null) {
                transitionToError(process, "Asset not found: " + assetId);
            }
            // default the content address to the asset address; this may be overridden during provisioning
            process.setContentDataAddress(dataAddress);
            manifest = manifestGenerator.generateProviderResourceManifest(dataRequest, dataAddress, policy);
        }

        process.transitionProvisioning(manifest);
        updateTransferProcess(process, l -> l.preProvisioning(process));
        return true;
    }

    /**
     * Process PROVISIONING transfer<p> Launch provision process. On completion, set to PROVISIONED if succeeded, ERROR
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
        var dataRequest = process.getDataRequest();

        var policy = policyArchive.findPolicyForContract(dataRequest.getContractId());

        var resources = process.getResourcesToProvision();
        provisionManager.provision(resources, policy)
                .whenComplete((responses, throwable) -> {
                    if (throwable == null) {
                        handleProvisionResult(process.getId(), responses);
                    } else {
                        transitionToError(process.getId(), throwable, "Error during provisioning");
                    }
                });

        return true;
    }

    /**
     * Process PROVISIONED transfer<p> If CONSUMER, set it to REQUESTING, if PROVIDER initiate data transfer
     *
     * @param process the PROVISIONED transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processProvisioned(TransferProcess process) {
        if (CONSUMER == process.getType()) {
            process.transitionRequesting();
            transferProcessStore.update(process);
            observable.invokeForEach(l -> l.preRequesting(process));
        } else {
            return processProviderRequest(process);
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
        var dataRequest = process.getDataRequest();
        if (CONSUMER == process.getType()) {
            return processConsumerRequest(process, dataRequest);
        } else {
            // should never happen: a provider transfer cannot be REQUESTING
            return false;
        }
    }

    /**
     * Process REQUESTED transfer<p> If is managed or there are provisioned resources set IN_PROGRESS or STREAMING, do
     * nothing otherwise
     *
     * @param process the REQUESTED transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processRequested(TransferProcess process) {
        if (!process.getDataRequest().isManagedResources() || (process.getProvisionedResourceSet() != null && !process.getProvisionedResourceSet().empty())) {
            process.transitionInProgressOrStreaming();
            updateTransferProcess(process, l -> l.preInProgress(process));
            monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
            return true;
        } else {
            monitor.debug("Process " + process.getId() + " does not yet have provisioned resources, will stay in " + TransferProcessStates.REQUESTED);
            return false;
        }
    }

    /**
     * Process IN PROGRESS transfer<p> if is completed or there's no checker and it's not managed, set to COMPLETE,
     * nothing otherwise.
     *
     * @param process the IN PROGRESS transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processInProgress(TransferProcess process) {
        if (process.getType() != CONSUMER) {
            return false;
        }

        var checker = statusCheckerRegistry.resolve(process.getDataRequest().getDestinationType());
        if (checker == null) {
            if (process.getDataRequest().isManagedResources()) {
                monitor.warning(format("No checker found for process %s. The process will not advance to the COMPLETED state.", process.getId()));
                return false;
            } else {
                //no checker, transition the process to the COMPLETED state automatically
                transitionToCompleted(process);
            }
            return true;
        } else {
            List<ProvisionedResource> resources = process.getDataRequest().isManagedResources() ? process.getProvisionedResourceSet().getResources() : emptyList();
            if (checker.isComplete(process, resources)) {
                transitionToCompleted(process);
                return true;
            } else {
                // Process is not finished yet, so it stays in the IN_PROGRESS state
                monitor.debug(format("Transfer process %s not COMPLETED yet. The process will not advance to the COMPLETED state.", process.getId()));
                breakLease(process);
                return false;
            }
        }
    }

    /**
     * Process DEPROVISIONING transfer<p> Launch deprovision process. On completion, set to DEPROVISIONED if succeeded,
     * ERROR otherwise
     *
     * @param process the DEPROVISIONING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processDeprovisioning(TransferProcess process) {
        observable.invokeForEach(l -> l.preDeprovisioning(process)); // TODO: this is called here since it's not callable from the command handler

        var dataRequest = process.getDataRequest();

        var policy = policyArchive.findPolicyForContract(dataRequest.getContractId());

        var resourcesToDeprovision = process.getResourcesToDeprovision();

        provisionManager.deprovision(resourcesToDeprovision, policy)
                .whenComplete((responses, throwable) -> {
                    if (throwable == null) {
                        handleDeprovisionResult(process.getId(), responses);
                    } else {
                        transitionToError(process.getId(), throwable, "Error during deprovisioning");
                    }
                });

        return true;
    }

    /**
     * Process DEPROVISIONED transfer<p> Set it to ENDED.
     *
     * @param process the DEPROVISIONED transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processDeprovisioned(TransferProcess process) {
        process.transitionEnded();
        observable.invokeForEach(l -> l.preEnded(process));
        transferProcessStore.update(process);
        observable.invokeForEach(l -> l.ended(process));
        monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
        return true;
    }

    private void handleProvisionResponses(TransferProcess transferProcess, List<ProvisionResponse> responses) {
        responses.stream()
                .map(response -> {
                    var provisionedResource = response.getResource();

                    if (provisionedResource instanceof ProvisionedDataAddressResource) {
                        var dataAddressResource = (ProvisionedDataAddressResource) provisionedResource;
                        var dataAddress = dataAddressResource.getDataAddress();
                        var secretToken = response.getSecretToken();
                        if (secretToken != null) {
                            var keyName = dataAddressResource.getResourceName();
                            dataAddress.setKeyName(keyName);
                        }

                        if (dataAddressResource instanceof ProvisionedDataDestinationResource) {
                            // a data destination was provisioned by a consumer
                            transferProcess.getDataRequest().updateDestination(dataAddress);
                        } else if (dataAddressResource instanceof ProvisionedContentResource) {
                            // content for the data transfer was provisioned by the provider
                            transferProcess.setContentDataAddress(dataAddress);
                        }
                    }

                    return provisionedResource;
                })
                .filter(Objects::nonNull)
                .forEach(transferProcess::addProvisionedResource);

        if (transferProcess.provisioningComplete()) {
            transferProcess.transitionProvisioned();
            observable.invokeForEach(l -> l.preProvisioned(transferProcess));
            transferProcessStore.update(transferProcess);
            observable.invokeForEach(l -> l.provisioned(transferProcess));
        } else if (responses.stream().anyMatch(ProvisionResponse::isInProcess)) {
            transferProcess.transitionProvisioningRequested();
            transferProcessStore.update(transferProcess);
            observable.invokeForEach(l -> l.provisioningRequested(transferProcess));
        } else {
            transferProcessStore.update(transferProcess);
        }
    }

    private void handleDeprovisionResponses(TransferProcess transferProcess, List<DeprovisionedResource> results) {
        results.stream()
                .map(deprovisionedResource -> {
                    var provisionedResource = transferProcess.getProvisionedResource(deprovisionedResource.getProvisionedResourceId());
                    if (provisionedResource == null) {
                        monitor.severe("Received a deprovision result for a provisioned resource that was not found. Skipping.");
                        return null;
                    }

                    if (provisionedResource.hasToken() && provisionedResource instanceof ProvisionedDataAddressResource) {
                        removeDeprovisionedSecrets((ProvisionedDataAddressResource) provisionedResource, transferProcess.getId());
                    }
                    return deprovisionedResource;
                })
                .filter(Objects::nonNull)
                .forEach(transferProcess::addDeprovisionedResource);

        if (transferProcess.deprovisionComplete()) {
            transferProcess.transitionDeprovisioned();
            observable.invokeForEach(l -> l.preDeprovisioned(transferProcess));
            transferProcessStore.update(transferProcess);
            observable.invokeForEach(l -> l.deprovisioned(transferProcess));
        } else if (results.stream().anyMatch(DeprovisionedResource::isInProcess)) {
            transferProcess.transitionDeprovisioningRequested();
            transferProcessStore.update(transferProcess);
            observable.invokeForEach(l -> l.deprovisioningRequested(transferProcess));
        } else {
            transferProcessStore.update(transferProcess);
        }
    }

    @NotNull
    private Result<Void> storeProvisionedSecrets(String transferProcessId, ProvisionResponse response) {
        var resource = response.getResource();

        if (resource instanceof ProvisionedDataAddressResource) {
            var dataAddressResource = (ProvisionedDataAddressResource) resource;
            var secretToken = response.getSecretToken();
            if (secretToken != null) {
                var keyName = dataAddressResource.getResourceName();
                var secretResult = vault.storeSecret(keyName, typeManager.writeValueAsString(secretToken));
                if (secretResult.failed()) {
                    return Result.failure(format("Error storing secret in vault with key %s for transfer process %s: \n %s",
                            keyName, transferProcessId, join("\n", secretResult.getFailureMessages())));
                }
            }
        }

        return Result.success();
    }

    private void removeDeprovisionedSecrets(ProvisionedDataAddressResource provisionedResource, String transferProcessId) {
        var keyName = provisionedResource.getResourceName();
        var result = vault.deleteSecret(keyName);
        if (result.failed()) {
            monitor.severe(format("Error deleting secret from vault with key %s for transfer process %s: \n %s",
                    keyName, transferProcessId, join("\n", result.getFailureMessages())));
        }
    }

    private boolean processCommand(TransferProcessCommand command) {
        return commandProcessor.processCommandQueue(command);
    }

    private StateProcessorImpl<TransferProcess> processTransfersInState(TransferProcessStates state, Function<TransferProcess, Boolean> function) {
        var functionWithTraceContext = telemetry.contextPropagationMiddleware(function);
        return new StateProcessorImpl<>(() -> transferProcessStore.nextForState(state.code(), batchSize), functionWithTraceContext);
    }

    private StateProcessorImpl<TransferProcessCommand> onCommands(Function<TransferProcessCommand, Boolean> process) {
        return new StateProcessorImpl<>(() -> commandQueue.dequeue(5), process);
    }

    @WithSpan
    private void transitionToCompleted(TransferProcess process) {
        process.transitionCompleted();
        monitor.debug("Process " + process.getId() + " is now " + COMPLETED);
        updateTransferProcess(process, l -> l.preCompleted(process));
        observable.invokeForEach(l -> l.completed(process));
    }

    private void transitionToError(String id, Throwable throwable, String message) {
        var transferProcess = transferProcessStore.find(id);
        if (transferProcess == null) {
            monitor.severe(format("TransferProcessManager: no TransferProcess found with id %s", id));
            return;
        }

        transitionToError(transferProcess, message, throwable);
    }

    private void transitionToError(TransferProcess process, String message, Throwable... errors) {
        monitor.severe(message, errors);
        process.transitionError(message);
        updateTransferProcess(process, l -> l.preError(process));
        observable.invokeForEach(l -> l.failed(process));
    }

    private boolean processProviderRequest(TransferProcess process) {
        if (sendRetryManager.shouldDelay(process)) {
            breakLease(process);
            return false;
        }

        initiateDataTransfer(process);
        return true;
    }

    private boolean processConsumerRequest(TransferProcess process, DataRequest dataRequest) {

        if (sendRetryManager.shouldDelay(process)) {
            breakLease(process);
            return false;
        }

        sendConsumerRequest(process, dataRequest);
        return true;
    }

    private void initiateDataTransfer(TransferProcess process) {
        var dataRequest = process.getDataRequest();
        var contentAddress = process.getContentDataAddress();

        var policy = policyArchive.findPolicyForContract(dataRequest.getContractId());

        var response = dataFlowManager.initiate(dataRequest, contentAddress, policy);

        if (response.succeeded()) {
            process.transitionInProgressOrStreaming();
            updateTransferProcess(process, l -> l.preInProgress(process));
        } else {
            if (response.fatalError()) {
                var message = format("TransferProcessManager: Fatal error initiating data transfer: %s. Error details: %s", process.getId(), response.getFailureDetail());
                transitionToError(process, message);
            } else if (sendRetryManager.retriesExhausted(process)) {
                var message = format("TransferProcessManager: attempt #%d failed to initiate data transfer. Retry limit exceeded, TransferProcess %s moves to ERROR state. Cause: %s",
                        process.getStateCount(),
                        process.getId(),
                        response.getFailureDetail());
                transitionToError(process, message);
            } else {
                monitor.debug(format("TransferProcessManager: attempt #%d failed to initiate data transfer. TransferProcess %s stays in state %s. Cause: %s",
                        process.getStateCount(),
                        process.getId(),
                        TransferProcessStates.from(process.getState()),
                        response.getFailureDetail()));
                process.transitionProvisioned();
                updateTransferProcess(process, l -> l.preProvisioned(process));
            }
        }
    }

    private void sendConsumerRequest(TransferProcess process, DataRequest dataRequest) {
        monitor.debug(format("TransferProcessManager: Sending process %s request to %s", process.getId(), dataRequest.getConnectorAddress()));
        dispatcherRegistry.send(Object.class, dataRequest, process::getId)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        sendConsumerRequestSuccess(process);
                    } else {
                        sendCustomerRequestFailure(process, throwable);
                    }
                });
    }

    private void sendConsumerRequestSuccess(TransferProcess transferProcess) {
        transferProcess.transitionRequested();
        updateTransferProcess(transferProcess, l -> l.preRequested(transferProcess));
        observable.invokeForEach(l -> l.requested(transferProcess));
        monitor.debug("TransferProcessManager: Process " + transferProcess.getId() + " is now " + TransferProcessStates.from(transferProcess.getState()));
    }

    private void sendCustomerRequestFailure(TransferProcess transferProcess, Throwable e) {
        if (sendRetryManager.retriesExhausted(transferProcess)) {
            var message = format("TransferProcessManager: attempt #%d failed to send transfer. Retry limit exceeded, TransferProcess %s moves to ERROR state. Cause: %s",
                    transferProcess.getStateCount(),
                    transferProcess.getId(),
                    e.getMessage());
            monitor.severe(message, e);
            transitionToError(transferProcess.getId(), e, message);
            return;
        }
        monitor.debug(format("TransferProcessManager: attempt #%d failed to send transfer. TransferProcess %s stays in state %s.",
                transferProcess.getStateCount(),
                transferProcess.getId(),
                TransferProcessStates.from(transferProcess.getState())), e);
        // update state count and timestamp
        transferProcess.transitionRequesting();
        transferProcessStore.update(transferProcess);
    }

    private void updateTransferProcess(TransferProcess transferProcess, Consumer<TransferProcessListener> observe) {
        observable.invokeForEach(observe);
        transferProcessStore.update(transferProcess);
    }

    private void breakLease(TransferProcess process) {
        transferProcessStore.update(process);
    }

    public static class Builder {
        private final TransferProcessManagerImpl manager;

        private Builder() {
            manager = new TransferProcessManagerImpl();
            manager.telemetry = new Telemetry(); // default noop implementation
            manager.executorInstrumentation = ExecutorInstrumentation.noop(); // default noop implementation
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder batchSize(int size) {
            manager.batchSize = size;
            return this;
        }

        public Builder sendRetryManager(SendRetryManager sendRetryManager) {
            manager.sendRetryManager = sendRetryManager;
            return this;
        }

        public Builder waitStrategy(WaitStrategy waitStrategy) {
            manager.waitStrategy = waitStrategy;
            return this;
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

        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public Builder statusCheckerRegistry(StatusCheckerRegistry statusCheckerRegistry) {
            manager.statusCheckerRegistry = statusCheckerRegistry;
            return this;
        }

        public Builder telemetry(Telemetry telemetry) {
            manager.telemetry = telemetry;
            return this;
        }

        public Builder executorInstrumentation(ExecutorInstrumentation executorInstrumentation) {
            manager.executorInstrumentation = executorInstrumentation;
            return this;
        }

        public Builder clock(Clock clock) {
            manager.clock = clock;
            return this;
        }

        public Builder vault(Vault vault) {
            manager.vault = vault;
            return this;
        }

        public Builder typeManager(TypeManager typeManager) {
            manager.typeManager = typeManager;
            return this;
        }

        public Builder commandQueue(CommandQueue<TransferProcessCommand> queue) {
            manager.commandQueue = queue;
            return this;
        }

        public Builder commandRunner(CommandRunner<TransferProcessCommand> runner) {
            manager.commandRunner = runner;
            return this;
        }

        public Builder observable(TransferProcessObservable observable) {
            manager.observable = observable;
            return this;
        }

        public Builder transferProcessStore(TransferProcessStore transferProcessStore) {
            manager.transferProcessStore = transferProcessStore;
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

        public TransferProcessManagerImpl build() {
            Objects.requireNonNull(manager.manifestGenerator, "manifestGenerator cannot be null");
            Objects.requireNonNull(manager.provisionManager, "provisionManager cannot be null");
            Objects.requireNonNull(manager.dataFlowManager, "dataFlowManager cannot be null");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry cannot be null");
            Objects.requireNonNull(manager.monitor, "monitor cannot be null");
            Objects.requireNonNull(manager.executorInstrumentation, "executorInstrumentation cannot be null");
            Objects.requireNonNull(manager.commandQueue, "commandQueue cannot be null");
            Objects.requireNonNull(manager.commandRunner, "commandRunner cannot be null");
            Objects.requireNonNull(manager.statusCheckerRegistry, "statusCheckerRegistry cannot be null!");
            Objects.requireNonNull(manager.observable, "observable cannot be null");
            Objects.requireNonNull(manager.telemetry, "telemetry cannot be null");
            Objects.requireNonNull(manager.policyArchive, "policyArchive cannot be null");
            Objects.requireNonNull(manager.transferProcessStore, "transferProcessStore cannot be null");
            Objects.requireNonNull(manager.addressResolver, "addressResolver cannot be null");
            manager.commandProcessor = new CommandProcessor<>(manager.commandQueue, manager.commandRunner, manager.monitor);

            return manager;
        }
    }

}
