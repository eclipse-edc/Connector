/*
 *  Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Catena-X Consortium - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.ids.api.transfer;

import com.github.javafaker.Faker;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.ArtifactRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import org.eclipse.dataspaceconnector.ids.spi.daps.DapsService;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyService;
import org.eclipse.dataspaceconnector.policy.engine.PolicyEvaluationResult;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResult;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.ids.spi.Protocols.IDS_REST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArtifactRequestControllerTest {

    public static final String DESTINATION_KEY = "dataspaceconnector-data-destination";
    public static final String PROPERTIES_KEY = "dataspaceconnector-properties";
    static Faker faker = new Faker();
    private final String consumerConnectorAddress = faker.internet().url();
    private final String artifactMessageId = faker.internet().uuid();
    private final String assetId = faker.internet().url();
    private ArtifactRequestController controller;
    private TransferProcessManager manager;

    @BeforeEach
    public void setUp() {
        manager = mock(TransferProcessManager.class);
    }

    @Test
    public void initiateDataRequest() {
        DapsService dapsService = mock(DapsService.class);
        Result<ClaimToken> verificationResult = Result.success(ClaimToken.Builder.newInstance().build());
        AssetIndex assetIndex = mock(AssetIndex.class);
        var asset = Asset.Builder.newInstance().id(assetId).build();
        PolicyRegistry policyRegistry = mock(PolicyRegistry.class);
        IdsPolicyService policyService = mock(IdsPolicyService.class);
        PolicyEvaluationResult policyEvaluationResult = mock(PolicyEvaluationResult.class);

        when(dapsService.verifyAndConvertToken(anyString())).thenReturn(verificationResult);
        when(assetIndex.findById(assetId)).thenReturn(asset);
        when(policyRegistry.resolvePolicy(any())).thenReturn(mock(Policy.class));
        when(policyEvaluationResult.valid()).thenReturn(true);
        when(policyService.evaluateRequest(eq(consumerConnectorAddress), eq(artifactMessageId), any(), any())).thenReturn(policyEvaluationResult);

        controller = new ArtifactRequestController(dapsService, assetIndex, manager, policyService, policyRegistry, mock(Vault.class), mock(Monitor.class));

        var requestProperties = Map.of(faker.lorem().word(), faker.lorem().word(), faker.lorem().word(), faker.lorem().word());
        var type = faker.lorem().word();
        var secretName = faker.lorem().word();
        var destinationMap = Map.of("type", type, "keyName", secretName, "properties", Map.of());
        ArtifactRequestMessage artifactRequestMessage = createArtifactRequestMessage(requestProperties, destinationMap);
        var requestCapture = ArgumentCaptor.forClass(DataRequest.class);
        when(manager.initiateProviderRequest(requestCapture.capture())).thenReturn(TransferInitiateResult.success("processId"));

        controller.request(artifactRequestMessage);

        DataRequest dataRequest = requestCapture.getValue();
        assertThat(dataRequest.getAssetId()).isEqualTo(assetId);
        assertThat(dataRequest.getProperties()).containsAllEntriesOf(requestProperties);
        assertThat(dataRequest.getProtocol()).isEqualTo(IDS_REST);
        assertThat(dataRequest.getDataDestination().getKeyName()).isEqualTo(secretName);
        assertThat(dataRequest.getDataDestination().getType()).isEqualTo(type);
        assertThat(dataRequest.getDataDestination().getProperties()).isEqualTo(Map.of("type", type, "keyName", secretName));
        verify(dapsService).verifyAndConvertToken(anyString());
        verify(assetIndex).findById(assetId);
        verify(policyRegistry).resolvePolicy(any());
        verify(policyEvaluationResult).valid();
        verify(policyService).evaluateRequest(eq(consumerConnectorAddress), eq(artifactMessageId), any(), any());
        verify(manager).initiateProviderRequest(requestCapture.capture());
    }

    @NotNull
    private ArtifactRequestMessage createArtifactRequestMessage(Map<String, String> additionalProperties, Map<String, Object> destinationMap) {
        ArtifactRequestMessage artifactRequestMessage = new ArtifactRequestMessageBuilder(URI.create(artifactMessageId))
                ._securityToken_(new DynamicAttributeTokenBuilder()
                        ._tokenValue_(faker.lorem().word())
                        .build())
                ._requestedArtifact_(URI.create(assetId))
                ._issuerConnector_(URI.create(consumerConnectorAddress))
                .build();

        artifactRequestMessage.setProperty(DESTINATION_KEY, destinationMap);
        artifactRequestMessage.setProperty(PROPERTIES_KEY, additionalProperties);
        return artifactRequestMessage;
    }

}