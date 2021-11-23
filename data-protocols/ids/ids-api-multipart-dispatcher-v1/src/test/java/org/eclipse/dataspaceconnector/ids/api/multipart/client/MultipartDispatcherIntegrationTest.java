/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import de.fraunhofer.iais.eis.RejectionMessage;
import okhttp3.OkHttpClient;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.api.multipart.client.sender.MultipartArtifactRequestSender;
import org.eclipse.dataspaceconnector.ids.api.multipart.client.sender.MultipartDescriptionRequestSender;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultipartDispatcherIntegrationTest extends AbstractMultipartDispatcherIntegrationTest {
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private MultipartDescriptionRequestSender descriptionRequestSender;
    private MultipartArtifactRequestSender artifactRequestSender;
    private TransformerRegistry transformerRegistry;

    @Override
    protected Map<String, String> getSystemProperties() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(getPort()));
                put("edc.ids.id", "urn:connector:" + CONNECTOR_ID);
            }
        };
    }

    @BeforeEach
    void init() {
        Monitor monitor = EasyMock.createNiceMock(Monitor.class);
        EasyMock.replay(monitor);
        transformerRegistry = EasyMock.createNiceMock(TransformerRegistry.class);
        EasyMock.replay(transformerRegistry);
        var httpClient = new OkHttpClient.Builder().build();
        descriptionRequestSender = new MultipartDescriptionRequestSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService);
        artifactRequestSender = new MultipartArtifactRequestSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService, transformerRegistry);
    }

    @Test
    void testSendDescriptionRequestMessage() throws Exception {
        var request = MetadataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .build();
        var result = descriptionRequestSender.send(request, null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();
        assertThat(result.getHeader()).isInstanceOf(DescriptionResponseMessage.class);
        assertThat(result.getPayload()).isNotNull();
        assertThat(result.getPayload()).isInstanceOf(BaseConnector.class);
    }

    @Test
    void testSendArtifactRequestMessage() throws Exception {
        var asset = (Asset) EasyMock.createNiceMock(Asset.class);
        EasyMock.expect(asset.getId()).andReturn("assetId");
        EasyMock.replay(asset);

        var dataDestination = (DataAddress) EasyMock.createNiceMock(DataAddress.class);
        EasyMock.replay(dataDestination);

        EasyMock.expect(transformerRegistry.transform(EasyMock.anyObject(), EasyMock.anyObject()))
                .andReturn(new TransformResult<>(URI.create("artifactId")));
        EasyMock.replay(transformerRegistry);

        var request = DataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .asset(asset)
                .dataDestination(dataDestination)
                .build();
        var result = artifactRequestSender.send(request, null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();

        //TODO revise when handler for ArtifactRequestMessage exists
        assertThat(result.getHeader()).isInstanceOf(RejectionMessage.class);
        assertThat(result.getPayload()).isNull();
    }

}
