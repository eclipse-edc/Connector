/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.core.transfer;

import io.opentelemetry.extension.annotations.WithSpan;
import org.eclipse.dataspaceconnector.common.stream.EntitiesProcessor;
import org.eclipse.dataspaceconnector.spi.command.CommandProcessor;
import org.eclipse.dataspaceconnector.spi.command.CommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.retry.WaitStrategy;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.telemetry.Telemetry;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommand;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.COMPLETED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONING_REQ;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ERROR;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.IN_PROGRESS;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED_ACK;

/**
 * This transfer process manager receives a {@link TransferProcess} and transitions it through its internal state machine (cf {@link TransferProcessStates}.
 * When submitting a new {@link TransferProcess} it gets created and inserted into the {@link TransferProcessStore}, then returns to the caller.
 * <p>
 * All subsequent state transitions happen asynchronously, the {@code AsyncTransferProcessManager#initiate*Request()} will return immediately.
 * <p>
 * A data transfer processes transitions through a series of states, which allows the system to model both terminating and non-terminating (e.g. streaming) transfers. Transitions
 * occur asynchronously, since long-running processes such as resource provisioning may need to be completed before transitioning to a subsequent state. The permissible state
 * transitions are defined by {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates}.
 * <br/>
 * The transfer manager performs continual iterations, which seek to advance the state of transfer processes, including recovery, in a FIFO state-based ordering.
 * Each iteration will seek to transition a set number of processes for each state to avoid situations where an excessive number of processes in one state block progress of
 * processes in other states.
 * <br/>
 * If no processes need to be transitioned, the transfer manager will wait according to the the defined {@link WaitStrategy} before conducting the next iteration.
 * A wait strategy may implement a backoff scheme.
 */
public class TransferProcessManagerImpl implements TransferProcessManager {
    private final AtomicBoolean active = new AtomicBoolean();

    private int batchSize = 5;
    private WaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds
    private ResourceManifestGenerator manifestGenerator;
    private ProvisionManager provisionManager;
    private TransferProcessStore transferProcessStore;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private DataFlowManager dataFlowManager;
    private ExecutorService executor;
    private StatusCheckerRegistry statusCheckerRegistry;
    private Vault vault;
    private TypeManager typeManager;
    private TransferProcessObservable observable;
    private CommandQueue<TransferProcessCommand> commandQueue;
    private CommandRunner<TransferProcessCommand> commandRunner;
    private CommandProcessor<TransferProcessCommand> commandProcessor;
    private Monitor monitor;
    private Telemetry telemetry;

    private TransferProcessManagerImpl() {
    }

