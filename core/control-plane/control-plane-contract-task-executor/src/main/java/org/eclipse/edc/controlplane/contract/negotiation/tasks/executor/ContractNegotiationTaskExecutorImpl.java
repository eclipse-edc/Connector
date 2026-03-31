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

package org.eclipse.edc.controlplane.contract.negotiation.tasks.executor;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ContractNegotiationPendingGuard;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.NegotiationProcessors;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.controlplane.contract.spi.negotiation.ContractNegotiationTaskExecutor;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.AgreeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.ContractNegotiationTaskPayload;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.FinalizeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.RequestNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendAccept;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendAgreement;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendFinalizeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendOffer;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendRequestNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendTerminateNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendVerificationNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.VerifyNegotiation;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.from;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

public class ContractNegotiationTaskExecutorImpl implements ContractNegotiationTaskExecutor {

    private final Map<Class<? extends ContractNegotiationTaskPayload>, Handler> handlers = new HashMap<>();
    protected Clock clock;
    protected ContractNegotiationStore store;
    protected TransactionContext transactionContext;
    protected ContractNegotiationPendingGuard pendingGuard;
    protected Monitor monitor;
    protected TaskService taskService;
    protected NegotiationProcessors negotiationProcessors;


    private ContractNegotiationTaskExecutorImpl() {
        registerStateHandlers();
    }

    private void registerStateHandlers() {
        handlers.put(RequestNegotiation.class, new Handler(this::handleRequest, ContractNegotiation.Type.CONSUMER));
        handlers.put(SendRequestNegotiation.class, new Handler(this::handleSendRequest, ContractNegotiation.Type.CONSUMER));
        handlers.put(AgreeNegotiation.class, new Handler(this::handleAgree, ContractNegotiation.Type.PROVIDER));
        handlers.put(SendAgreement.class, new Handler(this::handleSendAgreement, ContractNegotiation.Type.PROVIDER));
        handlers.put(SendAccept.class, new Handler(this::handleSendAccept, ContractNegotiation.Type.CONSUMER));
        handlers.put(SendOffer.class, new Handler(this::handleSendOffer, ContractNegotiation.Type.PROVIDER));
        handlers.put(SendTerminateNegotiation.class, new Handler(this::handleSendTermination, null));
        handlers.put(VerifyNegotiation.class, new Handler(this::handleVerify, ContractNegotiation.Type.CONSUMER));
        handlers.put(SendVerificationNegotiation.class, new Handler(this::handleSendVerification, ContractNegotiation.Type.CONSUMER));
        handlers.put(FinalizeNegotiation.class, new Handler(this::handleFinalize, ContractNegotiation.Type.PROVIDER));
        handlers.put(SendFinalizeNegotiation.class, new Handler(this::handleSendFinalize, ContractNegotiation.Type.PROVIDER));
    }

    @Override
    public StatusResult<Void> handle(ContractNegotiationTaskPayload task) {
        return handleTask(task);
    }

    private void storeTask(ContractNegotiationTaskPayload payload) {
        var task = Task.Builder.newInstance().at(clock.millis())
                .payload(payload)
                .build();
        taskService.create(task);
    }

    private StatusResult<Void> handleTask(ContractNegotiationTaskPayload task) {
        var expectedState = ContractNegotiationStates.from(task.getProcessState());
        var negotiationId = task.getProcessId();
        return transactionContext.execute(() -> {
            var negotiationResult = loadNegotiation(task.getProcessId());
            if (negotiationResult.failed()) {
                return StatusResult.failure(FATAL_ERROR, negotiationResult.getFailureDetail());
            }

            var negotiation = negotiationResult.getContent();
            if (ContractNegotiationStates.isFinal(negotiation.getState())) {
                monitor.debug("Skipping contract negotiation with id '%s' is in final state '%s'".formatted(task.getProcessId(), from(negotiation.getState())));
                return StatusResult.success();
            }

            if (negotiation.getState() != expectedState.code()) {
                monitor.warning("Skipping contract negotiation with id '%s' is in state '%s', expected '%s'".formatted(negotiationId, from(negotiation.getState()), expectedState));
                return StatusResult.success();
            }

            var handler = handlers.get(task.getClass());
            if (handler == null) {
                monitor.debug("No handler for task '%s' in contract negotiation with id '%s'".formatted(task.getClass().getSimpleName(), negotiationId));
                return StatusResult.success();
            }

            if (handler.type != null && handler.type != negotiation.getType()) {
                monitor.debug("Skipping '%s' for contract negotiation with id '%s' due to type mismatch: expected '%s', got '%s'".formatted(expectedState, negotiationId, handler.type, negotiation.getType()));
                return StatusResult.success();
            }

            if (pendingGuard.test(negotiation)) {
                monitor.debug("Skipping '%s' for contract negotiation with id '%s' due matched guard".formatted(expectedState, negotiationId));
                return StatusResult.success();
            }
            return handler.function.apply(negotiation);
        });

    }

