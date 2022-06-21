/*
 *  Copyright (c) 2022 Amadeus
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
import com.github.javafaker.Faker;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.ParticipantCertificateRevokedMessageBuilder;
import de.fraunhofer.iais.eis.ParticipantUpdateMessageBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EndpointDataReferenceHandlerTest {

    private static final Faker FAKER = new Faker();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EndpointDataReferenceHandler handler;
    private EndpointDataReferenceReceiverRegistry receiverRegistry;
    private EndpointDataReferenceTransformerRegistry transformerRegistry;

    @BeforeEach
    public void setUp() {
        var monitor = mock(Monitor.class);
        var connectorId = FAKER.lorem().word();
        receiverRegistry = mock(EndpointDataReferenceReceiverRegistry.class);
        transformerRegistry = mock(EndpointDataReferenceTransformerRegistry.class);
        var typeManager = new TypeManager();
        handler = new EndpointDataReferenceHandler(monitor, connectorId, receiverRegistry, transformerRegistry, typeManager);
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
        var inputEdr = createEndpointDataReference();
        var edrAfterTransformation = createEndpointDataReference();
        var request = createMultipartRequest(inputEdr);

        var edrCapture = ArgumentCaptor.forClass(EndpointDataReference.class);

        when(transformerRegistry.transform(any())).thenReturn(Result.success(edrAfterTransformation));
        when(receiverRegistry.receiveAll(edrAfterTransformation)).thenReturn(CompletableFuture.completedFuture(Result.success()));

        var response = handler.handleRequest(request, createClaimToken());

        verify(transformerRegistry, times(1)).transform(edrCapture.capture());

        assertThat(edrCapture.getValue()).satisfies(t -> {
            assertThat(t.getEndpoint()).isEqualTo(inputEdr.getEndpoint());
            assertThat(t.getAuthKey()).isEqualTo(inputEdr.getAuthKey());
            assertThat(t.getAuthCode()).isEqualTo(inputEdr.getAuthCode());
            assertThat(t.getId()).isEqualTo(inputEdr.getId());
            assertThat(t.getProperties()).isEqualTo(inputEdr.getProperties());
        });

        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(MessageProcessedNotificationMessage.class));
    }

    @Test
    void handleRequest_transformationFailure_shouldReturnRejectionMessage() throws JsonProcessingException {
        var edr = createEndpointDataReference();
        var request = createMultipartRequest(edr);

        when(transformerRegistry.transform(any())).thenReturn(Result.failure(FAKER.lorem().sentence()));

        var response = handler.handleRequest(request, createClaimToken());

        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(RejectionMessage.class));
    }

    @Test
    void handleRequest_receiveFailure_shouldReturnMessageProcessedNotification() throws JsonProcessingException {
        var edr = createEndpointDataReference();
        var request = createMultipartRequest(edr);

        when(transformerRegistry.transform(any())).thenReturn(Result.success(edr));
        when(receiverRegistry.receiveAll(edr)).thenReturn(CompletableFuture.completedFuture(Result.failure(FAKER.lorem().sentence())));

        var response = handler.handleRequest(request, createClaimToken());

        assertThat(response)
                .isNotNull()
                .satisfies(r -> assertThat(r.getHeader()).isInstanceOf(RejectionMessage.class));
    }

    private static EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .endpoint(FAKER.internet().url())
                .authKey(FAKER.lorem().word())
                .authCode(FAKER.internet().uuid())
                .id(FAKER.internet().uuid())
                .properties(Map.of(FAKER.lorem().word(), FAKER.internet().uuid()))
                .build();
    }

    private static MultipartRequest createMultipartRequest(EndpointDataReference payload) throws JsonProcessingException {
        return MultipartRequest.Builder.newInstance()
                .header(new ParticipantUpdateMessageBuilder().build())
                .payload(MAPPER.writeValueAsString(payload))
                .build();
    }

    private static ClaimToken createClaimToken() {
        return ClaimToken.Builder.newInstance().build();
    }
}