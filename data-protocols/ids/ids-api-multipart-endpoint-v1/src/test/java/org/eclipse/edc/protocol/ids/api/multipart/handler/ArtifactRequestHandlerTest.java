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
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.ids.api.multipart.message.MultipartRequest;
import org.eclipse.edc.protocol.ids.serialization.IdsTypeManagerUtil;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.spi.types.container.ArtifactRequestMessagePayload;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArtifactRequestHandlerTest {

    private ArtifactRequestHandler handler;

    private TransferProcessService transferProcessService;
    private IdsId connectorId;
    private ContractNegotiationStore contractNegotiationStore;

    private static URI createUri(IdsType type, String value) {
        return URI.create("urn:" + type.getValue() + ":" + value);
    }

    private static ContractAgreement createContractAgreement(String contractId, String assetId) {
        return ContractAgreement.Builder.newInstance()
                .id(contractId)
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .assetId(assetId)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    @BeforeEach
    public void setUp() {
        transferProcessService = mock(TransferProcessService.class);
        connectorId = IdsId.from("urn:connector:" + UUID.randomUUID()).getContent();
        Monitor monitor = mock(Monitor.class);
        contractNegotiationStore = mock(ContractNegotiationStore.class);
        Vault vault = mock(Vault.class);

        handler = new ArtifactRequestHandler(monitor, connectorId, getCustomizedObjectMapper(), contractNegotiationStore, vault, transferProcessService);
    }

    @Test
    void handleRequestOkTest() throws JsonProcessingException {
        var assetId = UUID.randomUUID().toString();
        var artifactRequestId = UUID.randomUUID().toString();
        var contractId = UUID.randomUUID().toString();
        var destination = DataAddress.Builder.newInstance().keyName(UUID.randomUUID().toString()).type("test").build();
        var agreement = createContractAgreement(contractId, assetId);
        var claimToken = ClaimToken.Builder.newInstance().build();
        var multipartRequest = createMultipartRequest(destination, artifactRequestId, assetId, contractId, claimToken);
        var header = (ArtifactRequestMessage) multipartRequest.getHeader();

        var drCapture = ArgumentCaptor.forClass(DataRequest.class);
        when(transferProcessService.initiateTransfer(drCapture.capture(), eq(claimToken))).thenReturn(ServiceResult.success("Transfer success"));
        when(contractNegotiationStore.findContractAgreement(contractId)).thenReturn(agreement);

        handler.handleRequest(multipartRequest);

        verify(transferProcessService).initiateTransfer(drCapture.capture(), eq(claimToken));

        assertThat(drCapture.getValue().getId()).hasToString(artifactRequestId);
        assertThat(drCapture.getValue().getDataDestination().getKeyName()).isEqualTo(destination.getKeyName());
        assertThat(drCapture.getValue().getConnectorId()).isEqualTo(connectorId.toString());
        assertThat(drCapture.getValue().getAssetId()).isEqualTo(agreement.getAssetId());
        assertThat(drCapture.getValue().getContractId()).isEqualTo(agreement.getId());
        assertThat(drCapture.getValue().getConnectorAddress()).isEqualTo(header.getProperties().get(IDS_WEBHOOK_ADDRESS_PROPERTY).toString());
        assertThat(drCapture.getValue().getProperties()).containsExactlyEntriesOf(Map.of("foo", "bar"));
    }

    @Test
    @DisplayName("Verifies that a contract is not passed with a separate id")
    void verifyIllegalArtifactIdRequestTest() throws JsonProcessingException {
        var artifactId = "assetIdNoSpecifiedInContract";
        var artifactRequestId = UUID.randomUUID().toString();
        var contractId = UUID.randomUUID().toString();
        var destination = DataAddress.Builder.newInstance().keyName(UUID.randomUUID().toString()).type("test").build();
        var claimToken = ClaimToken.Builder.newInstance().build();
        var multipartRequest = createMultipartRequest(destination, artifactRequestId, artifactId, contractId, claimToken);

        // Create the contract using a different asset id
        var agreement = createContractAgreement(contractId, UUID.randomUUID().toString());

        when(contractNegotiationStore.findContractAgreement(contractId)).thenReturn(agreement);

        var response = handler.handleRequest(multipartRequest);

        // Verify the request is rejected as the client sent a contract id with a different asset id
        verifyNoInteractions(transferProcessService);
        assertThat(response).isNotNull();
        assertThat(response.getHeader()).isInstanceOf(RejectionMessage.class);

    }

    private MultipartRequest createMultipartRequest(DataAddress dataDestination, String artifactRequestId, String artifactId, String contractId, ClaimToken claimToken) throws JsonProcessingException {
        var message = new ArtifactRequestMessageBuilder(URI.create(artifactRequestId))
                ._modelVersion_(IdsConstants.INFORMATION_MODEL_VERSION)
                ._securityToken_(new DynamicAttributeTokenBuilder()._tokenValue_(UUID.randomUUID().toString()).build())
                ._requestedArtifact_(createUri(IdsType.ARTIFACT, artifactId))
                ._transferContract_(createUri(IdsType.CONTRACT_AGREEMENT, contractId))
                .build();
        message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, "http://example.com/api/v1/ids/data");
        message.setProperty("foo", "bar");

        var payload = ArtifactRequestMessagePayload.Builder.newInstance()
                .dataDestination(dataDestination)
                .build();
        return MultipartRequest.Builder.newInstance()
                .header(message)
                .payload(new ObjectMapper().writeValueAsString(payload))
                .claimToken(claimToken)
                .build();
    }

    private ObjectMapper getCustomizedObjectMapper() {
        return IdsTypeManagerUtil.getIdsObjectMapper(new TypeManager());
    }
}
