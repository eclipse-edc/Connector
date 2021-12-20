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

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessListener;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Complete;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Initiate;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.InitiateDataFlow;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.RequireAck;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.RequireTransition;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.CompleteHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.InitiateDataFlowConsumerHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.InitiateDataFlowHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.InitiateHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.PrepareProvisionHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.ProvisionHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.RequireAckHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.RequireTransitionHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.TransferProcessCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.DEPROVISIONING_REQ;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.ENDED;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.INITIAL;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.PROVISIONED;

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
 * If no processes need to be transitioned, the transfer manager will wait according to the the defined {@link TransferWaitStrategy} before conducting the next iteration.
 * A wait strategy may implement a backoff scheme.
 */
public class AsyncTransferProcessManager extends TransferProcessObservable implements TransferProcessManager {
    private final AtomicBoolean active = new AtomicBoolean();

    private int batchSize = 5;
    private TransferWaitStrategy waitStrategy = () -> 5000L;  // default wait five seconds
    private ResourceManifestGenerator manifestGenerator;
    private ProvisionManager provisionManager;
    private TransferProcessStore transferProcessStore;
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    private DataFlowManager dataFlowManager;
    private Monitor monitor;
    private ExecutorService executor;
    private StatusCheckerRegistry statusCheckerRegistry;
    private Vault vault;
    private TypeManager typeManager;

    private final Queue<CommandRequest> commandQueue = new LinkedBlockingQueue<>();
    private final List<TransferProcessCommandHandler<? extends TransferProcessCommand>> commandHandlers = new ArrayList<>();

    private AsyncTransferProcessManager() {

    }

