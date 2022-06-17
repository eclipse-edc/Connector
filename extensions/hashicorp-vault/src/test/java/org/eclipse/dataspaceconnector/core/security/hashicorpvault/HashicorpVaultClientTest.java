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

package org.eclipse.dataspaceconnector.core.security.hashicorpvault;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.UUID;

class HashicorpVaultClientTest {
  private static final String key = "key";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void getSecretValue() throws IOException {
    // prepare
    String vaultUrl = "https://mock.url";
    String vaultToken = UUID.randomUUID().toString();
    HashicorpVaultClientConfig hashicorpVaultClientConfig =
        HashicorpVaultClientConfig.builder().vaultUrl(vaultUrl).vaultToken(vaultToken).build();

    OkHttpClient okHttpClient = Mockito.mock(OkHttpClient.class);
    HashicorpVaultClient vaultClient =
        new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, objectMapper);
    Call call = Mockito.mock(Call.class);
    Response response = Mockito.mock(Response.class);
    ResponseBody body = Mockito.mock(ResponseBody.class);
    HashicorpVaultGetEntryResponsePayload payload = new HashicorpVaultGetEntryResponsePayload();

    Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
    Mockito.when(call.execute()).thenReturn(response);
    Mockito.when(response.code()).thenReturn(200);
    Mockito.when(response.body()).thenReturn(body);
    Mockito.when(body.string()).thenReturn(payload.toString());

    // invoke
    Result<String> result = vaultClient.getSecretValue(key);

    // verify
    Assertions.assertNotNull(result);
    Mockito.verify(okHttpClient, Mockito.times(1))
        .newCall(
            Mockito.argThat(
                request ->
                    request.method().equalsIgnoreCase("GET")
                        && request.url().encodedPath().contains("/v1/secret/data")
                        && request.url().encodedPathSegments().contains(key)));
  }

  @Test
  void setSecretValue() throws IOException {
    // prepare
    String vaultUrl = "https://mock.url";
    String vaultToken = UUID.randomUUID().toString();
    String secretValue = UUID.randomUUID().toString();
    HashicorpVaultClientConfig hashicorpVaultClientConfig =
        HashicorpVaultClientConfig.builder().vaultUrl(vaultUrl).vaultToken(vaultToken).build();

    OkHttpClient okHttpClient = Mockito.mock(OkHttpClient.class);
    HashicorpVaultClient vaultClient =
        new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, objectMapper);
    HashicorpVaultCreateEntryResponsePayload payload =
        new HashicorpVaultCreateEntryResponsePayload();

    Call call = Mockito.mock(Call.class);
    Response response = Mockito.mock(Response.class);
    ResponseBody body = Mockito.mock(ResponseBody.class);

    Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
    Mockito.when(call.execute()).thenReturn(response);
    Mockito.when(response.code()).thenReturn(200);
    Mockito.when(response.body()).thenReturn(body);
    Mockito.when(body.string()).thenReturn(payload.toString());

    // invoke
    Result<HashicorpVaultCreateEntryResponsePayload> result =
        vaultClient.setSecret(key, secretValue);

    // verify
    Assertions.assertNotNull(result);
    Mockito.verify(okHttpClient, Mockito.times(1))
        .newCall(
            Mockito.argThat(
                request ->
                    request.method().equalsIgnoreCase("POST")
                        && request.url().encodedPath().contains("/v1/secret/data")
                        && request.url().encodedPathSegments().contains(key)));
  }

  @Test
  void destroySecretValue() throws IOException {
    // prepare
    String vaultUrl = "https://mock.url";
    String vaultToken = UUID.randomUUID().toString();
    HashicorpVaultClientConfig hashicorpVaultClientConfig =
        HashicorpVaultClientConfig.builder().vaultUrl(vaultUrl).vaultToken(vaultToken).build();

    OkHttpClient okHttpClient = Mockito.mock(OkHttpClient.class);
    HashicorpVaultClient vaultClient =
        new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, objectMapper);

    Call call = Mockito.mock(Call.class);
    Response response = Mockito.mock(Response.class);
    ResponseBody body = Mockito.mock(ResponseBody.class);
    Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
    Mockito.when(call.execute()).thenReturn(response);
    Mockito.when(response.code()).thenReturn(200);
    Mockito.when(response.body()).thenReturn(body);

    // invoke
    Result<Void> result = vaultClient.destroySecret(key);

    // verify
    Assertions.assertNotNull(result);
    Mockito.verify(okHttpClient, Mockito.times(1))
        .newCall(
            Mockito.argThat(
                request ->
                    request.method().equalsIgnoreCase("DELETE")
                        && request.url().encodedPath().contains("/v1/secret/metadata")
                        && request.url().encodedPathSegments().contains(key)));
  }
}
