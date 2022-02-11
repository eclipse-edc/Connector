/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.ParticipantCertificateRevokedMessageBuilder;
import de.fraunhofer.iais.eis.ParticipantUpdateMessageBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiver;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformer;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.transfer.core.edr.EndpointDataReferenceReceiverRegistryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EndpointDataReferenceHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EndpointDataReferenceHandler handler;
    private EndpointDataReferenceReceiverRegistry receiverRegistry;
    private EndpointDataReferenceTransformer transformer;

    @BeforeEach
    public void setUp() {
        var monitor = mock(Monitor.class);
        var connectorId = UUID.randomUUID().toString();
        receiverRegistry = new EndpointDataReferenceReceiverRegistryImpl();
        transformer = mock(EndpointDataReferenceTransformer.class);
        var typeManager = new TypeManager();
        handler = new EndpointDataReferenceHandler(monitor, connectorId, receiverRegistry, transformer, typeManager);
    }

    @Test
    void canHandle_supportedMessage_shouldReturnTrue() throws JsonProcessingException {
        var request = createMultipartRequest(createEndpointDataReference());
        assertThat(handler.canHandle(request)).isTrue();
    }

    @Test
    void canHandle_messageNotSupported_shouldReturnFalse() {
        var request = MultipartRequest.Builder.newInstance()
                .header(new ParticipantCertificateRevokedMessageBuilder().build())
                .build();
        assertThat(handler.canHandle(request)).isFalse();
    }

    @Test
    void handleRequest_success_shouldReturnMessageProcessedNotification() throws JsonProcessingException {
        var edr = createEndpointDataReference();
        var request = createMultipartRequest(edr);

        var edrCapture = ArgumentCaptor.forClass(EndpointDataReference.class);
        when(transformer.execute(edrCapture.capture())).thenReturn(Result.success(edr));
        var receiver = mock(EndpointDataReferenceReceiver.class);
        when(receiver.send(edr)).thenReturn(CompletableFuture.completedFuture(Result.success()));
        receiverRegistry.addReceiver(receiver);

        var response = handler.handleRequest(request, createSuccessClaimToken());

        verify(transformer).execute(edrCapture.capture());
        assertThat(edrCapture.getValue()).satisfies(t -> {
            assertThat(t.getAddress()).isEqualTo(edr.getAddress());
            assertThat(t.getAuthCode()).isEqualTo(edr.getAuthCode());
            assertThat(t.getCorrelationId()).isEqualTo(edr.getCorrelationId());
            assertThat(t.getExpirationEpochSeconds()).isEqualTo(edr.getExpirationEpochSeconds());
        });
        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(MessageProcessedNotificationMessage.class));
    }

    @Test
    void handleRequest_transformationFailure_shouldReturnRejectionMessage() throws JsonProcessingException {
        var edr = createEndpointDataReference();
        var request = createMultipartRequest(edr);

        when(transformer.execute(any())).thenReturn(Result.failure("error"));

        var response = handler.handleRequest(request, createSuccessClaimToken());

        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(RejectionMessage.class));
    }

    @Test
    void handleRequest_dispatchFailure_shouldReturnMessageProcessedNotification() throws JsonProcessingException {
        var edr = createEndpointDataReference();
        var request = createMultipartRequest(edr);

        when(transformer.execute(any())).thenReturn(Result.success(edr));
        var receiver = mock(EndpointDataReferenceReceiver.class);
        when(receiver.send(edr)).thenReturn(CompletableFuture.completedFuture(Result.failure("error")));
        receiverRegistry.addReceiver(receiver);

        var response = handler.handleRequest(request, createSuccessClaimToken());

        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(RejectionMessage.class));
    }

    @Test
    void handleRequest_dispatchUnhandledException_shouldReturnMessageProcessedNotification() throws JsonProcessingException {
        var edr = createEndpointDataReference();
        var request = createMultipartRequest(edr);

        when(transformer.execute(any())).thenReturn(Result.success(edr));
        var receiver = mock(EndpointDataReferenceReceiver.class);
        when(receiver.send(edr)).thenReturn(CompletableFuture.failedFuture(new RuntimeException("error")));
        receiverRegistry.addReceiver(receiver);

        var response = handler.handleRequest(request, createSuccessClaimToken());

        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(RejectionMessage.class));
    }

    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .address("http://example.com")
                .authKey("Api-Key")
                .authCode(UUID.randomUUID().toString())
                .correlationId("correlation-test")
                .expirationEpochSeconds(Instant.now().plusSeconds(100).getEpochSecond())
                .contractId(UUID.randomUUID().toString())
                .build();
    }

    private static MultipartRequest createMultipartRequest(EndpointDataReference payload) throws JsonProcessingException {
        return MultipartRequest.Builder.newInstance()
                .header(new ParticipantUpdateMessageBuilder().build())
                .payload(MAPPER.writeValueAsString(payload))
                .build();
    }

    private static Result<ClaimToken> createSuccessClaimToken() {
        return Result.success(ClaimToken.Builder.newInstance().build());
    }
}