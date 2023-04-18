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
 *       Fraunhofer Institute for Software and Systems Engineering - replace object mapper
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.NotificationMessage;
import de.fraunhofer.iais.eis.NotificationMessageBuilder;
import de.fraunhofer.iais.eis.ParticipantUpdateMessage;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.edc.protocol.ids.serialization.IdsTypeManagerUtil;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReferenceMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MultipartEndpointDataReferenceRequestSenderTest {

    private MultipartEndpointDataReferenceRequestSender sender;
    private SenderDelegateContext senderContext;
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        var connectorId = IdsId.from("urn:connector:edc").getContent();
        var transformerRegistry = mock(IdsTransformerRegistry.class);
        var idsWebhookAddress = UUID.randomUUID() + "/api/v1/ids/data";

        var typeManager = new TypeManager();
        mapper = IdsTypeManagerUtil.getIdsObjectMapper(typeManager);

        senderContext = new SenderDelegateContext(connectorId, mapper, transformerRegistry, idsWebhookAddress);
        sender = new MultipartEndpointDataReferenceRequestSender(senderContext, typeManager);
    }

    @Test
    void buildMessageHeader() {
        var request = createEdrRequest();
        var datToken = createDatToken();

        var header = sender.buildMessageHeader(request, datToken);

        assertThat(header).isInstanceOf(ParticipantUpdateMessage.class);
        var participantUpdateMessage = (ParticipantUpdateMessage) header;
        assertThat(participantUpdateMessage.getModelVersion()).isEqualTo(IdsConstants.INFORMATION_MODEL_VERSION);
        assertThat(participantUpdateMessage.getSecurityToken()).isEqualTo(datToken);
        assertThat(participantUpdateMessage.getIssuerConnector()).isEqualTo(senderContext.getConnectorId().toUri());
        assertThat(participantUpdateMessage.getSenderAgent()).isEqualTo(senderContext.getConnectorId().toUri());
        assertThat(participantUpdateMessage.getRecipientAgent()).allMatch(uri -> uri.equals(URI.create(request.getConnectorId())));
    }

    @Test
    void buildMessagePayload() throws Exception {
        var request = createEdrRequest();
        var payload = sender.buildMessagePayload(request);

        var edr = mapper.readValue(payload, EndpointDataReference.class);
        assertThat(edr.getAuthCode()).isEqualTo(request.getEndpointDataReference().getAuthCode());
        assertThat(edr.getId()).isEqualTo(request.getEndpointDataReference().getId());
        assertThat(edr.getProperties()).isEqualTo(request.getEndpointDataReference().getProperties());
        assertThat(edr.getEndpoint()).isEqualTo(request.getEndpointDataReference().getEndpoint());
    }

    @Test
    void getResponseContent() throws Exception {
        var header = new NotificationMessageBuilder()._contentVersion_(UUID.randomUUID().toString()).build();
        var payload = UUID.randomUUID().toString();
        var parts = IdsMultipartParts.Builder.newInstance()
                .header(new ByteArrayInputStream(mapper.writeValueAsBytes(header)))
                .payload(new ByteArrayInputStream(payload.getBytes()))
                .build();
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

    private DynamicAttributeToken createDatToken() {
        return new DynamicAttributeTokenBuilder()._tokenValue_(UUID.randomUUID().toString()).build();
    }

    private EndpointDataReferenceMessage createEdrRequest() {
        return EndpointDataReferenceMessage.Builder.newInstance()
                .protocol("test-protocol")
                .callbackAddress("http://consumer-connector.com")
                .connectorId(UUID.randomUUID().toString())
                .endpointDataReference(EndpointDataReference.Builder.newInstance()
                        .endpoint("http://provider-connector.com")
                        .id(UUID.randomUUID().toString())
                        .authKey("Api-Key")
                        .authCode("token-test")
                        .build())
                .build();
    }
}
