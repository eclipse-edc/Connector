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
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.ids.spi.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultipartArtifactRequestSenderTest {

    private MultipartArtifactRequestSender sender;
    private String idsWebhookAddress;
    private TransformerRegistry transformerRegistry;

    @BeforeEach
    public void setUp() {
        var httpClient = mock(OkHttpClient.class);
        var mapper = new ObjectMapper();
        var monitor = mock(Monitor.class);
        var vault = mock(Vault.class);
        var identityService = mock(IdentityService.class);
        String connectorId = UUID.randomUUID().toString();
        transformerRegistry = mock(TransformerRegistry.class);
        idsWebhookAddress = UUID.randomUUID().toString();
        sender = new MultipartArtifactRequestSender(connectorId, httpClient, mapper, monitor, vault, identityService, transformerRegistry, idsWebhookAddress);
    }

    @Test
    void buildMessageHeaderOkTest() {
        var token = new DynamicAttributeTokenBuilder()._tokenValue_(UUID.randomUUID().toString()).build();
        var request = createRequest();
        when(transformerRegistry.transform(any(IdsId.class), eq(URI.class))).thenAnswer(invocation -> {
            IdsId id = invocation.getArgument(0);
            return Result.success(URI.create(id.getValue()));
        });

        var message = sender.buildMessageHeader(request, token);

        assertThat(message).isInstanceOf(ArtifactRequestMessage.class);
        assertThat((ArtifactRequestMessage) message)
                .satisfies(msg -> {
                    assertThat(msg.getId()).hasToString(request.getId());
                    assertThat(msg.getModelVersion()).isEqualTo(IdsProtocol.INFORMATION_MODEL_VERSION);
                    assertThat(msg.getSecurityToken()).isEqualTo(token);
                    assertThat(msg.getIssuerConnector()).isEqualTo(sender.getConnectorId());
                    assertThat(msg.getSenderAgent()).isEqualTo(sender.getConnectorId());
                    assertThat(msg.getRecipientConnector()).containsExactly(URI.create(request.getConnectorId()));
                    assertThat(msg.getRequestedArtifact().compareTo(URI.create(request.getAssetId()))).isZero();
                    assertThat(msg.getTransferContract().compareTo(URI.create(request.getContractId()))).isZero();
                    assertThat(msg.getProperties())
                            .hasSize(request.getProperties().size() + 1)
                            .containsAllEntriesOf(request.getProperties())
                            .containsEntry(IDS_WEBHOOK_ADDRESS_PROPERTY, idsWebhookAddress + "/api/v1/ids/data");
                });
    }

    private static DataRequest createRequest() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance().type("test").build())
                .connectorId("connector-test")
                .properties(Map.of("foo", "bar", "hello", "world"))
                .build();
    }
}
