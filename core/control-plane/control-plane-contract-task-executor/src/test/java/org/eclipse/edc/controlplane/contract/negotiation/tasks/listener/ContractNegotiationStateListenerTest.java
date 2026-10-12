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

package org.eclipse.edc.controlplane.contract.negotiation.tasks.listener;

import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendAgreement;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendTerminateNegotiation;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ContractNegotiationStateListenerTest {

    private final TaskService taskService = mock();
    private final ContractNegotiationStateListener listener = new ContractNegotiationStateListener(taskService, Clock.systemUTC());

    @Test
    void approved_shouldStoreSendAgreementTask_whenProvider() {
        var negotiation = negotiation(PROVIDER, AGREEING);

        listener.approved(negotiation);

        var payload = capturePayload();
        assertThat(payload).isInstanceOf(SendAgreement.class);
        assertThat(payload.getProcessId()).isEqualTo(negotiation.getId());
        assertThat(payload.getProcessState()).isEqualTo(AGREEING.code());
        assertThat(payload.getProcessType()).isEqualTo(PROVIDER.name());
    }

    @Test
    void approved_shouldNotStoreTask_whenConsumer() {
        listener.approved(negotiation(CONSUMER, AGREEING));

        verify(taskService, never()).create(any());
    }

    @Test
    void terminating_shouldStoreSendTerminationTask() {
        var negotiation = negotiation(PROVIDER, TERMINATING);

        listener.terminating(negotiation);

        var payload = capturePayload();
        assertThat(payload).isInstanceOf(SendTerminateNegotiation.class);
        assertThat(payload.getProcessId()).isEqualTo(negotiation.getId());
        assertThat(payload.getProcessState()).isEqualTo(TERMINATING.code());
    }

    private ProcessTaskPayload capturePayload() {
        var captor = ArgumentCaptor.forClass(Task.class);
        verify(taskService).create(captor.capture());
        return (ProcessTaskPayload) captor.getValue().getPayload();
    }

    private ContractNegotiation negotiation(ContractNegotiation.Type type, ContractNegotiationStates state) {
        return ContractNegotiation.Builder.newInstance()
                .id("negotiation-id")
                .type(type)
                .state(state.code())
                .counterPartyId("counter-party")
                .counterPartyAddress("https://counter-party")
                .protocol("protocol")
                .build();
    }
}
