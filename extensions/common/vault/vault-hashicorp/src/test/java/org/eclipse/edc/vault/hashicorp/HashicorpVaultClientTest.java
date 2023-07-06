/*
 *  Copyright (c) 2022 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Initial Test
 *
 */

package org.eclipse.edc.vault.hashicorp;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.vault.hashicorp.model.CreateEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.GetEntryResponsePayload;
import org.eclipse.edc.vault.hashicorp.model.HealthResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HashicorpVaultClientTest {
    private static final String KEY = "key";
    private static final String CUSTOM_SECRET_PATH = "v1/test/secret";
    private static final String HEALTH_PATH = "sys/health";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EdcHttpClient edcClientMock = mock(EdcHttpClient.class);

    @BeforeEach
    void setup() {

    }

    @Test
    void getSecretValue() throws IOException {
        // prepare
        var vaultUrl = "https://mock.url";
        var vaultToken = UUID.randomUUID().toString();
        var config =
                HashicorpVaultClientConfig.Builder.newInstance()
                        .vaultUrl(vaultUrl)
                        .vaultApiSecretPath(CUSTOM_SECRET_PATH)
                        .vaultApiHealthPath(HEALTH_PATH)
                        .isVaultApiHealthStandbyOk(false)
                        .vaultToken(vaultToken)
                        .timeout(TIMEOUT)
                        .build();


        var vaultClient = new HashicorpVaultClient(config, edcClientMock, OBJECT_MAPPER);
        var response = mock(Response.class);
        var body = mock(ResponseBody.class);
        var payload = new GetEntryResponsePayload();

        when(edcClientMock.execute(any(Request.class))).thenReturn(response);
        when(response.code()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(body.string()).thenReturn(payload.toString());

        // invoke
        var result = vaultClient.getSecretValue(KEY);

        // verify
        assertNotNull(result);
        verify(edcClientMock).execute(argThat(request -> request.method().equalsIgnoreCase("GET") &&
                request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                request.url().encodedPathSegments().contains(KEY)));
    }

    @Test
    void setSecretValue() throws IOException {
        // prepare
        var vaultUrl = "https://mock.url";
        var vaultToken = UUID.randomUUID().toString();
        var secretValue = UUID.randomUUID().toString();
        var hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.Builder.newInstance()
                        .vaultUrl(vaultUrl)
                        .vaultApiSecretPath(CUSTOM_SECRET_PATH)
                        .vaultApiHealthPath(HEALTH_PATH)
                        .isVaultApiHealthStandbyOk(false)
                        .vaultToken(vaultToken)
                        .timeout(TIMEOUT)
                        .build();

        var vaultClient = new HashicorpVaultClient(hashicorpVaultClientConfig, edcClientMock, OBJECT_MAPPER);
        var payload = new CreateEntryResponsePayload();

        var call = mock(Call.class);
        var response = mock(Response.class);
        var body = mock(ResponseBody.class);

        when(edcClientMock.execute(any(Request.class))).thenReturn(response);
        when(call.execute()).thenReturn(response);
        when(response.code()).thenReturn(200);
        when(response.body()).thenReturn(body);
        when(body.string()).thenReturn(payload.toString());

        // invoke
        var result = vaultClient.setSecret(KEY, secretValue);

        // verify
        assertNotNull(result);
        verify(edcClientMock).execute(argThat(request -> request.method().equalsIgnoreCase("POST") &&
                request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/data") &&
                request.url().encodedPathSegments().contains(KEY)));
    }

    @Test
    void getHealth() throws IOException {
        // prepare
        var vaultUrl = "https://mock.url";
        var vaultToken = UUID.randomUUID().toString();
        var hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.Builder.newInstance()
                        .vaultUrl(vaultUrl)
                        .vaultApiSecretPath(CUSTOM_SECRET_PATH)
                        .vaultApiHealthPath(HEALTH_PATH)
                        .isVaultApiHealthStandbyOk(false)
                        .vaultToken(vaultToken)
                        .timeout(TIMEOUT)
                        .build();

        var vaultClient =
                new HashicorpVaultClient(hashicorpVaultClientConfig, edcClientMock, OBJECT_MAPPER);

        var response = mock(Response.class);
        var body = mock(ResponseBody.class);

        when(edcClientMock.execute(any(Request.class))).thenReturn(response);
        when(response.code()).thenReturn(200);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(body);
        when(body.string())
                .thenReturn(
                        "{ " +
                                "\"initialized\": true, " +
                                "\"sealed\": false," +
                                "\"standby\": false," +
                                "\"performance_standby\": false," +
                                "\"replication_performance_mode\": \"mode\"," +
                                "\"replication_dr_mode\": \"mode\"," +
                                "\"server_time_utc\": 100," +
                                "\"version\": \"1.0.0\"," +
                                "\"cluster_name\": \"name\"," +
                                "\"cluster_id\": \"id\" " +
                                " }");

        // invoke
        var result = vaultClient.getHealth();

        // verify
        assertNotNull(result);
        verify(edcClientMock).execute(
                argThat(request -> request.method().equalsIgnoreCase("GET") &&
                        request.url().encodedPath().contains(HEALTH_PATH) &&
                        request.url().queryParameter("standbyok").equals("false") &&
                        request.url().queryParameter("perfstandbyok").equals("false")));
        assertEquals(200, result.getCode());
        assertEquals(
                HealthResponse.HashiCorpVaultHealthResponseCode
                        .INITIALIZED_UNSEALED_AND_ACTIVE,
                result.getCodeAsEnum());

        var resultPayload = result.getPayload();

        assertNotNull(resultPayload);
        Assertions.assertTrue(resultPayload.isInitialized());
        Assertions.assertFalse(resultPayload.isSealed());
        Assertions.assertFalse(resultPayload.isStandby());
        Assertions.assertFalse(resultPayload.isPerformanceStandby());
        assertEquals("mode", resultPayload.getReplicationPerformanceMode());
        assertEquals("mode", resultPayload.getReplicationDrMode());
        assertEquals(100, resultPayload.getServerTimeUtc());
        assertEquals("1.0.0", resultPayload.getVersion());
        assertEquals("id", resultPayload.getClusterId());
        assertEquals("name", resultPayload.getClusterName());
    }

    @Test
    void destroySecretValue() throws IOException {
        // prepare
        var vaultUrl = "https://mock.url";
        var vaultToken = UUID.randomUUID().toString();
        var hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.Builder.newInstance()
                        .vaultUrl(vaultUrl)
                        .vaultApiSecretPath(CUSTOM_SECRET_PATH)
                        .vaultApiHealthPath(HEALTH_PATH)
                        .isVaultApiHealthStandbyOk(false)
                        .vaultToken(vaultToken)
                        .timeout(TIMEOUT)
                        .build();

        var vaultClient = new HashicorpVaultClient(hashicorpVaultClientConfig, edcClientMock, OBJECT_MAPPER);

        var response = mock(Response.class);
        var body = mock(ResponseBody.class);
        when(edcClientMock.execute(any(Request.class))).thenReturn(response);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(body);

        // invoke
        var result = vaultClient.destroySecret(KEY);

        // verify
        assertThat(result).isNotNull();
        assertThat(result.succeeded()).isTrue();
        verify(edcClientMock).execute(argThat(request -> request.method().equalsIgnoreCase("DELETE") &&
                        request.url().encodedPath().contains(CUSTOM_SECRET_PATH + "/metadata")
                /*request.url().encodedPathSegments().contains(KEY)*/));
    }
}
