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

package org.eclipse.edc.connector.transfer.process;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.command.TransferProcessCommand;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.command.CommandProcessor;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.StateProcessorImpl;
import org.eclipse.edc.statemachine.retry.SendRetryManager;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.TERMINATING;

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
public class TransferProcessManagerImpl implements TransferProcessManager, ProvisionCallbackDelegate {
    private int batchSize = DEFAULT_BATCH_SIZE;
    private WaitStrategy waitStrategy = () -> DEFAULT_ITERATION_WAIT;
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
    private SendRetryManager sendRetryManager;
    private Clock clock;

    private TransferProcessManagerImpl() {
    }

    public void start() {
        stateMachineManager = StateMachineManager.Builder.newInstance("transfer-process", monitor, executorInstrumentation, waitStrategy)
                .processor(processTransfersInState(INITIAL, this::processInitial))
                .processor(processTransfersInState(PROVISIONING, this::processProvisioning))
                .processor(processTransfersInState(PROVISIONED, this::processProvisioned))
                .processor(processTransfersInState(REQUESTING, this::processRequesting))
                .processor(processTransfersInState(REQUESTED, this::processRequested))
                .processor(processTransfersInState(STARTING, this::processStarting))
                .processor(processTransfersInState(STARTED, this::processStarted))
                .processor(processTransfersInState(COMPLETING, this::processCompleting))
                .processor(processTransfersInState(TERMINATING, this::processTerminating))
                .processor(processTransfersInState(DEPROVISIONING, this::processDeprovisioning))
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

        if (transferProcess.getState() == TERMINATED.code()) {
            monitor.severe(format("TransferProcessManager: transfer process %s is in TERMINATED state, so provisioning could not be completed", transferProcess.getId()));
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
            var message = format("Transitioning transfer process %s to TERMINATED state due to fatal provisioning errors: \n%s", transferProcess.getId(), validationResult.getFailureDetail());
            transitionToTerminating(transferProcess, message);
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

        if (transferProcess.getState() == TERMINATED.code()) {
            monitor.severe(format("TransferProcessManager: transfer process %s is in TERMINATED state, so deprovisioning could not be processed", transferProcess.getId()));
            return;
        }

        var validationResult = responses.stream()
                .filter(AbstractResult::failed)
                .map(this::toFatalError)
                .filter(AbstractResult::failed)
                .reduce(Result::merge)
                .orElse(Result.success());

        if (validationResult.failed()) {
            var message = format("Transitioning transfer process %s failed to deprovision. Errors: \n%s", transferProcess.getId(), validationResult.getFailureDetail());
            transitionToDeprovisioningError(transferProcess, message);
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
                .clock(clock)
                .properties(dataRequest.getProperties())
                .traceContext(telemetry.getCurrentTraceContext())
                .build();

        observable.invokeForEach(l -> l.preCreated(process));
        transferProcessStore.save(process);
        observable.invokeForEach(l -> l.initiated(process));
        monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));

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
                monitor.severe(format("Transitioning transfer process %s to TERMINATED state. Resource manifest cannot be modified to fulfil policy: %s",
                        process.getId(), manifestResult.getFailureMessages()));
                process.transitionTerminating(format("Resource manifest for process %s cannot be modified to fulfil policy.", process.getId()));
                updateTransferProcess(process, l -> l.preError(process));
                return true;
            }
            manifest = manifestResult.getContent();
        } else {
            var assetId = process.getDataRequest().getAssetId();
            var dataAddress = addressResolver.resolveForAsset(assetId);
            if (dataAddress == null) {
                transitionToTerminating(process, "Asset not found: " + assetId);
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
        var dataRequest = process.getDataRequest();

        var policy = policyArchive.findPolicyForContract(dataRequest.getContractId());

        var resources = process.getResourcesToProvision();
        provisionManager.provision(resources, policy)
                .whenComplete((responses, throwable) -> {
                    if (throwable == null) {
                        handleProvisionResult(process.getId(), responses);
                    } else {
                        transitionToTerminating(process.getId(), throwable, "Error during provisioning");
                    }
                });

        return true;
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
            process.transitionRequesting();
            updateTransferProcess(process, l -> l.preRequesting(process));
        } else {
            process.transitionStarting();
            updateTransferProcess(process, l -> {});
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
        if (process.getType() == PROVIDER) {
            return false; // should never happen: a provider transfer cannot be REQUESTING
        }

        var dataRequest = process.getDataRequest();
        return new TransferProcessSendRetryManager()
                .onSuccess(this::transitToRequested)
                .onFailure(TransferProcess::transitionRequesting)
                .executeAsync(
                    format("Send %s to %s", dataRequest.getClass().getSimpleName(), dataRequest.getConnectorAddress()),
                        process,
                    () -> dispatcherRegistry.send(Object.class, dataRequest, process::getId));
    }

    /**
     * Process REQUESTED transfer<p> If is managed or there are provisioned resources set {@link TransferProcess#transitionStarting()},
     * do nothing otherwise
     *
     * @param process the REQUESTED transfer fetched
     * @return if the transfer has been processed or not
     * @deprecated this method and the related processor could be removed when Dataspace Protocol takes over
     */
    @WithSpan
    @Deprecated(since = "milestone9")
    private boolean processRequested(TransferProcess process) {
        var dataRequest = process.getDataRequest();
        if (!"ids-multipart".equals(dataRequest.getProtocol())) {
            return false;
        }
        if (!dataRequest.isManagedResources() || (process.getProvisionedResourceSet() != null && !process.getProvisionedResourceSet().empty())) {
            transitToStarted(process);
            return true;
        } else {
            monitor.debug("Process " + process.getId() + " does not yet have provisioned resources, will stay in " + TransferProcessStates.REQUESTED);
            return false;
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
        if (CONSUMER == process.getType()) {
            // should never happen: a consumer transfer cannot be STARTING
            return false;
        }

        var dataRequest = process.getDataRequest();
        var contentAddress = process.getContentDataAddress();

        var policy = policyArchive.findPolicyForContract(dataRequest.getContractId());

        return new TransferProcessSendRetryManager()
                .onSuccess(p -> {
                    var message = TransferStartMessage.Builder.newInstance()
                            .protocol(dataRequest.getProtocol())
                            .connectorAddress(dataRequest.getConnectorAddress()) // TODO: is this correct? it shouldn't for provider.
                            .build();

                    return new TransferProcessSendRetryManager()
                            .onSuccess(this::transitToStarted)
                            .onFailure(TransferProcess::transitionStarting)
                            .executeAsync(
                                    format("Send %s to %s", dataRequest.getClass().getSimpleName(), dataRequest.getConnectorAddress()),
                                    process,
                                    () -> dispatcherRegistry.send(Object.class, message, process::getId)
                            );
                })
                .onFailure(TransferProcess::transitionStarting)
                .executeSync("Initiate data flow", process, () -> dataFlowManager.initiate(dataRequest, contentAddress, policy));
    }

    /**
     * Process STARTED transfer<p> if is completed or there's no checker and it's not managed, set to COMPLETE,
     * nothing otherwise.
     *
     * @param process the STARTED transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processStarted(TransferProcess process) {
        if (process.getType() != CONSUMER) {
            return false;
        }

        if (sendRetryManager.shouldDelay(process)) {
            breakLease(process);
            return false;
        }

        var checker = statusCheckerRegistry.resolve(process.getDataRequest().getDestinationType());
        if (checker == null) {
            if (process.getDataRequest().isManagedResources()) {
                monitor.warning(format("No checker found for process %s. The process will not advance to the COMPLETED state.", process.getId()));
                return false;
            } else {
                //no checker, transition the process to the COMPLETED state automatically
                transitionToCompleting(process);
            }
            return true;
        } else {
            List<ProvisionedResource> resources = process.getDataRequest().isManagedResources() ? process.getProvisionedResourceSet().getResources() : emptyList();
            if (checker.isComplete(process, resources)) {
                transitionToCompleting(process);
                return true;
            } else {
                monitor.debug(format("Transfer process %s not COMPLETED yet. The process will stay in STARTED.", process.getId()));
                breakLease(process);
                return false;
            }
        }
    }

    /**
     * Process COMPLETING transfer<p> Send COMPLETED message to counter-part
     *
     * @param process the COMPLETING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processCompleting(TransferProcess process) {
        var dataRequest = process.getDataRequest();
        var message = TransferCompletionMessage.Builder.newInstance()
                .protocol(dataRequest.getProtocol())
                .connectorAddress(dataRequest.getConnectorAddress())
                .build();

        return new TransferProcessSendRetryManager()
                .onSuccess(this::transitToCompleted)
                .onFailure(TransferProcess::transitionCompleting)
                .executeAsync(
                        format("Send %s to %s", dataRequest.getClass().getSimpleName(), dataRequest.getConnectorAddress()),
                        process,
                        () -> dispatcherRegistry.send(Object.class, message, process::getId)
                );
    }

    /**
     * Process TERMINATING transfer<p> Send TERMINATED message to counter-part, unless it is CONSUMER and not yet
     * REQUESTED, because in that case the counter-part does know nothing about the transfer yet
     *
     * @param process the TERMINATING transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processTerminating(TransferProcess process) {
        if (process.getType() == CONSUMER && process.getState() < REQUESTED.code()) {
            return transitToTerminated(process);
        }

        var dataRequest = process.getDataRequest();
        var message = TransferTerminationMessage.Builder.newInstance()
                .connectorAddress(dataRequest.getConnectorAddress())
                .protocol(dataRequest.getProtocol())
                .build();

        return new TransferProcessSendRetryManager()
                .onSuccess(this::transitToTerminated)
                .onFailure(TransferProcess::transitionTerminating)
                .executeAsync(
                        format("Send %s to %s", dataRequest.getClass().getSimpleName(), dataRequest.getConnectorAddress()),
                        process,
                        () -> dispatcherRegistry.send(Object.class, message, process::getId)
                );
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

        var dataRequest = process.getDataRequest();

        var policy = policyArchive.findPolicyForContract(dataRequest.getContractId());

        var resourcesToDeprovision = process.getResourcesToDeprovision();

        provisionManager.deprovision(resourcesToDeprovision, policy)
                .whenComplete((responses, throwable) -> {
                    if (throwable == null) {
                        handleDeprovisionResult(process.getId(), responses);
                    } else {
                        transitionToDeprovisioningError(process.getId(), throwable);
                    }
                });

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
            transferProcessStore.save(transferProcess);
            observable.invokeForEach(l -> l.provisioned(transferProcess));
        } else if (responses.stream().anyMatch(ProvisionResponse::isInProcess)) {
            transferProcess.transitionProvisioningRequested();
            transferProcessStore.save(transferProcess);
            observable.invokeForEach(l -> l.provisioningRequested(transferProcess));
        } else {
            transferProcessStore.save(transferProcess);
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
            transferProcessStore.save(transferProcess);
            observable.invokeForEach(l -> l.deprovisioned(transferProcess));
        } else if (results.stream().anyMatch(DeprovisionedResource::isInProcess)) {
            transferProcess.transitionDeprovisioningRequested();
            transferProcessStore.save(transferProcess);
            observable.invokeForEach(l -> l.deprovisioningRequested(transferProcess));
        } else {
            transferProcessStore.save(transferProcess);
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
    private void transitionToCompleting(TransferProcess process) {
        process.transitionCompleting();
        updateTransferProcess(process, l -> { });
    }

    @WithSpan
    private boolean transitToCompleted(TransferProcess p) {
        p.transitionCompleted();
        updateTransferProcess(p, l -> l.preCompleted(p));
        observable.invokeForEach(l -> l.completed(p));
        return true;
    }

    private void transitionToTerminating(String id, Throwable throwable, String message) {
        var transferProcess = transferProcessStore.find(id);
        if (transferProcess == null) {
            monitor.severe(format("TransferProcessManager: no TransferProcess found with id %s", id));
            return;
        }

        transitionToTerminating(transferProcess, message, throwable);
    }

    private boolean transitToTerminated(TransferProcess process) {
        process.transitionTerminated();
        updateTransferProcess(process, l -> l.preTerminated(process));
        observable.invokeForEach(l -> l.terminated(process));
        return true;
    }

    private void transitionToTerminating(TransferProcess process, String message, Throwable... errors) {
        monitor.severe(message, errors);
        process.transitionTerminating(message);
        updateTransferProcess(process, l -> { });
    }

    private void transitionToDeprovisioningError(String processId, Throwable throwable) {
        var transferProcess = transferProcessStore.find(processId);
        if (transferProcess == null) {
            monitor.severe(format("TransferProcessManager: no TransferProcess found with id %s", processId));
            return;
        }

        transitionToDeprovisioningError(transferProcess, throwable.getMessage());
    }

    private void transitionToDeprovisioningError(TransferProcess transferProcess, String message) {
        monitor.severe(message);
        transferProcess.transitionDeprovisioned(message);
        updateTransferProcess(transferProcess, l -> l.preDeprovisioned(transferProcess));
        observable.invokeForEach(l -> l.deprovisioned(transferProcess));
    }

    private boolean processConsumerRequest(TransferProcess process, DataRequest dataRequest) {
        if (sendRetryManager.shouldDelay(process)) {
            breakLease(process);
            return false;
        }

        sendConsumerRequest(process, dataRequest);
        return true;
    }

    private void sendConsumerRequest(TransferProcess process, DataRequest dataRequest) {
        monitor.debug(format("TransferProcessManager: Sending process %s request to %s", process.getId(), dataRequest.getConnectorAddress()));
        dispatcherRegistry.send(Object.class, dataRequest)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        transitToRequested(process);
                    } else {
                        handlingSendingRequestedFailure(process, throwable);
                    }
                });
    }

    private void transitToRequested(TransferProcess transferProcess) {
        transferProcess.transitionRequested();
        updateTransferProcess(transferProcess, l -> l.preRequested(transferProcess));
        observable.invokeForEach(l -> l.requested(transferProcess));
        return true;
    }

    private boolean transitToStarted(TransferProcess process) {
        process.transitionStarted();
        updateTransferProcess(process, l -> l.preStarted(process));
        observable.invokeForEach(l -> l.started(process));
        return true;
    }

    private void updateTransferProcess(TransferProcess transferProcess, Consumer<TransferProcessListener> observe) {
        observable.invokeForEach(observe);
        transferProcessStore.save(transferProcess);
        monitor.debug("Process " + transferProcess.getId() + " is now " + TransferProcessStates.from(transferProcess.getState()));
    }

    private void breakLease(TransferProcess process) {
        transferProcessStore.save(process);
    }

    @NotNull
    private Result<Void> toFatalError(StatusResult<?> result) {
        if (result.fatalError()) {
            return Result.failure(result.getFailureMessages());
        } else {
            return Result.success();
        }
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

    /**
     * Helper class that wraps the behavior needed to achieve retry mechanism within the state machine:
     * - waits if needed
     * - execute the action
     * - if it works, execute the onSuccessHandler
     * - if not, execute the onFailureHandler
     */
    private class TransferProcessSendRetryManager {
        private Function<TransferProcess, Boolean> onSuccessHandler;
        private Consumer<TransferProcess> onFailureHandler;

        public TransferProcessSendRetryManager onSuccess(Function<TransferProcess, Boolean> onSuccessHandler) {
            this.onSuccessHandler = onSuccessHandler;
            return this;
        }

        /**
         * onFailure handler, it should not save the TransferProcess as that's done by the RetryManager
         */
        public TransferProcessSendRetryManager onFailure(Consumer<TransferProcess> onFailureHandler) {
            this.onFailureHandler = onFailureHandler;
            return this;
        }

        public boolean executeAsync(String description, TransferProcess transferProcess, Supplier<CompletableFuture<?>> actionSupplier) {
            Objects.requireNonNull(onSuccessHandler, "onSuccessHandler");
            Objects.requireNonNull(onFailureHandler, "onFailureHandler");

            if (sendRetryManager.shouldDelay(transferProcess)) {
                breakLease(transferProcess);
                return false;
            }

            monitor.debug(format("TransferProcessManager: ID %s. %s", transferProcess.getId(), description));
            actionSupplier.get()
                    .whenComplete((result, throwable) -> {
                        var process = transferProcessStore.find(transferProcess.getId());
                        if (process == null) {
                            monitor.severe(format("TransferProcessManager: Process %s does not exist", transferProcess.getId()));
                            return;
                        }

                        if (throwable == null) {
                            onSuccessHandler.apply(process);
                        } else {
                            handleFailure(throwable, description, process);
                        }
                    });

            return true;
        }

        public boolean executeSync(String description, TransferProcess transferProcess, Supplier<StatusResult<?>> actionSupplier) {
            Objects.requireNonNull(onSuccessHandler, "onSuccessHandler");
            Objects.requireNonNull(onFailureHandler, "onFailureHandler");

            if (sendRetryManager.shouldDelay(transferProcess)) {
                breakLease(transferProcess);
                return false;
            }

            monitor.debug(format("TransferProcessManager: ID %s. %s", transferProcess.getId(), description));
            var result = actionSupplier.get();
            if (result.succeeded()) {
                return onSuccessHandler.apply(transferProcess);
            } else {
                if (result.fatalError()) {
                    var message = format("TransferProcessManager: ID %s. Fatal error while %s. Error details: %s", transferProcess.getId(), description, result.getFailureDetail());
                    monitor.severe(message);
                    transferProcess.transitionTerminating(message);
                    transferProcessStore.save(transferProcess);
                } else {
                    handleFailure(new EdcException(result.getFailureDetail()), description, transferProcess);
                }

            }

            return true;
        }

        private void handleFailure(Throwable throwable, String description, TransferProcess transferProcess) {
            if (sendRetryManager.retriesExhausted(transferProcess)) {
                var message = format("TransferProcessManager: ID %s. Attempt #%d failed to %s. Retry limit exceeded, TransferProcess transits to TERMINATED state. Cause: %s",
                        transferProcess.getId(),
                        transferProcess.getStateCount(),
                        description,
                        throwable.getMessage());
                monitor.severe(message, throwable);
                transferProcess.transitionTerminating(message);
            } else {
                var message = format("TransferProcessManager: ID %s. Attempt #%d failed to %s. TransferProcess stays in state %s. Cause: %s",
                        transferProcess.getId(),
                        transferProcess.getStateCount(),
                        description,
                        TransferProcessStates.from(transferProcess.getState()),
                        throwable.getMessage());

                monitor.debug(message, throwable);

                onFailureHandler.accept(transferProcess);
            }
            transferProcessStore.save(transferProcess);
        }
    }

}
