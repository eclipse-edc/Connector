/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.policy.monitor.manager;

import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntry;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorStore;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.telemetry.Telemetry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;
import java.time.Instant;

public class PolicyMonitor {

    private final PolicyMonitorStore store;
    private final Telemetry telemetry;
    private final TransferProcessService transferProcessService;
    private final ContractAgreementService contractAgreementService;
    private final PolicyEngine policyEngine;
    private final Monitor monitor;
    private final Clock clock;
    private final TransactionContext transactionContext;

    public PolicyMonitor(PolicyMonitorStore store, Telemetry telemetry, TransferProcessService transferProcessService,
                         ContractAgreementService contractAgreementService, PolicyEngine policyEngine, Monitor monitor,
                         Clock clock, TransactionContext transactionContext) {
        this.store = store;
        this.telemetry = telemetry;
        this.transferProcessService = transferProcessService;
        this.contractAgreementService = contractAgreementService;
        this.policyEngine = policyEngine;
        this.monitor = monitor.withPrefix(getClass().getSimpleName());
        this.clock = clock;
        this.transactionContext = transactionContext;
    }

    public StatusResult<Void> start(String transferProcessId, String contractId) {
        var entry = PolicyMonitorEntry.Builder.newInstance()
                .id(transferProcessId)
                .contractId(contractId)
                .traceContext(telemetry.getCurrentTraceContext())
                .build();

        entry.transitionToStarted();

        return store.save(entry).flatMap(StatusResult::from);
    }

    public void monitor(PolicyMonitorEntry entry) {
        transactionContext.execute(() -> {
            var transferProcess = transferProcessService.findById(entry.getId());
            if (transferProcess == null) {
                var message = "TransferProcess %s does not exist".formatted(entry.getId());
                entry.transitionToFailed(message);
                update(entry);
                return;
            }

            if (transferProcess.getState() >= TransferProcessStates.COMPLETING.code()) {
                entry.transitionToCompleted();
                update(entry);
                return;
            }

            var contractAgreement = contractAgreementService.findById(entry.getContractId());
            if (contractAgreement == null) {
                var message = "ContractAgreement %s does not exist".formatted(entry.getContractId());
                entry.transitionToFailed(message);
                update(entry);
                return;
            }

            var policy = contractAgreement.getPolicy();
            var policyContext = new PolicyMonitorContext(Instant.now(clock), contractAgreement);

            var result = policyEngine.evaluate(policy, policyContext);
            if (result.failed()) {
                monitor.debug(() -> "Policy evaluation for TP %s failed: %s".formatted(entry.getId(), result.getFailureDetail()));
                var command = new TerminateTransferCommand(entry.getId(), result.getFailureDetail());
                var terminationResult = transferProcessService.terminate(command);
                if (terminationResult.succeeded()) {
                    entry.transitionToCompleted();
                    update(entry);
                    return;
                } else {
                    monitor.severe("Cannot terminate Transfer %s because: %s".formatted(entry.getId(), terminationResult.getFailureDetail()));
                }
            }

            entry.updateStateTimestamp();
            store.save(entry);
        });
    }

    private void update(PolicyMonitorEntry entry) {
        store.save(entry).onSuccess(it -> {
            var error = entry.getErrorDetail() == null ? "" : ". errorDetail: " + entry.getErrorDetail();
            monitor.debug(() -> "PolicyMonitorEntry %s is now in state %s%s"
                    .formatted(entry.getId(), entry.stateAsString(), error));
        });
    }
}
