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

package org.eclipse.edc.virtual.controlplane.contract.negotiation.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.controlplane.contract.spi.negotiation.ContractNegotiationTaskExecutor;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.AgreeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.ContractNegotiationTaskPayload;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.FinalizeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.RequestNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendAgreement;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendFinalizeNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendRequestNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.SendVerificationNegotiation;
import org.eclipse.edc.controlplane.contract.spi.negotiation.tasks.VerifyNegotiation;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;
import org.eclipse.edc.controlplane.tasks.Task;
import org.eclipse.edc.controlplane.tasks.TaskService;
import org.eclipse.edc.controlplane.tasks.TaskTypes;
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
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREEING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.INITIAL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFIED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.VERIFYING;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NatsContractNegotiationTaskSubscriberTest {

    public static final String STREAM_NAME = "stream_test";
    public static final String CONSUMER_NAME = "consumer_test";
    @Order(0)
    @RegisterExtension
    static final NatsEndToEndExtension NATS_EXTENSION = new NatsEndToEndExtension();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ContractNegotiationTaskExecutor taskManager = mock();
    private final TaskService taskService = mock();
    private NatsContractNegotiationTaskSubscriber subscriber;

    @BeforeAll
    static void beforeAll() {
        TaskTypes.TYPES.forEach(MAPPER::registerSubtypes);
    }

    protected static <T extends ProcessTaskPayload, B extends ProcessTaskPayload.Builder<T, B>> B baseBuilder(B builder, String id, ContractNegotiationStates state, ContractNegotiation.Type type) {
        return builder.processId(id)
                .processState(state.code())
                .processType(type.name());
    }

    @BeforeEach
    void beforeEach() {
        NATS_EXTENSION.createStream(STREAM_NAME, "negotiations.>");
        NATS_EXTENSION.createConsumer(STREAM_NAME, CONSUMER_NAME, "negotiations.>");
        subscriber = NatsContractNegotiationTaskSubscriber.Builder.newInstance()
                .url(NATS_EXTENSION.getNatsUrl())
                .name(CONSUMER_NAME)
                .stream(STREAM_NAME)
                .subject("negotiations.>")
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
    void handleMessage(ContractNegotiationTaskPayload payload) throws JsonProcessingException {
        when(taskService.findById(any())).thenReturn(mock());
        when(taskManager.handle(any())).thenReturn(StatusResult.success());
        subscriber.start();
        var task = Task.Builder.newInstance().at(System.currentTimeMillis())
                .payload(payload)
                .build();

        NATS_EXTENSION.publish("negotiations.provider." + payload.name(), MAPPER.writeValueAsBytes(task));

        await().untilAsserted(() -> {
            verify(taskManager).handle(refEq(payload));
            verify(taskService).delete(any());
        });
    }

    @Test
    void handleRetryMessage_withLimit() throws JsonProcessingException {
        var payload = baseBuilder(RequestNegotiation.Builder.newInstance(), UUID.randomUUID().toString(), INITIAL, CONSUMER).build();
        var task = Task.Builder.newInstance().at(System.currentTimeMillis())
                .payload(payload)
                .build();

        when(taskService.findById(any())).thenReturn(task)
                .thenReturn(task.toBuilder().retryCount(task.getRetryCount() + 1).build())
                .thenReturn(task.toBuilder().retryCount(task.getRetryCount() + 2).build());

        when(taskManager.handle(any())).thenReturn(StatusResult.failure(ERROR_RETRY));
        subscriber.start();

        NATS_EXTENSION.publish("negotiations.provider." + payload.name(), MAPPER.writeValueAsBytes(task));

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            verify(taskManager, times(3)).handle(refEq(payload));
            verify(taskService).delete(task.getId());
            verify(taskService, times(2)).update(any());
        });
    }

    public static class StateTransitionProvider implements ArgumentsProvider {


        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            return Stream.of(
                    arguments(baseBuilder(RequestNegotiation.Builder.newInstance(), "id", INITIAL, CONSUMER).build(), REQUESTING),
                    arguments(baseBuilder(SendRequestNegotiation.Builder.newInstance(), "id", REQUESTING, CONSUMER).build(), REQUESTED),
                    arguments(baseBuilder(VerifyNegotiation.Builder.newInstance(), "id", AGREED, CONSUMER).build(), VERIFYING),
                    arguments(baseBuilder(SendVerificationNegotiation.Builder.newInstance(), "id", VERIFYING, CONSUMER).build(), VERIFIED),
                    arguments(baseBuilder(AgreeNegotiation.Builder.newInstance(), "id", REQUESTED, PROVIDER).build(), AGREEING),
                    arguments(baseBuilder(SendAgreement.Builder.newInstance(), "id", AGREEING, PROVIDER).build(), AGREED),
                    arguments(baseBuilder(FinalizeNegotiation.Builder.newInstance(), "id", VERIFIED, PROVIDER).build(), FINALIZING),
                    arguments(baseBuilder(SendFinalizeNegotiation.Builder.newInstance(), "id", FINALIZING, PROVIDER).build(), FINALIZED)
            );
        }
    }


}
