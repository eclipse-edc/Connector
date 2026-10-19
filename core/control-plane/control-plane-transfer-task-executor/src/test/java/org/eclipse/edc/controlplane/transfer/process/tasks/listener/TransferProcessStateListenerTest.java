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

package org.eclipse.edc.controlplane.transfer.process.tasks.listener;

import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SendTransferRequest;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SendTransferStart;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TransferProcessTaskPayload;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransferProcessStateListenerTest {

    private final TaskService taskService = mock();
    private final TransferProcessStateListener listener = new TransferProcessStateListener(taskService, Clock.systemUTC());

    @BeforeEach
    void setUp() {
        when(taskService.create(any())).thenAnswer(i -> ServiceResult.success(i.getArgument(0)));
    }

    @Nested
    class Prepared {

        @Test
        void shouldCreateSendTransferRequest_whenConsumerIsRequesting() {
            var process = transferProcess(CONSUMER, REQUESTING);

            listener.prepared(process);

            assertThat(createdTask()).isInstanceOf(SendTransferRequest.class)
                    .satisfies(payload -> {
                        assertThat(payload.getProcessId()).isEqualTo(process.getId());
                        assertThat(payload.getProcessState()).isEqualTo(REQUESTING.code());
                    });
        }

        @Test
        void shouldCreateSendTransferStart_whenProviderIsStarting() {
            var process = transferProcess(PROVIDER, STARTING);

            listener.prepared(process);

            assertThat(createdTask()).isInstanceOf(SendTransferStart.class)
                    .extracting(TransferProcessTaskPayload::getProcessState).isEqualTo(STARTING.code());
        }

        @Test
        void shouldNotCreateTask_whenStateIsUnexpected() {
            listener.prepared(transferProcess(CONSUMER, STARTED));

            verifyNoInteractions(taskService);
        }
    }

    @Nested
    class Started {

        @Test
        void shouldCreateSendTransferStart_whenProviderIsStarting() {
            var process = transferProcess(PROVIDER, STARTING);

            listener.started(process, TransferProcessStartedData.Builder.newInstance().build());

            assertThat(createdTask()).isInstanceOf(SendTransferStart.class)
                    .extracting(TransferProcessTaskPayload::getProcessState).isEqualTo(STARTING.code());
        }

        @Test
        void shouldNotCreateTask_whenProviderIsStarted() {
            listener.started(transferProcess(PROVIDER, STARTED), TransferProcessStartedData.Builder.newInstance().build());

            verifyNoInteractions(taskService);
        }

        @Test
        void shouldNotCreateTask_whenConsumerIsStarted() {
            listener.started(transferProcess(CONSUMER, STARTED), TransferProcessStartedData.Builder.newInstance().build());

            verifyNoInteractions(taskService);
        }
    }

    private TransferProcessTaskPayload createdTask() {
        var captor = ArgumentCaptor.forClass(Task.class);
        verify(taskService).create(captor.capture());
        return (TransferProcessTaskPayload) captor.getValue().getPayload();
    }

    private TransferProcess transferProcess(TransferProcess.Type type, TransferProcessStates state) {
        return TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .type(type)
                .state(state.code())
                .build();
    }
}
