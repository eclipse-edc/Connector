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

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.NotificationMessage;
import de.fraunhofer.iais.eis.NotificationMessageBuilder;
import de.fraunhofer.iais.eis.ParticipantUpdateMessage;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.dataspaceconnector.spi.types.domain.edr.EndpointDataReferenceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MultipartEndpointDataReferenceRequestSenderTest {

    private MultipartEndpointDataReferenceRequestSender sender;
    private String connectorId;
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        connectorId = UUID.randomUUID().toString();
        var httpClient = mock(OkHttpClient.class);
        var monitor = mock(Monitor.class);
        var transformerRegistry = mock(TransformerRegistry.class);
        var identityService = mock(IdentityService.class);
        mapper = new ObjectMapper();
        sender = new MultipartEndpointDataReferenceRequestSender(connectorId, httpClient, mapper, monitor, identityService, transformerRegistry);
    }

    @Test
    void retrieveRemoteConnectorAddress() {
        var request = createEdrRequest();
        assertThat(sender.retrieveRemoteConnectorAddress(request)).isEqualTo(request.getConnectorAddress());
    }

    @Test
    void buildMessageHeader() {
        var request = createEdrRequest();
        var datToken = createDatToken();

        var header = sender.buildMessageHeader(request, datToken);

        var uriConnectorId = URI.create(connectorId);

        assertThat(header).isInstanceOf(ParticipantUpdateMessage.class);
        assertThat((ParticipantUpdateMessage) header).satisfies(h -> {
            assertThat(h.getModelVersion()).isEqualTo(IdsProtocol.INFORMATION_MODEL_VERSION);
            assertThat(h.getSecurityToken()).isEqualTo(datToken);
            assertThat(h.getIssuerConnector()).isEqualTo(sender.getConnectorId());
            assertThat(h.getSenderAgent()).isEqualTo(sender.getConnectorId());
            assertThat(h.getRecipientAgent())
                    .allMatch(uri -> uri.equals(URI.create(request.getConnectorId())));
        });
    }

    @Test
    void buildMessagePayload() throws Exception {
        var request = createEdrRequest();
        var payload = sender.buildMessagePayload(request);

        var edr = mapper.readValue(payload, EndpointDataReference.class);
        assertThat(edr.getAuthCode()).isEqualTo(request.getEndpointDataReference().getAuthCode());
        assertThat(edr.getCorrelationId()).isEqualTo(request.getEndpointDataReference().getCorrelationId());
        assertThat(edr.getExpirationEpochSeconds()).isEqualTo(request.getEndpointDataReference().getExpirationEpochSeconds());
        assertThat(edr.getAddress()).isEqualTo(request.getEndpointDataReference().getAddress());
    }

    @Test
    void getResponseContent() throws Exception {
        var header = new NotificationMessageBuilder()._contentVersion_(UUID.randomUUID().toString()).build();
        var payload = UUID.randomUUID().toString();
        var parts = new IdsMultipartParts(new ByteArrayInputStream(mapper.writeValueAsBytes(header)), new ByteArrayInputStream(payload.getBytes()));
        var response = sender.getResponseContent(parts);

        assertThat(response).isNotNull();
        assertThat(response.getHeader())
                .isNotNull()
                .isInstanceOf(NotificationMessage.class)
                .satisfies(h -> assertThat(h.getContentVersion()).isEqualTo(header.getContentVersion()));
        assertThat(response.getPayload())
                .isNotNull()
                .isEqualTo(payload);
    }

    private static DynamicAttributeToken createDatToken() {
        return new DynamicAttributeTokenBuilder()._tokenValue_(UUID.randomUUID().toString()).build();
    }

    private EndpointDataReferenceRequest createEdrRequest() {
        return EndpointDataReferenceRequest.Builder.newInstance()
                .protocol("test-protocol")
                .connectorAddress("http://consumer-connector.com")
                .connectorId(UUID.randomUUID().toString())
                .endpointDataReference(EndpointDataReference.Builder.newInstance()
                        .address("http://provider-connector.com")
                        .correlationId(UUID.randomUUID().toString())
                        .authKey("Api-Key")
                        .contractId(UUID.randomUUID().toString())
                        .authCode(UUID.randomUUID().toString())
                        .expirationEpochSeconds(Instant.now().plusSeconds(100).getEpochSecond())
                        .build())
                .build();
    }
}
