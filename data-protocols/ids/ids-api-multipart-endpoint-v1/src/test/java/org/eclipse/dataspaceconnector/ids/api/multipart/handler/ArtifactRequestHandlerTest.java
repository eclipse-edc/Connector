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
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartRequest;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.spec.extension.ArtifactRequestMessagePayload;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.validation.ContractValidationService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.ids.spi.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtifactRequestHandlerTest {

    private ArtifactRequestHandler handler;

    private TransferProcessManager transferProcessManager;
    private String connectorId;
    private ContractValidationService contractValidationService;
    private ContractNegotiationStore contractNegotiationStore;

    @BeforeEach
    public void setUp() {
        transferProcessManager = mock(TransferProcessManager.class);
        connectorId = UUID.randomUUID().toString();
        Monitor monitor = mock(Monitor.class);
        var mapper = new ObjectMapper();
        contractValidationService = mock(ContractValidationService.class);
        contractNegotiationStore = mock(ContractNegotiationStore.class);
        Vault vault = mock(Vault.class);
        handler = new ArtifactRequestHandler(monitor, connectorId, mapper, contractNegotiationStore, contractValidationService, transferProcessManager, vault);
    }

    @Test
    void handleRequestOkTest() throws JsonProcessingException {
        var artifactId = UUID.randomUUID().toString();
        var artifactRequestId = UUID.randomUUID().toString();
        var contractId = UUID.randomUUID().toString();
        var destination = DataAddress.Builder.newInstance().keyName(UUID.randomUUID().toString()).type("test").build();
        var multipartRequest = createMultipartRequest(destination, artifactRequestId, artifactId, contractId);
        var header = (ArtifactRequestMessage) multipartRequest.getHeader();
        var agreement = createContractAgreement();
        var claimToken = ClaimToken.Builder.newInstance().build();

        var drCapture = ArgumentCaptor.forClass(DataRequest.class);
        when(transferProcessManager.initiateProviderRequest(drCapture.capture())).thenReturn(TransferInitiateResult.success("Transfer success"));
        when(contractNegotiationStore.findContractAgreement(contractId)).thenReturn(agreement);
        when(contractValidationService.validate(claimToken, agreement)).thenReturn(true);

        handler.handleRequest(multipartRequest, claimToken);

        verify(transferProcessManager).initiateProviderRequest(drCapture.capture());

        assertThat(drCapture.getValue().getId()).hasToString(artifactRequestId);
        assertThat(drCapture.getValue().getDataDestination().getKeyName()).isEqualTo(destination.getKeyName());
        assertThat(drCapture.getValue().getConnectorId()).isEqualTo(connectorId);
        assertThat(drCapture.getValue().getAssetId()).isEqualTo(artifactId);
        assertThat(drCapture.getValue().getContractId()).isEqualTo(contractId);
        assertThat(drCapture.getValue().getConnectorAddress()).isEqualTo(header.getProperties().get(IDS_WEBHOOK_ADDRESS_PROPERTY).toString());
        assertThat(drCapture.getValue().getProperties()).containsExactlyEntriesOf(Map.of("foo", "bar"));
    }

    private MultipartRequest createMultipartRequest(DataAddress dataDestination, String artifactRequestId, String artifactId, String contractId) throws JsonProcessingException {
        var message = new ArtifactRequestMessageBuilder(URI.create(artifactRequestId))
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._securityToken_(new DynamicAttributeTokenBuilder()._tokenValue_(UUID.randomUUID().toString()).build())
                ._requestedArtifact_(createUri(IdsType.ARTIFACT, artifactId))
                ._transferContract_(createUri(IdsType.CONTRACT, contractId))
                .build();
        message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, "http://example.com/api/v1/ids/data");
        message.setProperty("foo", "bar");

        var payload = ArtifactRequestMessagePayload.Builder.newInstance()
                .dataDestination(dataDestination)
                .build();
        return MultipartRequest.Builder.newInstance().header(message).payload(new ObjectMapper().writeValueAsString(payload)).build();
    }

    private static URI createUri(IdsType type, String value) {
        return URI.create("urn:" + type.getValue() + ":" + value);
    }

    private static ContractAgreement createContractAgreement() {
        return ContractAgreement.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }
}
