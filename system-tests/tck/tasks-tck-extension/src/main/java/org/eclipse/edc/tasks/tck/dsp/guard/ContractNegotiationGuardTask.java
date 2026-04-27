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

package org.eclipse.edc.tasks.tck.dsp.guard;

import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.ContractNegotiationTaskPayload;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendAccept;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendAgreement;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendFinalizeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendOffer;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendRequestNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendTerminateNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendVerificationNegotiation;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;

import java.time.Clock;

public class ContractNegotiationGuardTask {

    private final TaskService taskService;
    private final Clock clock;

    public ContractNegotiationGuardTask(TaskService taskService, Clock clock) {
        this.taskService = taskService;
        this.clock = clock;
    }

    public void sendOffer(ContractNegotiation contractNegotiation) {
        contractNegotiation.transitionOffering();
        storeTask(baseBuilder(SendOffer.Builder.newInstance(), contractNegotiation).build());
    }

    public void sendAgreement(ContractNegotiation contractNegotiation) {
        contractNegotiation.transitionAgreeing();
        storeTask(baseBuilder(SendAgreement.Builder.newInstance(), contractNegotiation).build());
    }

    public void sendTermination(ContractNegotiation contractNegotiation) {
        contractNegotiation.transitionTerminating();
        storeTask(baseBuilder(SendTerminateNegotiation.Builder.newInstance(), contractNegotiation).build());
    }

    public void sendAccept(ContractNegotiation contractNegotiation) {
        contractNegotiation.transitionAccepting();
        storeTask(baseBuilder(SendAccept.Builder.newInstance(), contractNegotiation).build());
    }

    public void contractRequest(ContractNegotiation contractNegotiation) {
        contractNegotiation.transitionRequesting();
        storeTask(baseBuilder(SendRequestNegotiation.Builder.newInstance(), contractNegotiation).build());
    }

    public void sendVerification(ContractNegotiation contractNegotiation) {
        contractNegotiation.transitionVerifying();
        storeTask(baseBuilder(SendVerificationNegotiation.Builder.newInstance(), contractNegotiation).build());
    }

    public void sendFinalize(ContractNegotiation contractNegotiation) {
        contractNegotiation.transitionFinalizing();
        storeTask(baseBuilder(SendFinalizeNegotiation.Builder.newInstance(), contractNegotiation).build());
    }

    private void storeTask(ContractNegotiationTaskPayload payload) {
        var task = Task.Builder.newInstance().at(clock.millis())
                .payload(payload)
                .build();
        taskService.create(task);
    }

    protected <T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> B baseBuilder(B builder, ContractNegotiation negotiation) {
        return builder.processId(negotiation.getId())
                .processState(negotiation.getState())
                .processType(negotiation.getType().name());
    }
}
