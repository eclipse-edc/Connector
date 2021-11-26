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
import org.easymock.Capture;
import org.eclipse.dataspaceconnector.ids.spi.daps.DapsService;
import org.eclipse.dataspaceconnector.ids.spi.policy.IdsPolicyService;
import org.eclipse.dataspaceconnector.policy.engine.PolicyEvaluationResult;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferInitiateResponse;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.eclipse.dataspaceconnector.ids.spi.Protocols.IDS_REST;

public class ArtifactRequestControllerTest {

    public static final String DESTINATION_KEY = "dataspaceconnector-data-destination";
    public static final String PROPERTIES_KEY = "dataspaceconnector-properties";
    static Faker faker = new Faker();

    private ArtifactRequestController controller;
    private TransferProcessManager manager;

    private final String consumerConnectorAddress = faker.internet().url();
    private final String artifactMessageId = faker.internet().uuid();
    private final String assetId = faker.internet().url();


    @BeforeEach
    public void setUp() {
        manager = mock(TransferProcessManager.class);

        DapsService dapsService = niceMock(DapsService.class);
        VerificationResult verificationResult = niceMock(VerificationResult.class);
        AssetIndex assetIndex = niceMock(AssetIndex.class);
        Asset asset = niceMock(Asset.class);
        PolicyRegistry policyRegistry = niceMock(PolicyRegistry.class);
        IdsPolicyService policyService = niceMock(IdsPolicyService.class);
        PolicyEvaluationResult policyEvaluationResult = niceMock(PolicyEvaluationResult.class);

        expect(dapsService.verifyAndConvertToken(anyString())).andReturn(verificationResult);
        expect(verificationResult.valid()).andReturn(true);
        expect(assetIndex.findById(assetId)).andReturn(asset);
        expect(asset.getId()).andReturn(assetId);
        expect(policyRegistry.resolvePolicy(anyObject())).andReturn(niceMock(Policy.class));
        expect(policyEvaluationResult.valid()).andReturn(true);
        expect(policyService.evaluateRequest(same(consumerConnectorAddress), same(artifactMessageId), anyObject(), anyObject())).andReturn(policyEvaluationResult);

        replay(dapsService, verificationResult, assetIndex, asset, policyRegistry, policyService, policyEvaluationResult);

        controller = new ArtifactRequestController(dapsService, assetIndex, manager, policyService, policyRegistry, mock(Vault.class), mock(Monitor.class));
    }


    @Test
    public void initiateDataRequest() {

        // prepare
        var requestProperties = Map.of(faker.lorem().word(), faker.lorem().word(), faker.lorem().word(), faker.lorem().word());
        var type = faker.lorem().word();
        var secretName = faker.lorem().word();
        var destinationMap = Map.of("type", type, "keyName", secretName, "properties", Map.of());
        ArtifactRequestMessage artifactRequestMessage = createArtifactRequestMessage(requestProperties, destinationMap);

        // record
        Capture<DataRequest> requestCapture = newCapture();
        expect(manager.initiateProviderRequest(capture(requestCapture))).andReturn(TransferInitiateResponse.Builder.newInstance().build()).times(1);
        replay(manager);

        // invoke
        controller.request(artifactRequestMessage);

        // verify
        DataRequest dataRequest = requestCapture.getValue();
        assertThat(dataRequest.getAssetId()).isEqualTo(assetId);
        assertThat(dataRequest.getProperties()).isEqualTo(requestProperties);
        assertThat(dataRequest.getProtocol()).isEqualTo(IDS_REST);
        assertThat(dataRequest.getDataDestination().getKeyName()).isEqualTo(secretName);
        assertThat(dataRequest.getDataDestination().getType()).isEqualTo(type);
        assertThat(dataRequest.getDataDestination().getProperties()).isEqualTo(Map.of("type", type, "keyName", secretName));
        verify(manager);
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