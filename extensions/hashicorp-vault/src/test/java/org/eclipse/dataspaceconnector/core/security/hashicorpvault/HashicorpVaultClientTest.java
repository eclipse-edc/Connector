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

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.UUID;

class HashicorpVaultClientTest {
    private static final String KEY = "key";
    private static final TypeManager TYPE_MANAGER = new TypeManager();

    @Test
    void getSecretValue() throws IOException {
        // prepare
        var vaultUrl = "https://mock.url";
        var vaultToken = UUID.randomUUID().toString();
        var hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.Builder.newInstance().vaultUrl(vaultUrl).vaultToken(vaultToken).build();

        var okHttpClient = Mockito.mock(OkHttpClient.class);
        var vaultClient =
                new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, TYPE_MANAGER);
        var call = Mockito.mock(Call.class);
        var response = Mockito.mock(Response.class);
        var body = Mockito.mock(ResponseBody.class);
        var payload = new HashicorpVaultGetEntryResponsePayload();

        Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
        Mockito.when(call.execute()).thenReturn(response);
        Mockito.when(response.code()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(body);
        Mockito.when(body.string()).thenReturn(payload.toString());

        // invoke
        var result = vaultClient.getSecretValue(KEY);

        // verify
        Assertions.assertNotNull(result);
        Mockito.verify(okHttpClient, Mockito.times(1))
                .newCall(
                        Mockito.argThat(
                                request ->
                                        request.method().equalsIgnoreCase("GET") &&
                                                request.url().encodedPath().contains("/v1/secret/data") &&
                                                request.url().encodedPathSegments().contains(KEY)));
    }

    @Test
    void setSecretValue() throws IOException {
        // prepare
        var vaultUrl = "https://mock.url";
        var vaultToken = UUID.randomUUID().toString();
        var secretValue = UUID.randomUUID().toString();
        var hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.Builder.newInstance().vaultUrl(vaultUrl).vaultToken(vaultToken).build();

        var okHttpClient = Mockito.mock(OkHttpClient.class);
        var vaultClient =
                new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, TYPE_MANAGER);
        var payload =
                new HashicorpVaultCreateEntryResponsePayload();

        var call = Mockito.mock(Call.class);
        var response = Mockito.mock(Response.class);
        var body = Mockito.mock(ResponseBody.class);

        Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
        Mockito.when(call.execute()).thenReturn(response);
        Mockito.when(response.code()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(body);
        Mockito.when(body.string()).thenReturn(payload.toString());

        // invoke
        var result =
                vaultClient.setSecret(KEY, secretValue);

        // verify
        Assertions.assertNotNull(result);
        Mockito.verify(okHttpClient, Mockito.times(1))
                .newCall(
                        Mockito.argThat(
                                request ->
                                        request.method().equalsIgnoreCase("POST") &&
                                                request.url().encodedPath().contains("/v1/secret/data") &&
                                                request.url().encodedPathSegments().contains(KEY)));
    }

    @Test
    void destroySecretValue() throws IOException {
        // prepare
        var vaultUrl = "https://mock.url";
        var vaultToken = UUID.randomUUID().toString();
        var hashicorpVaultClientConfig =
                HashicorpVaultClientConfig.Builder.newInstance().vaultUrl(vaultUrl).vaultToken(vaultToken).build();

        var okHttpClient = Mockito.mock(OkHttpClient.class);
        var vaultClient =
                new HashicorpVaultClient(hashicorpVaultClientConfig, okHttpClient, TYPE_MANAGER);

        var call = Mockito.mock(Call.class);
        var response = Mockito.mock(Response.class);
        var body = Mockito.mock(ResponseBody.class);
        Mockito.when(okHttpClient.newCall(Mockito.any(Request.class))).thenReturn(call);
        Mockito.when(call.execute()).thenReturn(response);
        Mockito.when(response.code()).thenReturn(200);
        Mockito.when(response.body()).thenReturn(body);

        // invoke
        var result = vaultClient.destroySecret(KEY);

        // verify
        Assertions.assertNotNull(result);
        Mockito.verify(okHttpClient, Mockito.times(1))
                .newCall(
                        Mockito.argThat(
                                request ->
                                        request.method().equalsIgnoreCase("DELETE") &&
                                                request.url().encodedPath().contains("/v1/secret/metadata") &&
                                                request.url().encodedPathSegments().contains(KEY)));
    }
}
