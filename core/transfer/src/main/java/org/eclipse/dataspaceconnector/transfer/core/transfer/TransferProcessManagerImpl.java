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
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
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

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

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

        handleProvisionResult(transferProcess, responses);
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

        handleDeprovisionResult(transferProcess, responses);
    }

    private StatusResult<String> initiateRequest(TransferProcess.Type type, DataRequest dataRequest) {
        // make the request idempotent: if the process exists, return
        var processId = transferProcessStore.processIdForTransferId(dataRequest.getId());
        if (processId != null) {
            return StatusResult.success(processId);
        }
        var id = randomUUID().toString();
        var process = TransferProcess.Builder.newInstance()
                .id(id)
                .dataRequest(dataRequest)
                .type(type)
                .createdTimestamp(clock.millis())
                .traceContext(telemetry.getCurrentTraceContext())
                .build();
        if (process.getState() == TransferProcessStates.UNSAVED.code()) {
            process.transitionInitial();
        }
        observable.invokeForEach(l -> l.preCreated(process));
        transferProcessStore.create(process);
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
            manifest = manifestGenerator.generateConsumerResourceManifest(dataRequest, policy);
        } else {
            var assetId = process.getDataRequest().getAssetId();
            var dataAddress = addressResolver.resolveForAsset(assetId);
            if (dataAddress == null) {
                process.transitionError("Asset not found: " + assetId);
                updateTransferProcess(process, l -> l.preError(process));
            }
            // default the content address to the asset address; this may be overridden during provisioning
            process.addContentDataAddress(dataAddress);
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
            processProviderRequest(process);
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
                monitor.info(format("No checker found for process %s. The process will not advance to the COMPLETED state.", process.getId()));
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
                monitor.info(format("Transfer process %s not COMPLETED yet. The process will not advance to the COMPLETED state.", process.getId()));
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
        transferProcessStore.update(process);
        observable.invokeForEach(l -> l.preEnded(process));
        monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
        return true;
    }

    private void handleProvisionResult(TransferProcess transferProcess, List<StatusResult<ProvisionResponse>> responses) {
        var fatalErrors = new ArrayList<String>();
        responses.forEach(result -> {
            if (result.failed()) {
                // Record fatal failure so that transfer process can be transitioned to the error state; if non-fatal, skip
                var status = result.getFailure().status();
                if (ResponseStatus.FATAL_ERROR == status) {
                    fatalErrors.addAll(result.getFailure().getMessages());
                }
                return;
            } else if (result.getContent().isInProcess()) {
                // Still in process, ignore and continue processing other resources
                return;
            }

            var response = result.getContent();
            var provisionedResource = response.getResource();

            if (provisionedResource instanceof ProvisionedDataAddressResource) {
                var dataAddressResource = (ProvisionedDataAddressResource) provisionedResource;
                var secretToken = response.getSecretToken();
                if (secretToken != null) {
                    var keyName = dataAddressResource.getResourceName();
                    var secretResult = vault.storeSecret(keyName, typeManager.writeValueAsString(secretToken));
                    if (secretResult.failed()) {
                        fatalErrors.add(format("Error storing secret in vault with key %s for transfer process %s: \n %s",
                                keyName, transferProcess.getId(), join("\n", secretResult.getFailureMessages())));
                    }
                    dataAddressResource.getDataAddress().setKeyName(keyName);
                }
                handleProvisionDataAddressResource(dataAddressResource, transferProcess);
            }
            // update the transfer process with the provisioned resource
            transferProcess.addProvisionedResource(provisionedResource);
        });

        if (!fatalErrors.isEmpty()) {
            var errors = join("\n", fatalErrors);
            monitor.severe(format("Transitioning transfer process %s to ERROR state due to fatal provisioning errors: \n%s", transferProcess.getId(), errors));
            transferProcess.transitionError("Fatal provisioning errors encountered. See logs for details.");
            transferProcessStore.update(transferProcess);
        } else if (transferProcess.provisioningComplete()) {
            transferProcess.transitionProvisioned();
            transferProcessStore.update(transferProcess);
            observable.invokeForEach(l -> l.preProvisioned(transferProcess));
        } else {
            transferProcessStore.update(transferProcess);
        }
    }

    private void handleProvisionDataAddressResource(ProvisionedDataAddressResource resource, TransferProcess transferProcess) {
        var dataAddress = resource.getDataAddress();
        if (resource instanceof ProvisionedDataDestinationResource) {
            // a data destination was provisioned by a consumer
            transferProcess.getDataRequest().updateDestination(dataAddress);
        } else if (resource instanceof ProvisionedContentResource) {
            // content for the data transfer was provisioned by the provider
            transferProcess.addContentDataAddress(dataAddress);
        }
    }

    private void handleDeprovisionResult(TransferProcess transferProcess, List<StatusResult<DeprovisionedResource>> results) {
        var fatalErrors = new ArrayList<String>();
        results.forEach(result -> {
            if (result.failed()) {
                // Record fatal failure so that transfer process can be transitioned to the error state; if non-fatal, skip
                var status = result.getFailure().status();
                if (status == ResponseStatus.FATAL_ERROR) {
                    fatalErrors.addAll(result.getFailure().getMessages());
                }
                return;
            } else if (result.getContent().isInProcess()) {
                // Still in process, ignore and continue processing other deprovisioned resources
                return;
            }
            var deprovisionedResource = result.getContent();

            var provisionedResource = transferProcess.getProvisionedResource(deprovisionedResource.getProvisionedResourceId());
            if (provisionedResource == null) {
                monitor.severe("Received a deprovision result for a provisioned resource that was not found. Skipping.");
                return;
            }

            if (provisionedResource.hasToken() && provisionedResource instanceof ProvisionedDataAddressResource) {
                removeDeprovisionedSecrets((ProvisionedDataAddressResource) provisionedResource, transferProcess);
            }

            transferProcess.addDeprovisionedResource(deprovisionedResource);

        });

        if (!fatalErrors.isEmpty()) {
            var errors = join("\n", fatalErrors);
            monitor.severe(format("Transitioning transfer process %s to ERROR state due to fatal deprovisioning errors: \n%s", transferProcess.getId(), errors));
            transferProcess.transitionError("Fatal depprovisioning errors encountered. See logs for details.");
            transferProcessStore.update(transferProcess);
        } else if (transferProcess.deprovisionComplete()) {
            transferProcess.transitionDeprovisioned();
            transferProcessStore.update(transferProcess);
            observable.invokeForEach(l -> l.preDeprovisioned(transferProcess));
        } else {
            transferProcessStore.update(transferProcess);
        }
    }

    private void removeDeprovisionedSecrets(ProvisionedDataAddressResource provisionedResource, TransferProcess transferProcess) {
        var keyName = provisionedResource.getResourceName();
        var result = vault.deleteSecret(keyName);
        if (result.failed()) {
            monitor.severe(format("Error deleting secret from vault with key %s for transfer process %s: \n %s",
                    keyName, transferProcess.getId(), join("\n", result.getFailureMessages())));
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
    }

    private void transitionToError(String id, Throwable throwable, String message) {
        var transferProcess = transferProcessStore.find(id);
        if (transferProcess == null) {
            monitor.severe(format("TransferProcessManager: no TransferProcess found with id %s", id));
            return;
        }

        monitor.severe(message, throwable);
        transferProcess.transitionError(format("%s: %s", message, throwable.getLocalizedMessage()));
        updateTransferProcess(transferProcess, l -> l.preError(transferProcess));
    }

    private void processProviderRequest(TransferProcess process) {
        var dataRequest = process.getDataRequest();
        var contentAddress = process.getContentDataAddress();

        var policy = policyArchive.findPolicyForContract(dataRequest.getContractId());

        var response = dataFlowManager.initiate(dataRequest, contentAddress, policy);

        if (response.succeeded()) {
            process.transitionInProgressOrStreaming();
            updateTransferProcess(process, l -> l.preInProgress(process));
        } else {
            if (ResponseStatus.ERROR_RETRY == response.getFailure().status()) {
                monitor.severe("Error processing transfer request. Setting to retry: " + process.getId());
                process.transitionProvisioned();
                updateTransferProcess(process, l -> l.preProvisioned(process));
            } else {
                monitor.severe(format("Fatal error processing transfer request: %s. Error details: %s", process.getId(), join(", ", response.getFailureMessages())));
                process.transitionError(response.getFailureMessages().stream().findFirst().orElse(""));
                updateTransferProcess(process, l -> l.preError(process));
            }
        }
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
        dispatcherRegistry.send(Object.class, dataRequest, process::getId)
                .thenApply(result -> {
                    sendConsumerRequestSuccess(process);
                    return result;
                })
                .exceptionally(e -> {
                    sendCustomerRequestFailure(process, e);
                    return e;
                });
    }

    private void sendConsumerRequestSuccess(TransferProcess transferProcess) {
        transferProcess.transitionRequested();
        updateTransferProcess(transferProcess, l -> l.preRequested(transferProcess));
        monitor.debug("TransferProcessManager: Process " + transferProcess.getId() + " is now " + TransferProcessStates.from(transferProcess.getState()));
    }

    private void sendCustomerRequestFailure(TransferProcess transferProcess, Throwable e) {
        if (sendRetryManager.retriesExhausted(transferProcess)) {
            monitor.info(format("TransferProcessManager: attempt #%d failed to send transfer. Retry limit exceeded, TransferProcess %s moves to ERROR state.",
                    transferProcess.getStateCount(),
                    transferProcess.getId()), e);
            transitionToError(transferProcess.getId(), e, "Retry limit exceeded");
            return;
        }
        monitor.info(format("TransferProcessManager: attempt #%d failed to send transfer. TransferProcess %s stays in state %s.",
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
