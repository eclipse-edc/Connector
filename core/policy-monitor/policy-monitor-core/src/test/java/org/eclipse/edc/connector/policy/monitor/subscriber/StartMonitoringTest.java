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

package org.eclipse.edc.connector.policy.monitor.subscriber;

import org.eclipse.edc.connector.controlplane.transfer.spi.event.TransferProcessStarted;
import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorManager;
import org.eclipse.edc.spi.event.EventEnvelope;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class StartMonitoringTest {

    private long now;
    private final PolicyMonitorManager manager = mock();

    @Test
    void shouldStartMonitoring_whenTransferProcessIsProvider() {
        var subscriber = new StartMonitoring(manager);
        now = Instant.now().toEpochMilli();
        var event = TransferProcessStarted.Builder.newInstance()
                .transferProcessId("transferProcessId")
                .contractId("contractId")
                .type(PROVIDER.name())
                .build();

        subscriber.on(envelope(event));

        verify(manager).startMonitoring("transferProcessId", "contractId");
    }

    @Test
    void shouldNotStartMonitoring_whenTransferProcessIsConsumer() {
        var subscriber = new StartMonitoring(manager);
        now = Instant.now().toEpochMilli();
        var event = TransferProcessStarted.Builder.newInstance()
                .transferProcessId("transferProcessId")
                .contractId("contractId")
                .type(CONSUMER.name())
                .build();

        subscriber.on(envelope(event));

        verifyNoInteractions(manager);
    }


    private EventEnvelope<TransferProcessStarted> envelope(TransferProcessStarted event) {
        return EventEnvelope.Builder.newInstance()
                .at(now)
                .payload(event)
                .build();
    }
}
