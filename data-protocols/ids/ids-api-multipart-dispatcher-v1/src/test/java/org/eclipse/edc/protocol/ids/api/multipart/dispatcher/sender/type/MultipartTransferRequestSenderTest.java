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

import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.SenderDelegateContext;
import org.eclipse.edc.protocol.ids.serialization.IdsTypeManagerUtil;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;
import static org.mockito.Mockito.mock;

class MultipartTransferRequestSenderTest {

    private MultipartTransferRequestSender sender;
    private SenderDelegateContext senderContext;
    private String idsWebhookAddress;

    @BeforeEach
    public void setUp() {
        var vault = mock(Vault.class);
        var connectorId = IdsId.from("urn:connector:edc").getContent();
        var transformerRegistry = mock(TypeTransformerRegistry.class);
        idsWebhookAddress = UUID.randomUUID() + "/api/v1/ids/data";

        var objectMapper = IdsTypeManagerUtil.getIdsObjectMapper(new TypeManager());

        senderContext = new SenderDelegateContext(connectorId, objectMapper, transformerRegistry, idsWebhookAddress);
        sender = new MultipartTransferRequestSender(senderContext, vault);
    }

    @Test
    void buildMessageHeaderOkTest() {
        var token = new DynamicAttributeTokenBuilder()._tokenValue_(UUID.randomUUID().toString()).build();
        var request = createRequest();
        var artifactId = IdsId.Builder.newInstance().value(request.getAssetId()).type(IdsType.ARTIFACT).build().toUri();
        var contractId = IdsId.Builder.newInstance().value(request.getContractId()).type(IdsType.CONTRACT_AGREEMENT).build().toUri();

        var message = sender.buildMessageHeader(request, token);

        assertThat(message).isInstanceOf(ArtifactRequestMessage.class);
        assertThat(message.getId()).hasToString(request.getId());
        assertThat(message.getModelVersion()).isEqualTo(IdsConstants.INFORMATION_MODEL_VERSION);
        assertThat(message.getSecurityToken()).isEqualTo(token);
        assertThat(message.getIssuerConnector()).isEqualTo(senderContext.getConnectorId().toUri());
        assertThat(message.getSenderAgent()).isEqualTo(senderContext.getConnectorId().toUri());
        assertThat(message.getRecipientConnector()).containsExactly(URI.create(request.getConnectorId()));
        assertThat(((ArtifactRequestMessage) message).getRequestedArtifact().compareTo(artifactId)).isZero();
        assertThat(message.getTransferContract().compareTo(contractId)).isZero();
        assertThat(message.getProperties())
                .hasSize(request.getProperties().size() + 1)
                .containsAllEntriesOf(request.getProperties())
                .containsEntry(IDS_WEBHOOK_ADDRESS_PROPERTY, idsWebhookAddress);
    }

    @Test
    void buildMessagePayload() throws Exception {
        var request = createRequest();

        var result = sender.buildMessagePayload(request);

        assertThat(result).isNotNull();
    }

    private static TransferRequestMessage createRequest() {
        return TransferRequestMessage.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance().type("test").build())
                .connectorId("connector-test")
                .properties(Map.of("foo", "bar", "hello", "world"))
                .protocol("ids-multipart")
                .callbackAddress("http://any")
                .counterPartyAddress("http://any")
                .build();
    }
}
