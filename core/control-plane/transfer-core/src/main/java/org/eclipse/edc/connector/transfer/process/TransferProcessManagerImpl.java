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
 *
 */

package org.eclipse.edc.connector.transfer.process;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.connector.transfer.spi.status.StatusCheckerRegistry;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataAddressResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedDataDestinationResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceManifest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.transfer.spi.types.command.TransferProcessCommand;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.spi.asset.DataAddressResolver;
import org.eclipse.edc.spi.command.CommandProcessor;
import org.eclipse.edc.spi.command.CommandQueue;
import org.eclipse.edc.spi.command.CommandRunner;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.retry.ExponentialWaitStrategy;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ExecutorInstrumentation;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.statemachine.StateMachineManager;
import org.eclipse.edc.statemachine.StateProcessorImpl;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessConfiguration;
import org.eclipse.edc.statemachine.retry.EntityRetryProcessFactory;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_BATCH_SIZE;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_ITERATION_WAIT;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_SEND_RETRY_BASE_DELAY;
import static org.eclipse.edc.connector.transfer.TransferCoreExtension.DEFAULT_SEND_RETRY_LIMIT;
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
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;

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
    private EntityRetryProcessFactory entityRetryProcessFactory;
    private Clock clock;
    private EntityRetryProcessConfiguration entityRetryProcessConfiguration = defaultEntityRetryProcessConfiguration();
    private ProtocolWebhook protocolWebhook;

    private TransferProcessManagerImpl() {
    }

    public void start() {
        entityRetryProcessFactory = new EntityRetryProcessFactory(monitor, clock, entityRetryProcessConfiguration);
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
    public StatusResult<TransferProcess> initiateConsumerRequest(TransferRequest transferRequest) {
        // make the request idempotent: if the process exists, return
        var dataRequest = transferRequest.getDataRequest();
        var processId = transferProcessStore.processIdForDataRequestId(dataRequest.getId());
        if (processId != null) {
            return StatusResult.success(transferProcessStore.findById(processId));
        }
        var id = randomUUID().toString();
        var process = TransferProcess.Builder.newInstance()
                .id(id)
                .dataRequest(dataRequest)
                .type(CONSUMER)
                .clock(clock)
                .properties(dataRequest.getProperties())
                .callbackAddresses(transferRequest.getCallbackAddresses())
                .traceContext(telemetry.getCurrentTraceContext())
                .build();

        observable.invokeForEach(l -> l.preCreated(process));
        update(process);
        observable.invokeForEach(l -> l.initiated(process));
        monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));

        return StatusResult.success(process);
    }

    @Override
    public void enqueueCommand(TransferProcessCommand command) {
        commandQueue.enqueue(command);
    }

    @Override
    public Result<Void> runCommand(TransferProcessCommand command) {
        return commandRunner.runCommand(command);
    }

    @Override
    public void handleProvisionResult(String processId, List<StatusResult<ProvisionResponse>> responses) {
        var transferProcess = transferProcessStore.findById(processId);
        if (transferProcess == null) {
            monitor.severe("TransferProcessManager: no TransferProcess found for provisioned resources");
            return;
        }

        handleProvisionResult(transferProcess, responses);
    }

    @Override
    public void handleDeprovisionResult(String processId, List<StatusResult<DeprovisionedResource>> responses) {
        var transferProcess = transferProcessStore.findById(processId);
        if (transferProcess == null) {
            monitor.severe("TransferProcessManager: no TransferProcess found for deprovisioned resources");
            return;
        }

        handleDeprovisionResult(transferProcess, responses);
    }

    private void handleProvisionResult(TransferProcess transferProcess, List<StatusResult<ProvisionResponse>> responses) {
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

    private void handleDeprovisionResult(TransferProcess transferProcess, List<StatusResult<DeprovisionedResource>> responses) {
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

    /**
     * Process INITIAL transfer<p> set it to PROVISIONING
     *
     * @param process the INITIAL transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processInitial(TransferProcess process) {
        var dataRequest = process.getDataRequest();

        var contractId = dataRequest.getContractId();
        var policy = policyArchive.findPolicyForContract(contractId);

        if (policy == null) {
            transitionToTerminating(process, "Policy not found for contract: " + contractId);
            return true;
        }

        ResourceManifest manifest;
        if (process.getType() == CONSUMER) {
            var manifestResult = manifestGenerator.generateConsumerResourceManifest(dataRequest, policy);
            if (manifestResult.failed()) {
                monitor.severe(format("Transitioning transfer process %s to TERMINATED state. Resource manifest cannot be modified to fulfil policy: %s",
                        process.getId(), manifestResult.getFailureMessages()));
                transitionToTerminating(process, format("Resource manifest for process %s cannot be modified to fulfil policy.", process.getId()));
                return true;
            }
            manifest = manifestResult.getContent();
        } else {
            var assetId = process.getDataRequest().getAssetId();
            var dataAddress = addressResolver.resolveForAsset(assetId);
            if (dataAddress == null) {
                transitionToTerminating(process, "Asset not found: " + assetId);
                return true;
            }
            // default the content address to the asset address; this may be overridden during provisioning
            process.setContentDataAddress(dataAddress);
            manifest = manifestGenerator.generateProviderResourceManifest(dataRequest, dataAddress, policy);
        }

        process.transitionProvisioning(manifest);
        observable.invokeForEach(l -> l.preProvisioning(process));
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
        var dataRequest = process.getDataRequest();

        var policy = policyArchive.findPolicyForContract(dataRequest.getContractId());

        var resources = process.getResourcesToProvision();

        return entityRetryProcessFactory.doAsyncProcess(process, () -> provisionManager.provision(resources, policy))
                .entityRetrieve(transferProcessStore::findById)
                .onSuccess(this::handleProvisionResult)
                .onFailure((t, throwable) -> transitionToProvisioning(t))
                .onRetryExhausted((t, throwable) -> transitionToTerminating(t, format("Error during provisioning: %s", throwable.getMessage())))
                .onDelay(this::breakLease)
                .execute("Provisioning");
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
        if (process.getType() == PROVIDER) {
            return false; // should never happen: a provider transfer cannot be REQUESTING
        }

        var dataRequest = process.getDataRequest();

        var message = TransferRequestMessage.Builder.newInstance()
                .id(dataRequest.getId())
                .protocol(dataRequest.getProtocol())
                .connectorId(dataRequest.getConnectorId())
                .counterPartyAddress(dataRequest.getConnectorAddress())
                .callbackAddress(protocolWebhook.url())
                .dataDestination(dataRequest.getDataDestination())
                .properties(dataRequest.getProperties())
                .assetId(dataRequest.getAssetId())
                .contractId(dataRequest.getContractId())
                .build();

        var description = format("Send %s to %s", message.getClass().getSimpleName(), message.getCounterPartyAddress());
        return entityRetryProcessFactory.doAsyncProcess(process, () -> dispatcherRegistry.send(Object.class, message))
                .entityRetrieve(id -> transferProcessStore.findById(id))
                .onSuccess((t, content) -> transitionToRequested(t))
                .onRetryExhausted((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .onFailure((t, throwable) -> transitionToRequesting(t))
                .onDelay(this::breakLease)
                .execute(description);
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
            transitionToStarted(process);
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

        var description = "Initiate data flow";

        return entityRetryProcessFactory.doSyncProcess(process, () -> dataFlowManager.initiate(dataRequest, contentAddress, policy))
                .onSuccess(this::sendTransferStartMessage)
                .onFatalError((p, failure) -> transitionToTerminating(p, failure.getFailureDetail()))
                .onFailure((t, failure) -> transitionToStarting(t))
                .onRetryExhausted((p, failure) -> transitionToTerminating(p, failure.getFailureDetail()))
                .onDelay(this::breakLease)
                .execute(description);
    }

    @WithSpan
    private void sendTransferStartMessage(TransferProcess process, DataFlowResponse dataFlowResponse) {
        var dataRequest = process.getDataRequest();
        var message = TransferStartMessage.Builder.newInstance()
                .protocol(dataRequest.getProtocol())
                .dataAddress(dataFlowResponse.getDataAddress())
                .counterPartyAddress(dataRequest.getConnectorAddress())
                .processId(dataRequest.getId())
                .build();

        var description = format("Send %s to %s", dataRequest.getClass().getSimpleName(), dataRequest.getConnectorAddress());

        entityRetryProcessFactory.doAsyncProcess(process, () -> dispatcherRegistry.send(Object.class, message))
                .entityRetrieve(id -> transferProcessStore.findById(id))
                .onSuccess((t, content) -> transitionToStarted(t))
                .onFailure((t, throwable) -> transitionToStarting(t))
                .onRetryExhausted((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .onDelay(this::breakLease)
                .execute(description);
    }

    /**
     * Process STARTED transfer<p> if is completed or there's no checker and it's not managed, set to COMPLETE,
     * nothing otherwise.
     *
     * @param transferProcess the STARTED transfer fetched
     * @return if the transfer has been processed or not
     */
    @WithSpan
    private boolean processStarted(TransferProcess transferProcess) {
        if (transferProcess.getType() != CONSUMER) {
            return false;
        }

        return entityRetryProcessFactory.doSimpleProcess(transferProcess, () -> checkCompletion(transferProcess))
                .onDelay(this::breakLease)
                .execute("Check completion");
    }

    @NotNull
    private Boolean checkCompletion(TransferProcess transferProcess) {
        var checker = statusCheckerRegistry.resolve(transferProcess.getDataRequest().getDestinationType());
        if (checker == null) {
            if (transferProcess.getDataRequest().isManagedResources()) {
                monitor.warning(format("No checker found for process %s. The process will not advance to the COMPLETED state.", transferProcess.getId()));
                return false;
            } else {
                //no checker, transition the process to the COMPLETED state automatically
                transitionToCompleting(transferProcess);
            }
            return true;
        } else {
            List<ProvisionedResource> resources = transferProcess.getDataRequest().isManagedResources() ? transferProcess.getProvisionedResourceSet().getResources() : emptyList();
            if (checker.isComplete(transferProcess, resources)) {
                transitionToCompleting(transferProcess);
                return true;
            } else {
                monitor.debug(format("Transfer process %s not COMPLETED yet. The process will stay in STARTED.", transferProcess.getId()));
                breakLease(transferProcess);
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
                .counterPartyAddress(dataRequest.getConnectorAddress())
                .processId(dataRequest.getId())
                .build();

        var description = format("Send %s to %s", dataRequest.getClass().getSimpleName(), dataRequest.getConnectorAddress());
        return entityRetryProcessFactory.doAsyncProcess(process, () -> dispatcherRegistry.send(Object.class, message))
                .entityRetrieve(id -> transferProcessStore.findById(id))
                .onSuccess((t, content) -> transitionToCompleted(t))
                .onFailure((t, throwable) -> transitionToCompleting(t))
                .onRetryExhausted((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .onDelay(this::breakLease)
                .execute(description);
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
            transitionToTerminated(process);
            return true;
        }

        var dataRequest = process.getDataRequest();
        var message = TransferTerminationMessage.Builder.newInstance()
                .counterPartyAddress(dataRequest.getConnectorAddress())
                .protocol(dataRequest.getProtocol())
                .processId(dataRequest.getId())
                .build();

        var description = format("Send %s to %s", dataRequest.getClass().getSimpleName(), dataRequest.getConnectorAddress());
        return entityRetryProcessFactory.doAsyncProcess(process, () -> dispatcherRegistry.send(Object.class, message))
                .entityRetrieve(id -> transferProcessStore.findById(id))
                .onSuccess((t, content) -> transitionToTerminated(t))
                .onFailure((t, throwable) -> transitionToTerminating(t, throwable.getMessage(), throwable))
                .onRetryExhausted(this::transitionToTerminated)
                .onDelay(this::breakLease)
                .execute(description);
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

        return entityRetryProcessFactory.doAsyncProcess(process, () -> provisionManager.deprovision(resourcesToDeprovision, policy))
                        .entityRetrieve(transferProcessStore::findById)
                        .onDelay(this::breakLease)
                        .onSuccess(this::handleDeprovisionResult)
                        .onFailure((t, throwable) -> transitionToDeprovisioning(t))
                        .onRetryExhausted((t, throwable) -> transitionToDeprovisioningError(t, throwable.getMessage()))
                        .execute("deprovisioning");
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
            update(transferProcess);
            observable.invokeForEach(l -> l.provisioned(transferProcess));
        } else if (responses.stream().anyMatch(ProvisionResponse::isInProcess)) {
            transferProcess.transitionProvisioningRequested();
            update(transferProcess);
            observable.invokeForEach(l -> l.provisioningRequested(transferProcess));
        } else {
            update(transferProcess);
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
            update(transferProcess);
            observable.invokeForEach(l -> l.deprovisioned(transferProcess));
        } else if (results.stream().anyMatch(DeprovisionedResource::isInProcess)) {
            transferProcess.transitionDeprovisioningRequested();
            update(transferProcess);
            observable.invokeForEach(l -> l.deprovisioningRequested(transferProcess));
        } else {
            update(transferProcess);
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
        return new StateProcessorImpl<>(() -> transferProcessStore.nextNotLeased(batchSize, hasState(state.code())), functionWithTraceContext);
    }

    private StateProcessorImpl<TransferProcessCommand> onCommands(Function<TransferProcessCommand, Boolean> process) {
        return new StateProcessorImpl<>(() -> commandQueue.dequeue(5), process);
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

    private void transitionToRequested(TransferProcess transferProcess) {
        transferProcess.transitionRequested();
        observable.invokeForEach(l -> l.preRequested(transferProcess));
        update(transferProcess);
        observable.invokeForEach(l -> l.requested(transferProcess));
    }

    private void transitionToStarting(TransferProcess transferProcess) {
        transferProcess.transitionStarting();
        update(transferProcess);
    }

    private void transitionToStarted(TransferProcess process) {
        process.transitionStarted();
        observable.invokeForEach(l -> l.preStarted(process));
        update(process);
        observable.invokeForEach(l -> l.started(process, TransferProcessStartedData.Builder.newInstance().build()));
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

    private void transitionToTerminating(TransferProcess process, String message, Throwable... errors) {
        monitor.severe(message, errors);
        process.transitionTerminating(message);
        update(process);
    }

    private void transitionToTerminated(TransferProcess process, Throwable throwable) {
        process.setErrorDetail(throwable.getMessage());
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

    private void update(TransferProcess transferProcess) {
        transferProcessStore.updateOrCreate(transferProcess);
        monitor.debug(format("TransferProcess %s is now in state %s", transferProcess.getId(), TransferProcessStates.from(transferProcess.getState())));
    }

    private void breakLease(TransferProcess process) {
        transferProcessStore.updateOrCreate(process);
    }

    @NotNull
    private Result<Void> toFatalError(StatusResult<?> result) {
        if (result.fatalError()) {
            return Result.failure(result.getFailureMessages());
        } else {
            return Result.success();
        }
    }

    @NotNull
    private EntityRetryProcessConfiguration defaultEntityRetryProcessConfiguration() {
        return new EntityRetryProcessConfiguration(DEFAULT_SEND_RETRY_LIMIT, () -> new ExponentialWaitStrategy(DEFAULT_SEND_RETRY_BASE_DELAY));
    }

    public static class Builder {
        private final TransferProcessManagerImpl manager;

        private Builder() {
            manager = new TransferProcessManagerImpl();
            manager.clock = Clock.systemUTC(); // default implementation
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

        public Builder entityRetryProcessConfiguration(EntityRetryProcessConfiguration entityRetryProcessConfiguration) {
            manager.entityRetryProcessConfiguration = entityRetryProcessConfiguration;
            return this;
        }

        public Builder protocolWebhook(ProtocolWebhook protocolWebhook) {
            manager.protocolWebhook = protocolWebhook;
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
            manager.entityRetryProcessFactory = new EntityRetryProcessFactory(manager.monitor, manager.clock, manager.entityRetryProcessConfiguration);

            return manager;
        }
    }

}
