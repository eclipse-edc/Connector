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
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.Initiate;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.PrepareDeprovision;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.RequireAck;
import org.eclipse.dataspaceconnector.transfer.core.transfer.command.TransferProcessCommand;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.CompleteHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.DeprovisionHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.EndHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.InitiateDataFlowConsumerHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.InitiateDataFlowHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.InitiateHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.PrepareDeprovisionHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.PrepareProvisionHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.ProvisionHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.RequireAckHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.RequireTransitionHandler;
import org.eclipse.dataspaceconnector.transfer.core.transfer.commandhandler.TransferProcessCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.CONSUMER;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess.Type.PROVIDER;

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
        commandHandlers.add(new ProvisionHandler(transferProcessStore, provisionManager, monitor, vault, typeManager, commandQueue)); // TODO: command queue?
        commandHandlers.add(new RequireTransitionHandler(transferProcessStore, dispatcherRegistry));
        commandHandlers.add(new InitiateDataFlowHandler(transferProcessStore, dataFlowManager, monitor));
        commandHandlers.add(new CompleteHandler(transferProcessStore, statusCheckerRegistry, monitor));
        commandHandlers.add(new RequireAckHandler(transferProcessStore));
        commandHandlers.add(new InitiateDataFlowConsumerHandler(transferProcessStore, monitor));
        commandHandlers.add(new PrepareDeprovisionHandler(transferProcessStore));
        commandHandlers.add(new DeprovisionHandler(transferProcessStore, provisionManager, monitor));
        commandHandlers.add(new EndHandler(transferProcessStore));
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
        var id = randomUUID().toString();
        commandQueue.add(new CommandRequest(new Initiate(id, CONSUMER, dataRequest), new CompletableFuture<>()));
        return TransferInitiateResult.success(id);
    }

    @Override
    public TransferInitiateResult initiateProviderRequest(DataRequest dataRequest) {
        var id = randomUUID().toString();
        commandQueue.add(new CommandRequest(new Initiate(id, PROVIDER, dataRequest), new CompletableFuture<>()));
        return TransferInitiateResult.success(id);
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
        var future = new CompletableFuture<>();
        commandQueue.add(new CommandRequest(new PrepareDeprovision(processId), future));
        future.join();

        var process = transferProcessStore.find(processId);
        return Result.success(TransferProcessStates.from(process.getState()));
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

    public static class Builder {
        private final AsyncTransferProcessManager manager;

        private Builder() {
            manager = new AsyncTransferProcessManager();
        }

        public static Builder newInstance() {
            return new Builder();
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

    public static class CommandRequest {

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