    protected StatusResult<Void> handleSendAccept(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processAccepting);
    }

    private StatusResult<Void> handleSendOffer(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processOffering);
    }

    protected StatusResult<Void> handleSendTermination(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processTerminating);
    }

    private StatusResult<Void> handleSendRequest(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processRequesting);
    }

    private StatusResult<Void> handleFinalize(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processVerified).onSuccess(v -> {
            var task = baseBuilder(SendFinalizeNegotiation.Builder.newInstance(), negotiation)
                    .build();
            storeTask(task);
        });
    }

    protected StatusResult<Void> handleSendVerification(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processVerifying);
    }

    private StatusResult<Void> handleAgree(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processRequested).onSuccess(v -> {
            var task = baseBuilder(SendAgreement.Builder.newInstance(), negotiation)
                    .build();
            storeTask(task);
        });
    }

    protected StatusResult<Void> handleSendAgreement(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processAgreeing);
    }

    private StatusResult<Void> handleVerify(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processAgreed).onSuccess(v -> {
            var task = baseBuilder(SendVerificationNegotiation.Builder.newInstance(), negotiation)
                    .build();
            storeTask(task);
        });
    }

    private StatusResult<Void> handleRequest(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processInitial).onSuccess(v -> {
            var task = baseBuilder(SendRequestNegotiation.Builder.newInstance(), negotiation)
                    .build();
            storeTask(task);
        });
    }

    private StatusResult<Void> handleSendFinalize(ContractNegotiation negotiation) {
        return invokeProcessor(negotiation, negotiationProcessors::processFinalizing);
    }

    private StatusResult<Void> invokeProcessor(ContractNegotiation negotiation, Function<ContractNegotiation, CompletableFuture<StatusResult<Void>>> processor) {
        try {
            return processor.apply(negotiation).get();
        } catch (Exception e) {
            return StatusResult.failure(FATAL_ERROR, "Failed to invoke processor: %s".formatted(e.getMessage()));
        }
    }

    private StatusResult<ContractNegotiation> loadNegotiation(String negotiationId) {
        var negotiation = store.findById(negotiationId);
        if (negotiation == null) {
            return StatusResult.failure(FATAL_ERROR, "Contract negotiation with id '%s' not found".formatted(negotiationId));
        }
        return StatusResult.success(negotiation);
    }

    protected <T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> B baseBuilder(B builder, ContractNegotiation negotiation) {
        return builder.processId(negotiation.getId())
                .processState(negotiation.getState())
                .processType(negotiation.getType().name());
    }

    private record Handler(Function<ContractNegotiation, StatusResult<Void>> function, ContractNegotiation.Type type) {

    }

    public static class Builder {
        private final ContractNegotiationTaskExecutorImpl manager;

        private Builder() {
            manager = new ContractNegotiationTaskExecutorImpl();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder taskService(TaskService taskStore) {
            this.manager.taskService = taskStore;
            return this;
        }

        public Builder clock(Clock clock) {
            this.manager.clock = clock;
            return this;
        }

        public Builder store(ContractNegotiationStore store) {
            this.manager.store = store;
            return this;
        }

        public Builder transactionContext(TransactionContext transactionContext) {
            this.manager.transactionContext = transactionContext;
            return this;
        }

        public Builder pendingGuard(ContractNegotiationPendingGuard pendingGuard) {
            this.manager.pendingGuard = pendingGuard;
            return this;
        }

        public Builder negotiationProcessors(NegotiationProcessors negotiationProcessors) {
            this.manager.negotiationProcessors = negotiationProcessors;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.manager.monitor = monitor;
            return this;
        }

        public ContractNegotiationTaskExecutor build() {
            requireNonNull(manager.negotiationProcessors, "negotiationProcessors must not be null");
            requireNonNull(manager.taskService, "taskStore must not be null");
            requireNonNull(manager.clock, "clock must not be null");
            requireNonNull(manager.store, "store must not be null");
            requireNonNull(manager.transactionContext, "transactionContext must not be null");
            requireNonNull(manager.pendingGuard, "pendingGuard must not be null");
            requireNonNull(manager.monitor, "monitor must not be null");

            return manager;
        }
    }


}
