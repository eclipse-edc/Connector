/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.policy.monitor.manager;

import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorManager;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates.STARTED;
import static org.eclipse.edc.spi.persistence.StateEntityStore.hasState;

/**
 * Implementation of the {@link PolicyMonitorManager}
 */
public class PolicyMonitorManagerImpl extends AbstractStateEntityManager<PolicyMonitorEntry, PolicyMonitorStore>
        implements PolicyMonitorManager {

    private PolicyEngine policyEngine;
    private TransferProcessService transferProcessService;
    private ContractAgreementService contractAgreementService;

    private PolicyMonitorManagerImpl() {

    }

    @Override
    public void startMonitoring(String transferProcessId, String contractId) {
        var entry = PolicyMonitorEntry.Builder.newInstance()
                .id(transferProcessId)
                .contractId(contractId)
                .traceContext(telemetry.getCurrentTraceContext())
                .build();

        entry.transitionToStarted();

        update(entry);
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processEntriesInState(STARTED, this::processMonitoring));
    }

    private CompletableFuture<StatusResult<Void>> processMonitoring(PolicyMonitorEntry entry) {
        var transferProcess = transferProcessService.findById(entry.getId());
        if (transferProcess == null) {
            var message = "TransferProcess %s does not exist".formatted(entry.getId());
            entry.transitionToFailed(message);
            update(entry);
            return CompletableFuture.completedFuture(StatusResult.fatalError(message));
        }

        if (transferProcess.getState() >= TransferProcessStates.COMPLETING.code()) {
            entry.transitionToCompleted();
            update(entry);
            return CompletableFuture.completedFuture(StatusResult.success());
        }

        var contractAgreement = contractAgreementService.findById(entry.getContractId());
        if (contractAgreement == null) {
            var message = "ContractAgreement %s does not exist".formatted(entry.getContractId());
            entry.transitionToFailed(message);
            update(entry);
            return CompletableFuture.completedFuture(StatusResult.fatalError(message));
        }

        var policy = contractAgreement.getPolicy();
        var policyContext = new PolicyMonitorContext(Instant.now(clock), contractAgreement);

        var result = policyEngine.evaluate(policy, policyContext);
        if (result.failed()) {
            monitor.debug(() -> "[policy-monitor] Policy evaluation for TP %s failed: %s".formatted(entry.getId(), result.getFailureDetail()));
            var command = new TerminateTransferCommand(entry.getId(), result.getFailureDetail());
            var terminationResult = transferProcessService.terminate(command);
            if (terminationResult.succeeded()) {
                entry.transitionToCompleted();
                update(entry);
                return CompletableFuture.completedFuture(StatusResult.success());
            }
        }

        // we update the state timestamp ensure fairness on polling on  `STARTED` state
        entry.updateStateTimestamp();
        store.save(entry);
        return CompletableFuture.completedFuture(StatusResult.success());
    }

    private Processor processEntriesInState(PolicyMonitorEntryStates state, Function<PolicyMonitorEntry, CompletableFuture<StatusResult<Void>>> function) {
        var filter = new Criterion[]{ hasState(state.code()) };
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter), entityRetryProcessConfiguration, clock, monitor)
                .process(telemetry.contextPropagationMiddleware(function))
                .onNotProcessed(this::breakLease)
                .build();
    }

    public static class Builder
            extends AbstractStateEntityManager.Builder<PolicyMonitorEntry, PolicyMonitorStore, PolicyMonitorManagerImpl, Builder> {

        private Builder() {
            super(new PolicyMonitorManagerImpl());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder contractAgreementService(ContractAgreementService contractAgreementService) {
            manager.contractAgreementService = contractAgreementService;
            return this;
        }

        public Builder policyEngine(PolicyEngine policyEngine) {
            manager.policyEngine = policyEngine;
            return this;
        }

        public Builder transferProcessService(TransferProcessService transferProcessService) {
            manager.transferProcessService = transferProcessService;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}
