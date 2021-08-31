/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.hub.gaiax;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.WriteRequestWriter;
import org.eclipse.dataspaceconnector.iam.did.testFixtures.TemporaryKeyLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.eclipse.dataspaceconnector.iam.did.hub.gaiax.GaiaxConstants.CONSUMER_WRITE_COMMIT_TEMP_URL;
import static org.eclipse.dataspaceconnector.iam.did.hub.gaiax.GaiaxConstants.CONSUMER_WRITE_COMMIT_URL;
import static org.eclipse.dataspaceconnector.iam.did.hub.gaiax.GaiaxConstants.PRODUCER_WRITE_COMMIT_URL;
import static org.eclipse.dataspaceconnector.iam.did.util.GaiaXAssumptions.assumptions;

/**
 * Tools for writing verified credentials. To enable set the ENV variable {@code GAIA-X-LOCAL-HACKATHON} to true.
 * TODO HACKATHON-1 TASK 1: Java-based example
 */
public class VerificationTool {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Deprecated
    public void writeConsumerCredentialsDirect() throws Exception {
        assumptions();
        var credential = GaiaXCredential.Builder.newInstance().companyId("Consumer").region("eu").build();

        var serializedCredential = objectMapper.writeValueAsString(credential);
        System.out.println("Serialized credential: " + serializedCredential);

        var requestBody = RequestBody.create(serializedCredential, MediaType.get("application/json"));
        var request = new Request.Builder().url(CONSUMER_WRITE_COMMIT_TEMP_URL).post(requestBody).build();

        var httpClient = new OkHttpClient.Builder().build();
        var response = httpClient.newCall(request).execute();

        System.out.println("Write consumer response: " + response.code());
    }

    @Test
    public void writeConsumerCredentials() throws Exception {
        assumptions();
        var credential = GaiaXCredential.Builder.newInstance().companyId("Consumer").region("eu").build();

        Response response = getResponse(credential, CONSUMER_WRITE_COMMIT_URL);

        System.out.println("Write consumer response: " + response.code());
    }

    @Test
    public void writeProducerCredentials() throws Exception {
        assumptions();
        var credential = GaiaXCredential.Builder.newInstance().companyId("Producer").region("eu").build();
        Response response = getResponse(credential, PRODUCER_WRITE_COMMIT_URL);

        System.out.println("Write producer response: " + response.code());
    }


    @NotNull
    private Response getResponse(GaiaXCredential credential, String url) throws Exception {


        var keys = TemporaryKeyLoader.loadKeys();

        var jwe = new WriteRequestWriter()
                .privateKey(keys.toRSAPrivateKey())
                .publicKey(keys.toRSAPublicKey())
                .objectMapper(objectMapper)
                .commitObject(credential)
                .kid("kid")
                .sub("sub")
                .context("GAIA-X")
                .type("RegistrationCredentials").buildJwe();

        var httpClient = new OkHttpClient.Builder().build();
        var requestBody = RequestBody.create(jwe, MediaType.get("application/json"));
        var request = new Request.Builder().url(url).post(requestBody).build();
        return httpClient.newCall(request).execute();
    }


}