    public void start() {
        active.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::run);
    }

    public void stop() {
        active.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Initiate a consumer request TransferProcess.
     */
    @WithSpan
    @Override
    public TransferInitiateResult initiateConsumerRequest(DataRequest dataRequest) {
        return initiateRequest(CONSUMER, dataRequest);

    }

    /**
     * Initiate a provider request TransferProcess.
     */
    @WithSpan
    @Override
    public TransferInitiateResult initiateProviderRequest(DataRequest dataRequest) {
        return initiateRequest(PROVIDER, dataRequest);
    }

    @Override
    public void enqueueCommand(TransferProcessCommand command) {
        commandQueue.enqueue(command);
    }

    void onProvisionComplete(String processId, List<ProvisionResponse> responses) {
        var transferProcess = transferProcessStore.find(processId);
        if (transferProcess == null) {
            monitor.severe("TransferProcessManager: no TransferProcess found for deprovisioned resources");
            return;
        }

        if (transferProcess.getState() == ERROR.code()) {
            monitor.severe(format("TransferProcessManager: transfer process %s is in ERROR state, so provisioning could not be completed", transferProcess.getId()));
            return;
        }

        responses.stream()
                .map(response -> {
                    var destinationResource = response.getResource();
                    var secretToken = response.getSecretToken();

                    if (destinationResource instanceof ProvisionedDataDestinationResource) {
                        var dataDestinationResource = (ProvisionedDataDestinationResource) destinationResource;
                        DataAddress dataDestination = dataDestinationResource.createDataDestination();

                        if (secretToken != null) {
                            String keyName = dataDestinationResource.getResourceName();
                            vault.storeSecret(keyName, typeManager.writeValueAsString(secretToken));
                            dataDestination.setKeyName(keyName);
                        }

                        transferProcess.getDataRequest().updateDestination(dataDestination);
                    }

                    return destinationResource;
                })
                .forEach(transferProcess::addProvisionedResource);

        if (transferProcess.provisioningComplete()) {
            transferProcess.transitionProvisioned();
        }

        transferProcessStore.update(transferProcess);
    }

    void onDeprovisionComplete(String processId) {
        monitor.info("Deprovisioning successfully completed.");

        TransferProcess transferProcess = transferProcessStore.find(processId);
        if (transferProcess == null) {
            monitor.severe("TransferProcessManager: no TransferProcess found for provisioned resources");
            return;
        }

        if (transferProcess.getState() == ERROR.code()) {
            monitor.severe(format("TransferProcessManager: transfer process %s is in ERROR state, so deprovisioning could not be completed", transferProcess.getId()));
            return;
        }

        transferProcess.transitionDeprovisioned();
        transferProcessStore.update(transferProcess);
        observable.invokeForEach(l -> l.deprovisioned(transferProcess));
    }

    private void transitionToRequestAck(String processId) {
        TransferProcess transferProcess = transferProcessStore.find(processId);
        if (transferProcess == null) {
            monitor.severe("TransferProcessManager: no TransferProcess found for acked request");
            return;
        }

        transferProcess.transitionRequestAck();
        transferProcessStore.update(transferProcess);
    }

    private TransferInitiateResult initiateRequest(TransferProcess.Type type, DataRequest dataRequest) {
        // make the request idempotent: if the process exists, return
        var processId = transferProcessStore.processIdForTransferId(dataRequest.getId());
        if (processId != null) {
            return TransferInitiateResult.success(processId);
        }
        var id = randomUUID().toString();
        var process = TransferProcess.Builder.newInstance().id(id).dataRequest(dataRequest).type(type)
                .traceContext(telemetry.getCurrentTraceContext()).build();
        if (process.getState() == TransferProcessStates.UNSAVED.code()) {
            process.transitionInitial();
        }
        transferProcessStore.create(process);
        observable.invokeForEach(l -> l.created(process));
        return TransferInitiateResult.success(process.getId());
    }

    private void run() {
        while (active.get()) {
            try {
                long initial = processTransfersInState(INITIAL, this::processInitial);
                long provisioned = processTransfersInState(PROVISIONED, this::processProvisioned);
                long ackRequested = processTransfersInState(REQUESTED_ACK, this::processAckRequested);
                long inProgress = processTransfersInState(IN_PROGRESS, this::processInProgress);
                long deprovisioningRequest = processTransfersInState(DEPROVISIONING_REQ, this::processDeprovisioningRequest);
                long deprovisioned = processTransfersInState(DEPROVISIONED, this::processDeprovisioned);

                long commandsProcessed = onCommands().doProcess(this::processCommand);

                var totalProcessed = initial + provisioned + ackRequested + inProgress + deprovisioningRequest + deprovisioned + commandsProcessed;
                if (totalProcessed == 0) {
                    Thread.sleep(waitStrategy.waitForMillis());
                }
                waitStrategy.success();
            } catch (Error e) {
                throw e; // let the thread die and don't reschedule as the error is unrecoverable
            } catch (InterruptedException e) {
                Thread.interrupted();
                active.set(false);
                break;
            } catch (Throwable e) {
                monitor.severe("Error caught in transfer process manager", e);
                try {
                    Thread.sleep(waitStrategy.retryInMillis());
                } catch (InterruptedException e2) {
                    Thread.interrupted();
                    active.set(false);
                    break;
                }
            }
        }
    }

    private long processTransfersInState(TransferProcessStates state, Function<TransferProcess, Boolean> function) {
        var functionWithTraceContext = telemetry.contextPropagationMiddleware(function);
        return new EntitiesProcessor<>(() -> transferProcessStore.nextForState(state.code(), batchSize)).doProcess(functionWithTraceContext);
    }

    private EntitiesProcessor<TransferProcessCommand> onCommands() {
        return new EntitiesProcessor<>(() -> commandQueue.dequeue(5));
    }

    private boolean processCommand(TransferProcessCommand command) {
        return commandProcessor.processCommandQueue(command);
    }

    @WithSpan
    private boolean processDeprovisioned(TransferProcess process) {
        process.transitionEnded();
        transferProcessStore.update(process);
        observable.invokeForEach(l -> l.ended(process));
        monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
        return true;
    }

    @WithSpan
    private boolean processDeprovisioningRequest(TransferProcess process) {
        process.transitionDeprovisioning();
        transferProcessStore.update(process);
        observable.invokeForEach(l -> l.deprovisioning(process));
        monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
        provisionManager.deprovision(process)
                .whenComplete((responses, throwable) -> {
                    if (throwable == null) {
                        onDeprovisionComplete(process.getId());
                    } else {
                        transitionToError(process.getId(), throwable, "Error during deprovisioning");
                    }
                });

        return true;
    }

    @WithSpan
    private boolean processAckRequested(TransferProcess process) {
        if (!process.getDataRequest().isManagedResources() || (process.getProvisionedResourceSet() != null && !process.getProvisionedResourceSet().empty())) {
            process.transitionInProgressOrStreaming();
            transferProcessStore.update(process);
            observable.invokeForEach(l -> l.inProgress(process));
            monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
            return true;
        } else {
            monitor.debug("Process " + process.getId() + " does not yet have provisioned resources, will stay in " + TransferProcessStates.REQUESTED_ACK);
            return false;
        }
    }

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
                process.transitionInProgress();
                transferProcessStore.update(process);
                monitor.info(format("Transfer process %s not COMPLETED yet. The process will not advance to the COMPLETED state.", process.getId()));
                return false;
            }
        }
    }

    @WithSpan
    private void transitionToCompleted(TransferProcess process) {
        process.transitionCompleted();
        monitor.debug("Process " + process.getId() + " is now " + COMPLETED);
        transferProcessStore.update(process);
        observable.invokeForEach(listener -> listener.completed(process));
    }

    /**
     * Performs consumer-side or provider side provisioning for a service.
     * <br/>
     * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
     * map involve preprocessing data or other operations.
     */
    @WithSpan
    private boolean processInitial(TransferProcess process) {
        var manifest = manifestGenerator.generateResourceManifest(process);
        process.transitionProvisioning(manifest);
        transferProcessStore.update(process);
        observable.invokeForEach(l -> l.provisioning(process));

        provisionManager.provision(process)
                .whenComplete((responses, throwable) -> {
                    if (throwable == null) {
                        onProvisionComplete(process.getId(), responses);
                    } else {
                        transitionToError(process.getId(), throwable.getCause(), "Error during provisioning");
                    }
                });

        return true;
    }

    private void transitionToError(String id, Throwable throwable, String message) {
        var transferProcess = transferProcessStore.find(id);
        if (transferProcess == null) {
            monitor.severe(format("TransferProcessManager: no TransferProcess found with id %s", id));
            return;
        }

        monitor.severe(message, throwable);
        transferProcess.transitionError(format("%s: %s", message, throwable.getLocalizedMessage()));
        transferProcessStore.update(transferProcess);
    }

    private boolean processProvisioned(TransferProcess process) {
        DataRequest dataRequest = process.getDataRequest();
        if (CONSUMER == process.getType()) {
            sendConsumerRequest(process, dataRequest);
        } else {
            processProviderRequest(process, dataRequest);
        }
        return true;
    }

    @WithSpan
    private void processProviderRequest(TransferProcess process, DataRequest dataRequest) {
        var response = dataFlowManager.initiate(dataRequest);
        if (response.succeeded()) {
            process.transitionInProgressOrStreaming();
            transferProcessStore.update(process);
            observable.invokeForEach(l -> l.inProgress(process));
        } else {
            if (ResponseStatus.ERROR_RETRY == response.getFailure().status()) {
                monitor.severe("Error processing transfer request. Setting to retry: " + process.getId());
                process.transitionProvisioned();
                transferProcessStore.update(process);
                observable.invokeForEach(l -> l.provisioned(process));
            } else {
                monitor.severe(format("Fatal error processing transfer request: %s. Error details: %s", process.getId(), String.join(", ", response.getFailureMessages())));
                process.transitionError(response.getFailureMessages().stream().findFirst().orElse(""));
                transferProcessStore.update(process);
                observable.invokeForEach(l -> l.error(process));
            }
        }

    }

    @WithSpan
    private void sendConsumerRequest(TransferProcess process, DataRequest dataRequest) {
        process.transitionRequested();
        transferProcessStore.update(process);   // update before sending to accommodate synchronous transports; reliability will be managed by retry and idempotency
        observable.invokeForEach(l -> l.requested(process));
        dispatcherRegistry.send(Object.class, dataRequest, process::getId)
                .thenApply(o -> {
                    // TODO: what's the point of this state transition?
                    transitionToRequestAck(process.getId());
                    return o;
                })
                .whenComplete((o, throwable) -> {
                    if (o != null) {
                        monitor.info("Object received: " + o);
                        TransferProcess transferProcess = transferProcessStore.find(process.getId());
                        if (transferProcess == null) {
                            monitor.severe(format("TransferProcessManager: no TransferProcess found with id %s", process.getId()));
                            return;
                        }

                        transferProcess.transitionInProgressOrStreaming();
                        transferProcessStore.update(transferProcess);
                        observable.invokeForEach(l -> l.inProgress(transferProcess));
                    }
                });
    }

    public static class Builder {
        private final TransferProcessManagerImpl manager;

        private Builder() {
            manager = new TransferProcessManagerImpl();
            manager.telemetry = new Telemetry(); // default noop implementation
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

        public Builder store(TransferProcessStore transferProcessStore) {
            manager.transferProcessStore = transferProcessStore;
            return this;
        }

        public TransferProcessManagerImpl build() {
            Objects.requireNonNull(manager.manifestGenerator, "manifestGenerator");
            Objects.requireNonNull(manager.provisionManager, "provisionManager");
            Objects.requireNonNull(manager.dataFlowManager, "dataFlowManager");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.commandQueue, "commandQueue cannot be null");
            Objects.requireNonNull(manager.commandRunner, "commandRunner cannot be null");
            Objects.requireNonNull(manager.statusCheckerRegistry, "StatusCheckerRegistry cannot be null!");
            Objects.requireNonNull(manager.observable, "Observable cannot be null");
            Objects.requireNonNull(manager.telemetry, "Telemetry cannot be null");
            Objects.requireNonNull(manager.transferProcessStore, "Store cannot be null");
            manager.commandProcessor = new CommandProcessor<>(manager.commandQueue, manager.commandRunner, manager.monitor);

            return manager;
        }
    }

}
