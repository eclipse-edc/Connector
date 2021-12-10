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
package org.eclipse.dataspaceconnector.ids.core.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import de.fraunhofer.iais.eis.ArtifactRequestMessageImpl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.eclipse.dataspaceconnector.ids.spi.Protocols.IDS_REST;

class DataRequestMessageSenderTest {

    public static final String DESTINATION_KEY = "dataspaceconnector-data-destination";
    public static final String PROPERTIES_KEY = "dataspaceconnector-properties";
    static Faker faker = new Faker();
    private final String connectorAddress = "http://" + faker.internet().url();
    private final String connectorId = faker.internet().url();
    private final String processId = faker.internet().uuid();
    private final ObjectMapper mapper = new ObjectMapper();


    private DataRequestMessageSender sender;
    private OkHttpClient httpClient;

    @BeforeEach
    public void setUp() {
        httpClient = niceMock(OkHttpClient.class);

        IdentityService identityService = niceMock(IdentityService.class);
        TokenResult tokenResult = niceMock(TokenResult.class);
        expect(tokenResult.getToken()).andReturn(faker.lorem().characters());
        expect(identityService.obtainClientCredentials(connectorId)).andReturn(tokenResult);
        replay(identityService, tokenResult);
        sender = new DataRequestMessageSender(connectorId, identityService, niceMock(TransferProcessStore.class), EasyMock.mock(Vault.class), httpClient, mapper, niceMock(Monitor.class));
    }

    @Test
    public void initiateIdsMessage() throws IOException {
        DataRequest dataRequest = createDataRequest();
        DataAddress dataDestination = dataRequest.getDataDestination();

        // record
        Capture<Request> requestCapture = newCapture();
        expect(httpClient.newCall(capture(requestCapture))).andReturn(niceMock(Call.class)).times(1);
        replay(httpClient);

        // invoke
        sender.send(dataRequest, () -> processId);

        //verify
        verify(httpClient);

        var artifactMessageRequest = getArtifactMessageRequest(requestCapture.getValue());
        assertThat(artifactMessageRequest.getIssuerConnector()).isEqualTo(URI.create(connectorId));
        assertThat(artifactMessageRequest.getRequestedArtifact()).isEqualTo(URI.create(dataRequest.getAssetId()));

        var properties = artifactMessageRequest.getProperties();
        assertThat((Map<String, Object>) properties.get(DESTINATION_KEY)).contains(entry("keyName", dataDestination.getKeyName()), entry("type", dataDestination.getType()));
        assertThat(properties.get(PROPERTIES_KEY)).isEqualTo(dataRequest.getProperties());

    }

    private DataRequest createDataRequest() {
        var requestProperties = Map.of(faker.internet().uuid(), faker.lorem().word(), faker.internet().uuid(), faker.lorem().word());
        var keyName = faker.lorem().word();
        var type = faker.lorem().word();
        return DataRequest.Builder.newInstance()
                .connectorId(connectorId)
                .assetId(faker.internet().url())
                .dataDestination(DataAddress.Builder.newInstance().keyName(keyName).type(type).build())
                .protocol(IDS_REST)
                .properties(requestProperties)
                .connectorAddress(connectorAddress)
                .build();
    }

    private ArtifactRequestMessageImpl getArtifactMessageRequest(Request request) throws IOException {
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        String json = buffer.readString(StandardCharsets.UTF_8);
        return mapper.readValue(json, ArtifactRequestMessageImpl.class);
    }

}