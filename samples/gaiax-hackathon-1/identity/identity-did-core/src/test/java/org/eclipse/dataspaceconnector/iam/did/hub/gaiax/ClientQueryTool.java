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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.GenericJweReader;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.GenericJweWriter;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.HubMessage;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.eclipse.dataspaceconnector.iam.did.hub.TemporaryKeyLoader.loadKeys;
import static org.eclipse.dataspaceconnector.iam.did.util.GaiaXAssumptions.assumptions;
import static org.eclipse.dataspaceconnector.iam.did.hub.gaiax.GaiaxConstants.CONSUMER_COMMIT_QUERY_URL;
import static org.eclipse.dataspaceconnector.iam.did.hub.gaiax.GaiaxConstants.CONSUMER_OBJECT_QUERY_URL;

/**
 *
 */
public class ClientQueryTool {
    private ObjectMapper objectMapper = new ObjectMapper();
    private RSAKey keys;

    public ClientQueryTool() {
        keys = loadKeys();
    }

    @Test
    public void queryConsumerCredentials() throws Exception {
        assumptions();

        var privateKey = keys.toRSAPrivateKey();
        var publicKey = keys.toRSAPublicKey();

        var query = ObjectQuery.Builder.newInstance().context("GAIA-X").type("RegistrationCredentials").build();
        var queryRequest = ObjectQueryRequest.Builder.newInstance().query(query).iss("123").aud("aud").sub("sub").build();

        var objectRequestJwe = new GenericJweWriter()
                .privateKey(privateKey)
                .publicKey(publicKey)
                .objectMapper(objectMapper)
                .payload(queryRequest)
                .buildJwe();

        var objectQueryResponse = executeQuery(ObjectQueryResponse.class, objectRequestJwe, CONSUMER_OBJECT_QUERY_URL);

        objectQueryResponse.getObjects().forEach(hubObject -> {
            var commitQuery = CommitQuery.Builder.newInstance().objectId(hubObject.getId()).build();
            var commitQueryRequest = CommitQueryRequest.Builder.newInstance().query(commitQuery).iss("123").aud("aud").sub("sub").build();

            var commitRequestJwe = new GenericJweWriter()
                    .privateKey(privateKey)
                    .publicKey(publicKey)
                    .objectMapper(objectMapper)
                    .payload(commitQueryRequest)
                    .buildJwe();

            var commitQueryResponse = executeQuery(CommitQueryResponse.class, commitRequestJwe, CONSUMER_COMMIT_QUERY_URL);
            commitQueryResponse.getCommits().forEach(commit -> {
                try {
                    System.out.println("Commit:" + objectMapper.writeValueAsString(commit.getPayload()));
                } catch (JsonProcessingException e) {
                    throw new EdcException(e);
                }
            });

        });
    }


    private <M extends HubMessage> M executeQuery(Class<M> type, String jwe, String url) {
        var httpClient = new OkHttpClient.Builder().build();
        var requestBody = RequestBody.create(jwe, MediaType.get("application/json"));
        var request = new Request.Builder().url(url).post(requestBody).build();

        try (var response = httpClient.newCall(request).execute()) {
            var body = response.body();
            assert body != null;
            return new GenericJweReader().mapper(objectMapper).jwe(body.string()).privateKey(keys.toRSAPrivateKey()).readType(type);
        } catch (IOException | JOSEException e) {
            throw new EdcException(e);
        }
    }
}
