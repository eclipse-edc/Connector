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

package org.eclipse.edc.controlplane.transfer.process.tasks.executor;

import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessors;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.controlplane.transfer.spi.TransferProcessTaskExecutor;
import org.eclipse.edc.controlplane.transfer.spi.tasks.CompleteDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.PrepareTransfer;
import org.eclipse.edc.controlplane.transfer.spi.tasks.ResumeDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SendTransferRequest;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SendTransferStart;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SignalDataflowStarted;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SuspendDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TerminateDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TransferProcessTaskPayload;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.from;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

public class TransferProcessTaskExecutorImpl implements TransferProcessTaskExecutor {

    private final Map<Class<? extends TransferProcessTaskPayload>, Handler> handlers = new HashMap<>();
    private TransferProcessStore store;
    private TaskService taskService;
    private TransactionContext transactionContext;
    private TransferProcessPendingGuard pendingGuard;
    private TransferProcessors transferProcessors;
    private Monitor monitor;
    private Clock clock = Clock.systemUTC();

    private TransferProcessTaskExecutorImpl() {
        registerStateHandlers();
    }

    private void registerStateHandlers() {
        handlers.put(PrepareTransfer.class, new Handler(this::handlePrepareTransfer, null));
        handlers.put(SendTransferRequest.class, new Handler(this::handleSendRequest, CONSUMER));
        handlers.put(SendTransferStart.class, new Handler(this::handleSendStartMessage, PROVIDER));
        handlers.put(SignalDataflowStarted.class, new Handler(this::handleSignalStartedDataflow, CONSUMER));
        handlers.put(SuspendDataFlow.class, new Handler(this::handleSuspendDataflow, null));
        handlers.put(ResumeDataFlow.class, new Handler(this::handleResumeDataflow, null));
        handlers.put(TerminateDataFlow.class, new Handler(this::handleTerminateDataflow, null));
        handlers.put(CompleteDataFlow.class, new Handler(this::handleCompleteDataflow, null));
    }

    @Override
    public StatusResult<Void> handle(TransferProcessTaskPayload task) {
        return handleTask(task);
    }

    private void storeTask(TransferProcessTaskPayload payload) {

        var task = Task.Builder.newInstance().at(clock.millis())
                .payload(payload)
                .build();
        taskService.create(task);
    }

    private StatusResult<Void> handleTask(TransferProcessTaskPayload task) {
        var expectedState = TransferProcessStates.from(task.getProcessState());
        var transferId = task.getProcessId();
        return transactionContext.execute(() -> {
            var transferResult = loadTransferProcess(transferId);
            if (transferResult.failed()) {
                return StatusResult.failure(FATAL_ERROR, transferResult.getFailureDetail());
            }

            var transferProcess = transferResult.getContent();
            if (TransferProcessStates.isFinal(transferProcess.getState())) {
                monitor.debug("Skipping transfer process with id '%s' is in final state '%s'".formatted(transferId, from(transferProcess.getState())));
                return StatusResult.success();
            }

            if (transferProcess.getState() != expectedState.code()) {
                monitor.warning("Skipping transfer process with id '%s' is in state '%s', expected '%s'".formatted(transferId, from(transferProcess.getState()), expectedState));
                return StatusResult.success();
            }

            var handler = handlers.get(task.getClass());
            if (handler == null) {
                monitor.debug("No handler for task '%s' in transfer process with id '%s'".formatted(task.getClass().getSimpleName(), transferId));
                return StatusResult.success();
            }

            if (handler.type != null && handler.type != transferProcess.getType()) {
                var msg = "Expected type '%s' for state '%s', but got '%s' for transfer process %s".formatted(handler.type, expectedState, transferProcess.getType(), transferId);
                monitor.severe(msg);
                return StatusResult.failure(FATAL_ERROR, msg);
            }

            if (transferProcess.isPending()) {
                monitor.debug("Skipping transfer process with id '%s' is in pending state".formatted(transferId));
                return StatusResult.success();
            }

            if (pendingGuard.test(transferProcess)) {
                monitor.debug("Skipping '%s' for transfer process with id '%s' due matched guard".formatted(expectedState, transferId));
                return StatusResult.success();
            }

            return handler.function.apply(transferProcess);
        });

    }

