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

package org.eclipse.edc.nats.tasks.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.ContractNegotiationTaskPayload;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.RequestNegotiation;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.transfer.spi.tasks.PrepareTransfer;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TransferProcessTaskPayload;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NatsTaskPublisherTest {

    private final JetStream jetStream = mock();
    private final Monitor monitor = mock();

    @BeforeEach
    void setUp() {
    }

    @Test
    void created_shouldPublishContractNegotiationTaskToCorrectSubject() throws Exception {
        var publisher = new NatsTaskPublisher("negotiations", ContractNegotiationTaskPayload.class, jetStream, monitor, ObjectMapper::new);

        var payload = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-123")
                .processState(100)
                .processType("CONSUMER")
                .build();
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();

        publisher.created(task);

        verify(jetStream).publish(eq("negotiations.consumer.negotiation.request.prepare"), isA(byte[].class));
    }

    @Test
    void created_shouldPublishTransferProcessTaskToCorrectSubject() throws Exception {
        var publisher = new NatsTaskPublisher("transfers", TransferProcessTaskPayload.class, jetStream, monitor, ObjectMapper::new);

        var payload = PrepareTransfer.Builder.newInstance()
                .processId("transfer-123")
                .processState(100)
                .processType("CONSUMER")
                .build();
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();

        publisher.created(task);

        verify(jetStream).publish(eq("transfers.consumer.transfer.prepare"), isA(byte[].class));
    }

    @Test
    void created_shouldSerializeTaskAsJsonBytes() throws Exception {
        var publisher = new NatsTaskPublisher("negotiations", ContractNegotiationTaskPayload.class, jetStream, monitor, ObjectMapper::new);

        var payload = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-123")
                .processState(100)
                .processType("CONSUMER")
                .build();
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();

        publisher.created(task);

        verify(jetStream).publish(any(String.class), isA(byte[].class));
    }

    @Test
    void created_shouldThrowEdcExceptionOnSerializationFailure() {
        var publisher = new NatsTaskPublisher("negotiations", ProcessTaskPayload.class, jetStream, monitor, ObjectMapper::new);

        var mockPayload = mock(ProcessTaskPayload.class);
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(mockPayload)
                .name("test-task")
                .group("test-group")
                .build();

        // ObjectMapper will fail to serialize the mock object
        assertThatThrownBy(() -> publisher.created(task))
                .isInstanceOf(EdcException.class);

        verify(monitor).severe(any(String.class), any(Exception.class));
    }

    @Test
    void created_shouldThrowEdcExceptionOnPublishFailure() throws Exception {
        var publisher = new NatsTaskPublisher("negotiations", ContractNegotiationTaskPayload.class, jetStream, monitor, ObjectMapper::new);

        var payload = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-123")
                .processState(100)
                .processType("CONSUMER")
                .build();
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();
        var exception = new RuntimeException("NATS publish failed");
        doThrow(exception).when(jetStream).publish(any(String.class), any(byte[].class));

        assertThatThrownBy(() -> publisher.created(task))
                .isInstanceOf(EdcException.class)
                .hasCause(exception);

        verify(monitor).severe(any(String.class), eq(exception));
    }

    @Test
    void created_shouldSkipPublish_ForUnsupportedPayloadType() {
        var publisher = new NatsTaskPublisher("negotiations", ContractNegotiationTaskPayload.class, jetStream, monitor, ObjectMapper::new);

        var payload = new UnsupportedPayload("process-1", 100, "CONSUMER");
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();

        publisher.created(task);

        verifyNoInteractions(jetStream);
    }

    @Test
    void created_shouldPublishWithCorrectMessageFormat() throws Exception {
        var publisher = new NatsTaskPublisher("transfers", TransferProcessTaskPayload.class, jetStream, monitor, ObjectMapper::new);

        var payload = PrepareTransfer.Builder.newInstance()
                .processId("transfer-456")
                .processState(600)
                .processType("PROVIDER")
                .build();
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();

        publisher.created(task);

        verify(jetStream).publish(any(String.class), isA(byte[].class));
    }

    @Test
    void created_shouldLogErrorWithTaskIdOnPublishFailure() throws Exception {
        var publisher = new NatsTaskPublisher("negotiations", ContractNegotiationTaskPayload.class, jetStream, monitor, ObjectMapper::new);

        var payload = RequestNegotiation.Builder.newInstance()
                .processId("negotiation-789")
                .processState(100)
                .processType("PROVIDER")
                .build();
        var task = Task.Builder.newInstance()
                .at(System.currentTimeMillis())
                .payload(payload)
                .build();
        var exception = new RuntimeException("Publish error");
        doThrow(exception).when(jetStream).publish(any(String.class), any(byte[].class));

        assertThatThrownBy(() -> publisher.created(task))
                .isInstanceOf(EdcException.class);

        verify(monitor).severe(any(String.class), eq(exception));
    }

    /**
     * Unsupported task payload for testing error handling
     */
    private static class UnsupportedPayload extends ProcessTaskPayload {
        UnsupportedPayload(String processId, Integer processState, String processType) {
            this.processId = processId;
            this.processState = processState;
            this.processType = processType;
        }

        @Override
        public String name() {
            return "unsupported.payload";
        }

        @Override
        public String group() {
            return "unsupported";
        }
    }
}