    public void start(TransferProcessStore processStore) {
        transferProcessStore = processStore;
        commandHandlers.add(new InitiateHandler(transferProcessStore));
        commandHandlers.add(new PrepareProvisionHandler(transferProcessStore, manifestGenerator));
        commandHandlers.add(new ProvisionHandler(transferProcessStore, provisionManager));
        commandHandlers.add(new RequireTransitionHandler(transferProcessStore, dispatcherRegistry));
        commandHandlers.add(new InitiateDataFlowHandler(transferProcessStore, dataFlowManager, monitor));
        commandHandlers.add(new CompleteHandler(transferProcessStore, statusCheckerRegistry, monitor));
        commandHandlers.add(new RequireAckHandler(transferProcessStore));
        commandHandlers.add(new InitiateDataFlowConsumerHandler(transferProcessStore, monitor));
        active.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::run);
    }

    // TODO: stop should wait some time to empty the command queue before shutdown the executor
    public void stop() {
        active.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public TransferInitiateResult initiateConsumerRequest(DataRequest dataRequest) {
        return initiateRequest(CONSUMER, dataRequest);
    }

    @Override
    public TransferInitiateResult initiateProviderRequest(DataRequest dataRequest) {
        return initiateRequest(PROVIDER, dataRequest);
    }

    @Override
    public CompletableFuture<Void> transitionRequestAck(String processId) {
        var future = new CompletableFuture<Void>();
        commandQueue.add(new CommandRequest(new RequireAck(processId), future));
        return future;
    }

    @Override
    public void transitionProvisioned(String processId) {
        TransferProcess transferProcess = transferProcessStore.find(processId);
        transferProcess.transitionProvisioned();
        transferProcessStore.update(transferProcess);
    }

    @Override
    public void transitionError(String processId, String detail) {
        TransferProcess transferProcess = transferProcessStore.find(processId);
        transferProcess.transitionError(detail);
        transferProcessStore.update(transferProcess);
    }

    @Override
    public Result<TransferProcessStates> deprovision(String processId) {
        var process = transferProcessStore.find(processId);
        if (process == null) {
            return Result.failure("not found");
        }

        if (Set.of(DEPROVISIONED.code(), DEPROVISIONING_REQ.code(), DEPROVISIONING.code(), ENDED.code()).contains(process.getState())) {
            monitor.info("Request already deprovisioning or deprovisioned.");
        } else {
            monitor.info("starting to deprovision data request " + processId);
            process.transitionCompleted();
            process.transitionDeprovisionRequested();
            transferProcessStore.update(process);
        }

        return Result.success(TransferProcessStates.from(process.getState()));
    }

    @Override
    public CompletableFuture<Void> requireTransition(String id) {
        return null;
    }

    @Override
    public CompletableFuture<Void> initiateDataFlow(String id) {
        return null;
    }

    void onDeprovisionComplete(ProvisionedDataDestinationResource resource, Throwable deprovisionError) {
        if (deprovisionError != null) {
            monitor.severe("Deprovisioning error: ", deprovisionError);
        } else {
            monitor.info("Deprovisioning successfully completed.");

            TransferProcess transferProcess = transferProcessStore.find(resource.getTransferProcessId());
            if (transferProcess != null) {
                transferProcess.transitionDeprovisioned();
                transferProcessStore.update(transferProcess);
                monitor.debug("Process " + transferProcess.getId() + " is now " + TransferProcessStates.from(transferProcess.getState()));
            } else {
                monitor.severe("ProvisionManager: no TransferProcess found for deprovisioned resource");
            }

        }
    }

    void onProvisionComplete(ProvisionedResource destinationResource, SecretToken secretToken) {
        var processId = destinationResource.getTransferProcessId();
        var transferProcess = transferProcessStore.find(processId);
        if (transferProcess == null) {
            monitor.severe(format("Error received when provisioning resource %s Process id not found for: %s",
                    destinationResource.getResourceDefinitionId(), destinationResource.getTransferProcessId()));
            return;
        }

        if (destinationResource instanceof ProvisionedDataDestinationResource) {
            var dataDestinationResource = (ProvisionedDataDestinationResource) destinationResource;
            if (!destinationResource.isError()) {
                transferProcess.getDataRequest().updateDestination(dataDestinationResource.createDataDestination());
            }

            if (secretToken != null) {
                String keyName = dataDestinationResource.getResourceName();
                vault.storeSecret(keyName, typeManager.writeValueAsString(secretToken));
                transferProcess.getDataRequest().getDataDestination().setKeyName(keyName);
            }
        }

        transferProcess.addProvisionedResource(destinationResource);

        if (destinationResource.isError()) {
            var processId1 = transferProcess.getId();
            var resourceId = destinationResource.getResourceDefinitionId();
            monitor.severe(format("Error provisioning resource %s for process %s: %s", resourceId, processId1, destinationResource.getErrorMessage()));
            transferProcessStore.update(transferProcess);
            return;
        }

        if (TransferProcessStates.ERROR.code() != transferProcess.getState() && transferProcess.provisioningComplete()) {
            // TODO If all resources provisioned, delete scratch data
            transferProcess.transitionProvisioned();
        }
        transferProcessStore.update(transferProcess);
    }

    public CompletableFuture<Void> complete(String id) {
        var future = new CompletableFuture<Void>();
        commandQueue.add(new CommandRequest(new Complete(id), future));
        return future;
    }

    private TransferInitiateResult initiateRequest(TransferProcess.Type type, DataRequest dataRequest) {
        var id = randomUUID().toString();
        commandQueue.add(new CommandRequest(new Initiate(id, type, dataRequest), new CompletableFuture<>()));
        return TransferInitiateResult.success(id);
    }

    private void run() {
        while (active.get()) {
            try {
                var commandRequest = commandQueue.poll();

                if (commandRequest != null) {
                    var optionalHandler = commandHandlers.stream()
                            .filter(it -> it.handles().equals(commandRequest.getCommand().getClass()))
                            .findFirst();

                    if (optionalHandler.isPresent()) {
                        TransferProcessCommandHandler handler = optionalHandler.get();
                        var result = handler.handle(commandRequest.getCommand());

                        var nextCommand = result.getNextCommand();
                        if (nextCommand != null) {
                            commandQueue.add(new CommandRequest(nextCommand, commandRequest.getFuture()));
                        } else {
                            commandRequest.getFuture().complete(null);
                        }
                        getListeners().forEach(listener -> result.getPostAction().apply(listener));

                    } else {
                        monitor.severe(String.format("Transfer Command type %s is not handled", commandRequest.getClass().getName()));
                    }
                } else {
//                    Thread.sleep(waitStrategy.waitForMillis());
                    Thread.sleep(10); // TODO: resolve this
                }

                // TODO check processes in provisioning state and timestamps for failed processes

                int deprovisioning = checkDeprovisioningRequested();

                int deprovisioned = checkDeprovisioned();

//                if (provisioned + finished + deprovisioning + deprovisioned == 0) {
//                    Thread.sleep(waitStrategy.waitForMillis());
//                }
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

    private int checkDeprovisioned() {
        var deprovisionedProcesses = transferProcessStore.nextForState(DEPROVISIONED.code(), batchSize);

        for (var process : deprovisionedProcesses) {
            invokeForEach(l -> l.deprovisioned(process));
            process.transitionEnded();
            transferProcessStore.update(process);
            invokeForEach(l -> l.ended(process));
            monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
        }
        return deprovisionedProcesses.size();
    }

    /**
     * Transitions all processes that are in state DEPROVISIONING_REQ and deprovisions their associated
     * resources. Then they are moved to DEPROVISIONING
     *
     * @return the number of transfer processes in DEPROVISIONING_REQ
     */
    private int checkDeprovisioningRequested() {
        List<TransferProcess> processesDeprovisioning = transferProcessStore.nextForState(DEPROVISIONING_REQ.code(), batchSize);

        for (var process : processesDeprovisioning) {
            process.transitionDeprovisioning();
            transferProcessStore.update(process);
            invokeForEach(l -> l.deprovisioning(process));
            monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
            provisionManager.deprovision(process)
                    .forEach(future -> {
                        future.whenComplete((response, throwable) -> {
                            if (response != null) {
                                onDeprovisionComplete(response.getResource(), null);
                            } else {
                                monitor.severe("Error during deprovisioning", throwable);
                                process.transitionError("Error during deprovisioning: " + throwable.getLocalizedMessage());
                                transferProcessStore.update(process);
                            }
                        });
                    });
        }

        return processesDeprovisioning.size();
    }

    /**
     * Transition all processes, who have provisioned resources, into the IN_PROGRESS or STREAMING status, depending on
     * whether they're finite or not.
     * If a process does not have provisioned resources, it will remain in REQUESTED_ACK.
     */
    private int checkProvisioned() {
        var requestAcked = transferProcessStore.nextForState(TransferProcessStates.REQUESTED_ACK.code(), batchSize);

        for (var process : requestAcked) {
            // process must either have a non-empty list of provisioned resources, or not have managed resources at all.
            if (!process.getDataRequest().isManagedResources() || (process.getProvisionedResourceSet() != null && !process.getProvisionedResourceSet().empty())) {

                if (process.getDataRequest().getTransferType().isFinite()) {
                    process.transitionInProgress();
                } else {
                    process.transitionStreaming();
                }
                transferProcessStore.update(process);
                invokeForEach(l -> l.inProgress(process));
                monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.from(process.getState()));
            } else {
                monitor.debug("Process " + process.getId() + " does not yet have provisioned resources, will stay in " + TransferProcessStates.REQUESTED_ACK);
            }
        }

        return requestAcked.size();

    }

    /**
     * Checks all provisioned resources that are assigned to a transfer process for completion. If no StatusChecker exists
     * for a particular ProvisionedResource, it is automatically assumed to be complete.
     */
    private int checkCompleted() {

        //deal with all the consumer processes
        var processesInProgress = transferProcessStore.nextForState(TransferProcessStates.IN_PROGRESS.code(), batchSize);

        for (var process : processesInProgress.stream().filter(p -> p.getType() == CONSUMER).collect(toList())) {
            if (process.getDataRequest().isManagedResources()) {
                var resources = process.getProvisionedResourceSet().getResources();
                var checker = statusCheckerRegistry.resolve(process.getDataRequest().getDestinationType());
                if (checker == null) {
                    monitor.info(format("No checker found for process %s. The process will not advance to the COMPLETED state.", process.getId()));
                } else if (checker.isComplete(process, resources)) {
                    // checker passed, transition the process to the COMPLETED state
                    transitionToCompleted(process);
                }
            } else {
                var checker = statusCheckerRegistry.resolve(process.getDataRequest().getDestinationType());
                if (checker != null) {
                    if (checker.isComplete(process, emptyList())) {
                        //checker passed, transition the process to the COMPLETED state automatically
                        transitionToCompleted(process);
                    }
                } else {
                    //no checker, transition the process to the COMPLETED state automatically
                    transitionToCompleted(process);
                }
            }
        }
        return processesInProgress.size();
    }

    private void transitionToCompleted(TransferProcess process) {
        process.transitionCompleted();
        monitor.debug("Process " + process.getId() + " is now " + TransferProcessStates.COMPLETED);
        transferProcessStore.update(process);
        invokeForEach(listener -> listener.completed(process));
    }

    /**
     * Performs consumer-side or provider side provisioning for a service.
     * <br/>
     * On a consumer, provisioning may entail setting up a data destination and supporting infrastructure. On a provider, provisioning is initiated when a request is received and
     * map involve preprocessing data or other operations.
     */
    private int provisionInitialProcesses() {
        var processes = transferProcessStore.nextForState(INITIAL.code(), batchSize);
        for (TransferProcess process : processes) {
            DataRequest dataRequest = process.getDataRequest();
            ResourceManifest manifest;
            if (process.getType() == CONSUMER) {
                // if resources are managed by this connector, generate the manifest; otherwise create an empty one
                manifest = dataRequest.isManagedResources() ? manifestGenerator.generateConsumerManifest(process) : ResourceManifest.Builder.newInstance().build();
            } else {
                manifest = manifestGenerator.generateProviderManifest(process);
            }
            process.transitionProvisioning(manifest);
            transferProcessStore.update(process);
            invokeForEach(l -> l.provisioning(process));
            if (process.getResourceManifest().getDefinitions().isEmpty()) {
                // no resources to provision, advance state
                process.transitionProvisioned();
                transferProcessStore.update(process);
            } else {
                provisionManager.provision(process).forEach(future -> {
                    future.whenComplete((result, throwable) -> {
                        if (result != null) {
                            onProvisionComplete(result.getResource(), result.getSecretToken());
                        } else {
                            monitor.severe("Error during provisioning", throwable);
                            process.transitionError("Error during provisioning: " + throwable.getLocalizedMessage());
                            transferProcessStore.update(process);
                        }
                    });
                });
            }

        }
        return processes.size();
    }

    /**
     * On a consumer, sends provisioned requests to the provider connector. On the provider, sends provisioned requests to the data flow manager.
     *
     * @return the number of requests processed
     */
    private int sendOrProcessProvisionedRequests() {
        var processes = transferProcessStore.nextForState(PROVISIONED.code(), batchSize);
        for (TransferProcess process : processes) {
            DataRequest dataRequest = process.getDataRequest();
            if (CONSUMER == process.getType()) {
                process.transitionRequested();
                transferProcessStore.update(process);   // update before sending to accommodate synchronous transports; reliability will be managed by retry and idempotency
                invokeForEach(l -> l.requested(process));
                dispatcherRegistry.send(Object.class, dataRequest, process::getId)
                        .thenApply(o -> {
                            transitionRequestAck(process.getId());
                            transferProcessStore.update(process);
                            return o;
                        })
                        .whenComplete((o, throwable) -> {
                            if (o != null) {
                                monitor.info("Object received: " + o);
                                if (dataRequest.getTransferType().isFinite()) {
                                    process.transitionInProgress();
                                } else {
                                    process.transitionStreaming();
                                }
                                transferProcessStore.update(process);
                            }
                        });
            } else {
                var response = dataFlowManager.initiate(dataRequest);
                if (response.succeeded()) {
                    if (process.getDataRequest().getTransferType().isFinite()) {
                        process.transitionInProgress();
                    } else {
                        process.transitionStreaming();
                    }
                    transferProcessStore.update(process);
                    invokeForEach(l -> l.inProgress(process));
                } else {
                    if (ResponseStatus.ERROR_RETRY == response.getFailure().status()) {
                        monitor.severe("Error processing transfer request. Setting to retry: " + process.getId());
                        process.transitionProvisioned();
                        transferProcessStore.update(process);
                        invokeForEach(l -> l.provisioned(process));
                    } else { //ERROR_FATAL
                        monitor.severe(format("Fatal error processing transfer request: %s. Error details: %s", process.getId(), String.join(", ", response.getFailureMessages())));
                        process.transitionError(response.getFailureMessages().stream().findFirst().orElse(""));
                        transferProcessStore.update(process);
                        invokeForEach(l -> l.error(process));
                    }
                }
            }
        }
        return processes.size();
    }

    private void invokeForEach(Consumer<TransferProcessListener> action) {
        getListeners().forEach(action);
    }

    public static class Builder {
        private final AsyncTransferProcessManager manager;

        private Builder() {
            manager = new AsyncTransferProcessManager();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder batchSize(int size) {
            manager.batchSize = size;
            return this;
        }

        public Builder waitStrategy(TransferWaitStrategy waitStrategy) {
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

        public Builder vault(Vault vault) {
            manager.vault = vault;
            return this;
        }

        public Builder typeManager(TypeManager typeManager) {
            manager.typeManager = typeManager;
            return this;
        }

        public AsyncTransferProcessManager build() {
            Objects.requireNonNull(manager.manifestGenerator, "manifestGenerator");
            Objects.requireNonNull(manager.provisionManager, "provisionManager");
            Objects.requireNonNull(manager.dataFlowManager, "dataFlowManager");
            Objects.requireNonNull(manager.dispatcherRegistry, "dispatcherRegistry");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.statusCheckerRegistry, "StatusCheckerRegistry cannot be null!");
            return manager;
        }
    }

    private static class CommandRequest {

        private final TransferProcessCommand command;
        private final CompletableFuture<?> future;

        public CommandRequest(TransferProcessCommand command, CompletableFuture<?> future) {
            this.command = command;
            this.future = future;
        }

        public TransferProcessCommand getCommand() {
            return command;
        }

        public CompletableFuture<?> getFuture() {
            return future;
        }
    }
}