    private StatusResult<Void> handlePrepareTransfer(TransferProcess process) {
        if (process.getType() == CONSUMER) {
            return invokeProcessor(process, transferProcessors::processConsumerInitial).onSuccess(v -> {
                if (process.getState() == REQUESTING.code()) {
                    var task = baseBuilder(SendTransferRequest.Builder.newInstance(), process).build();
                    storeTask(task);
                }
            });
        } else {
            return invokeProcessor(process, transferProcessors::processProviderInitial).onSuccess(v -> {
                if (process.getState() == STARTING.code()) {
                    var task = baseBuilder(SendTransferStart.Builder.newInstance(), process).build();
                    storeTask(task);
                }
            });
        }
    }

    private StatusResult<Void> handleSendStartMessage(TransferProcess process) {
        return invokeProcessor(process, transferProcessors::processStarting);
    }

    private StatusResult<Void> handleSignalStartedDataflow(TransferProcess process) {
        return invokeProcessor(process, transferProcessors::processStartupRequested);
    }

    private StatusResult<Void> handleSuspendDataflow(TransferProcess process) {
        return invokeProcessor(process, transferProcessors::processSuspending);
    }

    private StatusResult<Void> handleResumeDataflow(TransferProcess process) {
        if (process.getType() == CONSUMER) {
            return invokeProcessor(process, transferProcessors::processConsumerResuming);
        } else {
            return invokeProcessor(process, transferProcessors::processProviderResuming);
        }
    }

    private StatusResult<Void> handleTerminateDataflow(TransferProcess process) {
        return invokeProcessor(process, transferProcessors::processTerminating);
    }

    private StatusResult<Void> handleCompleteDataflow(TransferProcess process) {
        return invokeProcessor(process, transferProcessors::processCompleting);
    }

    private StatusResult<Void> handleSendRequest(TransferProcess process) {
        return invokeProcessor(process, transferProcessors::processRequesting);
    }

    private StatusResult<TransferProcess> loadTransferProcess(String transferProcessId) {
        var transferProcess = store.findById(transferProcessId);
        if (transferProcess == null) {
            return StatusResult.failure(FATAL_ERROR, "Transfer process with id '%s' not found".formatted(transferProcessId));
        }
        return StatusResult.success(transferProcess);
    }

    private StatusResult<Void> invokeProcessor(TransferProcess transferProcess, Function<TransferProcess, CompletableFuture<StatusResult<Void>>> processor) {
        try {
            return processor.apply(transferProcess).get();
        } catch (Exception e) {
            return StatusResult.failure(FATAL_ERROR, "Failed to invoke processor: %s".formatted(e.getMessage()));
        }
    }

    protected <T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> B baseBuilder(B builder, TransferProcess transferProcess) {
        return builder.processId(transferProcess.getId())
                .processState(transferProcess.getState())
                .processType(transferProcess.getType().name());
    }

    private record Handler(Function<TransferProcess, StatusResult<Void>> function, TransferProcess.Type type) {

    }

    public static class Builder {

        private final TransferProcessTaskExecutorImpl manager;

        private Builder() {
            manager = new TransferProcessTaskExecutorImpl();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public TransferProcessTaskExecutor build() {
            Objects.requireNonNull(manager.transferProcessors, "transferProcessors cannot be null");
            Objects.requireNonNull(manager.store, "store");
            Objects.requireNonNull(manager.taskService, "taskService");
            Objects.requireNonNull(manager.monitor, "monitor");
            Objects.requireNonNull(manager.transactionContext, "transactionContext cannot be null");
            return manager;
        }

        public Builder store(TransferProcessStore store) {
            manager.store = store;
            return this;
        }

        public Builder taskService(TaskService taskService) {
            manager.taskService = taskService;
            return this;
        }

        public Builder transferProcessors(TransferProcessors transferProcessors) {
            manager.transferProcessors = transferProcessors;
            return this;
        }

        public Builder transactionContext(TransactionContext transactionContext) {
            manager.transactionContext = transactionContext;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            manager.monitor = monitor;
            return this;
        }

        public Builder pendingGuard(TransferProcessPendingGuard pendingGuard) {
            manager.pendingGuard = pendingGuard;
            return this;
        }

        public Builder clock(Clock clock) {
            manager.clock = clock;
            return this;
        }
    }
}
