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

package org.eclipse.edc.virtual.controlplane.transfer.subscriber.nats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.controlplane.tasks.TaskTypes;
import org.eclipse.edc.controlplane.transfer.spi.TransferProcessTaskExecutor;
import org.eclipse.edc.controlplane.transfer.spi.tasks.CompleteDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.PrepareTransfer;
import org.eclipse.edc.controlplane.transfer.spi.tasks.ResumeDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SendTransferRequest;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SendTransferStart;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SignalDataflowStarted;
import org.eclipse.edc.controlplane.transfer.spi.tasks.SuspendDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TerminateDataFlow;
import org.eclipse.edc.controlplane.transfer.spi.tasks.TransferProcessTaskPayload;
import org.eclipse.edc.nats.testfixtures.NatsEndToEndExtension;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NatsTransferProcessSubscriberTest {

    public static final String STREAM_NAME = "stream_test";
    public static final String CONSUMER_NAME = "consumer_test";
    @Order(0)
    @RegisterExtension
    static final NatsEndToEndExtension NATS_EXTENSION = new NatsEndToEndExtension();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final TransferProcessTaskExecutor taskManager = mock();
    private final TaskService taskService = mock();
    private NatsTransferProcessTaskSubscriber subscriber;

    @BeforeAll
    static void beforeAll() {
        TaskTypes.TYPES.forEach(MAPPER::registerSubtypes);
    }

    protected static <T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> B baseBuilder(B builder, String id, TransferProcessStates state, TransferProcess.Type type) {
        return builder.processId(id)
                .processState(state.code())
                .processType(type.name());
    }

    @BeforeEach
    void beforeEach() {
        NATS_EXTENSION.createStream(STREAM_NAME, "transfers.>");
        NATS_EXTENSION.createConsumer(STREAM_NAME, CONSUMER_NAME, "transfers.>");


        subscriber = NatsTransferProcessTaskSubscriber.Builder.newInstance()
                .url(NATS_EXTENSION.getNatsUrl())
                .name(CONSUMER_NAME)
                .stream(STREAM_NAME)
                .subject("transfers.>")
                .monitor(mock())
                .mapperSupplier(() -> MAPPER)
                .taskExecutor(taskManager)
                .taskService(taskService)
                .transactionContext(new NoopTransactionContext())
                .clock(Clock.systemUTC())
                .maxRetries(2)
                .build();
    }

    @AfterEach
    void afterEach() {
        subscriber.stop();
        NATS_EXTENSION.deleteStream(STREAM_NAME);
    }

    @ParameterizedTest
    @ArgumentsSource(StateTransitionProvider.class)
    void handleMessage(TransferProcessTaskPayload payload) throws JsonProcessingException {
        var task = Task.Builder.newInstance().at(System.currentTimeMillis())
                .payload(payload)
                .build();

        when(taskService.findById(any())).thenReturn(task)
                .thenReturn(task.toBuilder().retryCount(task.getRetryCount() + 1).build())
                .thenReturn(task.toBuilder().retryCount(task.getRetryCount() + 2).build());

        when(taskManager.handle(any())).thenReturn(StatusResult.success());
        subscriber.start();

        NATS_EXTENSION.publish("transfers.provider." + payload.name(), MAPPER.writeValueAsBytes(task));

        await().untilAsserted(() -> {
            verify(taskManager).handle(refEq(payload));
        });
    }

    @Test
    void handleRetryMessage_withLimit() throws JsonProcessingException {
        var payload = baseBuilder(PrepareTransfer.Builder.newInstance(), UUID.randomUUID().toString(), INITIAL, CONSUMER).build();
        var task = Task.Builder.newInstance().at(System.currentTimeMillis())
                .payload(payload)
                .build();

        when(taskService.findById(any())).thenReturn(task)
                .thenReturn(task.toBuilder().retryCount(task.getRetryCount() + 1).build())
                .thenReturn(task.toBuilder().retryCount(task.getRetryCount() + 2).build());

        when(taskManager.handle(any())).thenReturn(StatusResult.failure(ERROR_RETRY));
        subscriber.start();

        NATS_EXTENSION.publish("transfers.provider." + payload.name(), MAPPER.writeValueAsBytes(task));

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            verify(taskManager, times(3)).handle(refEq(payload));
            verify(taskService).delete(task.getId());
            verify(taskService, times(2)).update(any());
        });
    }

    public static class StateTransitionProvider implements ArgumentsProvider {


        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            var id = UUID.randomUUID().toString();
            return Stream.of(
                    arguments(baseBuilder(PrepareTransfer.Builder.newInstance(), id, INITIAL, CONSUMER).build(), REQUESTING),
                    arguments(baseBuilder(SendTransferRequest.Builder.newInstance(), id, REQUESTING, CONSUMER).build(), REQUESTED),
                    arguments(baseBuilder(SignalDataflowStarted.Builder.newInstance(), id, REQUESTED, CONSUMER).build(), STARTED),
                    arguments(baseBuilder(SuspendDataFlow.Builder.newInstance(), id, SUSPENDING, CONSUMER).build(), SUSPENDED),
                    arguments(baseBuilder(ResumeDataFlow.Builder.newInstance(), id, RESUMING, CONSUMER).build(), RESUMED),
                    arguments(baseBuilder(TerminateDataFlow.Builder.newInstance(), id, TERMINATING, CONSUMER).build(), TERMINATED),
                    arguments(baseBuilder(CompleteDataFlow.Builder.newInstance(), id, COMPLETING, CONSUMER).build(), COMPLETED),

                    arguments(baseBuilder(PrepareTransfer.Builder.newInstance(), id, INITIAL, PROVIDER).build(), STARTING),
                    arguments(baseBuilder(SendTransferStart.Builder.newInstance(), id, STARTING, PROVIDER).build(), STARTED),
                    arguments(baseBuilder(SuspendDataFlow.Builder.newInstance(), id, SUSPENDING, PROVIDER).build(), SUSPENDED),
                    arguments(baseBuilder(ResumeDataFlow.Builder.newInstance(), id, RESUMING, PROVIDER).build(), STARTED),
                    arguments(baseBuilder(TerminateDataFlow.Builder.newInstance(), id, TERMINATING, PROVIDER).build(), TERMINATED),
                    arguments(baseBuilder(CompleteDataFlow.Builder.newInstance(), id, COMPLETING, PROVIDER).build(), COMPLETED)
            );

        }
    }

}
