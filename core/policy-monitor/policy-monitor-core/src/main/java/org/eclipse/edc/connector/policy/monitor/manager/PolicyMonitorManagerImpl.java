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

import org.eclipse.edc.connector.core.entity.AbstractStateEntityManager;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorManager;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.connector.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.statemachine.Processor;
import org.eclipse.edc.statemachine.ProcessorImpl;
import org.eclipse.edc.statemachine.StateMachineManager;

import java.time.Instant;
import java.util.function.Function;

import static org.eclipse.edc.connector.policy.monitor.PolicyMonitorExtension.POLICY_MONITOR_SCOPE;
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
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder
                .processor(processEntriesInState(STARTED, this::processMonitoring));
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

    private boolean processMonitoring(PolicyMonitorEntry entry) {
        var transferProcess = transferProcessService.findById(entry.getId());
        if (transferProcess == null) {
            entry.transitionToFailed("TransferProcess %s does not exist".formatted(entry.getId()));
            update(entry);
            return true;
        }

        if (transferProcess.getState() >= TransferProcessStates.COMPLETING.code()) {
            entry.transitionToCompleted();
            update(entry);
            return true;
        }

        var contractAgreement = contractAgreementService.findById(entry.getContractId());
        if (contractAgreement == null) {
            entry.transitionToFailed("ContractAgreement %s does not exist".formatted(entry.getContractId()));
            update(entry);
            return true;
        }

        var policy = contractAgreement.getPolicy();
        var policyContext = PolicyContextImpl.Builder.newInstance()
                .additional(Instant.class, Instant.now(clock))
                .additional(ContractAgreement.class, contractAgreement)
                .build();

        var result = policyEngine.evaluate(POLICY_MONITOR_SCOPE, policy, policyContext);
        if (result.failed()) {
            monitor.debug(() -> "[policy-monitor] Policy evaluation for TP %s failed: %s".formatted(entry.getId(), result.getFailureDetail()));
            var command = new TerminateTransferCommand(entry.getId(), result.getFailureDetail());
            var terminationResult = transferProcessService.terminate(command);
            if (terminationResult.succeeded()) {
                entry.transitionToCompleted();
                update(entry);
                return true;
            }
        }

        breakLease(entry);
        return true;
    }

    private Processor processEntriesInState(PolicyMonitorEntryStates state, Function<PolicyMonitorEntry, Boolean> function) {
        var filter = new Criterion[]{ hasState(state.code()) };
        return ProcessorImpl.Builder.newInstance(() -> store.nextNotLeased(batchSize, filter))
                .process(telemetry.contextPropagationMiddleware(function))
                .onNotProcessed(this::breakLease)
                .build();
    }

    public static class Builder
            extends AbstractStateEntityManager.Builder<PolicyMonitorEntry, PolicyMonitorStore, PolicyMonitorManagerImpl, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new PolicyMonitorManagerImpl());
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
