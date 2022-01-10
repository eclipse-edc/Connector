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
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Map.entry;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.ids.spi.Protocols.IDS_REST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataRequestMessageSenderTest {

    public static final String DESTINATION_KEY = "dataspaceconnector-data-destination";
    public static final String PROPERTIES_KEY = "dataspaceconnector-properties";
    static Faker faker = new Faker();
    private final String connectorAddress = "http://" + faker.internet().url();
    private final String connectorId = faker.internet().url();
    private final String processId = faker.internet().uuid();
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient httpClient = mock(OkHttpClient.class);
    private final IdentityService identityService = mock(IdentityService.class);

    private DataRequestMessageSender sender;

    @BeforeEach
    public void setUp() {
        sender = new DataRequestMessageSender(connectorId, identityService, mock(Vault.class), httpClient, mapper, mock(Monitor.class), mock(TransferProcessManager.class));
    }

    @Test
    public void initiateIdsMessage() throws IOException {
        var tokenRepresentation = TokenRepresentation.Builder.newInstance().token(faker.lorem().characters()).build();
        when(identityService.obtainClientCredentials(connectorId)).thenReturn(Result.success(tokenRepresentation));
        DataRequest dataRequest = createDataRequest();
        DataAddress dataDestination = dataRequest.getDataDestination();

        var requestCapture = ArgumentCaptor.forClass(Request.class);
        when(httpClient.newCall(requestCapture.capture())).thenReturn(mock(Call.class));

        sender.send(dataRequest, () -> processId);

        var artifactMessageRequest = getArtifactMessageRequest(requestCapture.getValue());
        assertThat(artifactMessageRequest.getIssuerConnector()).isEqualTo(URI.create(connectorId));
        assertThat(artifactMessageRequest.getRequestedArtifact()).isEqualTo(URI.create(dataRequest.getAssetId()));

        var properties = artifactMessageRequest.getProperties();
        assertThat((Map<String, Object>) properties.get(DESTINATION_KEY)).contains(entry("keyName", dataDestination.getKeyName()), entry("type", dataDestination.getType()));
        assertThat(properties.get(PROPERTIES_KEY)).isEqualTo(dataRequest.getProperties());
        verify(httpClient).newCall(requestCapture.capture());
    }

    @Test
    void should_return_failed_future_if_client_credentials_retrieval_fails() {
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.failure("error"));

        var result = sender.send(createDataRequest(), () -> processId);

        assertThat(result).failsWithin(1, MILLISECONDS);
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