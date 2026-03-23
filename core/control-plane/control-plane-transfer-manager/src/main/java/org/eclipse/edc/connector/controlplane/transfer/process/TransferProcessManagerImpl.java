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

import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessPendingGuard;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessors;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.retry.WaitStrategy;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTUP_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING_REQUESTED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;
import static org.eclipse.edc.spi.persistence.StateEntityStore.isNotPending;

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
    private TransferProcessPendingGuard pendingGuard = tp -> false;
    private TransferProcessors transferProcessors;

    private TransferProcessManagerImpl() {
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processConsumerTransfersInState(INITIAL, transferProcessors::processConsumerInitial))
                .processor(processProviderTransfersInState(INITIAL, transferProcessors::processProviderInitial))
                .processor(processConsumerTransfersInState(REQUESTING, transferProcessors::processRequesting))
                .processor(processProviderTransfersInState(STARTING, transferProcessors::processStarting))
                .processor(processConsumerTransfersInState(STARTUP_REQUESTED, transferProcessors::processStartupRequested))
                .processor(processTransfersInState(SUSPENDING, transferProcessors::processSuspending))
                .processor(processTransfersInState(SUSPENDING_REQUESTED, transferProcessors::processSuspending))
                .processor(processProviderTransfersInState(RESUMING, transferProcessors::processProviderResuming))
                .processor(processConsumerTransfersInState(RESUMING, transferProcessors::processConsumerResuming))
                .processor(processTransfersInState(COMPLETING, transferProcessors::processCompleting))
                .processor(processTransfersInState(COMPLETING_REQUESTED, transferProcessors::processCompleting))
                .processor(processTransfersInState(TERMINATING, transferProcessors::processTerminating))
                .processor(processTransfersInState(TERMINATING_REQUESTED, transferProcessors::processTerminating));
    }

    private Processor processConsumerTransfersInState(TransferProcessStates state, Function<TransferProcess, CompletableFuture<StatusResult<Void>>> function) {
        var filter = new Criterion[]{hasState(state.code()), isNotPending(), Criterion.criterion("type", "=", CONSUMER.name())};
        return createProcessor(function, filter);
    }

    private Processor processProviderTransfersInState(TransferProcessStates state, Function<TransferProcess, CompletableFuture<StatusResult<Void>>> function) {
        var filter = new Criterion[]{hasState(state.code()), isNotPending(), Criterion.criterion("type", "=", PROVIDER.name())};
        return createProcessor(function, filter);
    }

    private Processor processTransfersInState(TransferProcessStates state, Function<TransferProcess, CompletableFuture<StatusResult<Void>>> function) {
        var filter = new Criterion[]{hasState(state.code()), isNotPending()};
        return createProcessor(function, filter);
    }

    private ProcessorImpl<TransferProcess> createProcessor(Function<TransferProcess, CompletableFuture<StatusResult<Void>>> function, Criterion[] filter) {
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter), entityRetryProcessConfiguration, clock, monitor)
                .process(telemetry.contextPropagationMiddleware(function))
                .guard(pendingGuard, this::setPending)
                .onNotProcessed(this::breakLease)
                .build();
    }

    private CompletableFuture<StatusResult<Void>> setPending(TransferProcess transferProcess) {
        transferProcess.setPending(true);
        update(transferProcess);
        return CompletableFuture.completedFuture(StatusResult.success());
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
            Objects.requireNonNull(manager.transferProcessors, "transferProcessors cannot be null");

            return manager;
        }

        public Builder pendingGuard(TransferProcessPendingGuard pendingGuard) {
            manager.pendingGuard = pendingGuard;
            return this;
        }

        public Builder transferProcessors(TransferProcessors transferProcessors) {
            manager.transferProcessors = transferProcessors;
            return this;
        }
    }

}
