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
package org.eclipse.dataspaceconnector.iam.did.hub;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.GenericJweReader;
import org.eclipse.dataspaceconnector.iam.did.hub.jwe.GenericJweWriter;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.ClientResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.HubMessage;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.function.Supplier;

/**
 *
 */
public class IdentityHubClientImpl implements IdentityHubClient {
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;
    private Supplier<RSAPrivateKey> privateKeyResolver;

    public IdentityHubClientImpl(Supplier<RSAPrivateKey> privateKeyResolver, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.privateKeyResolver = privateKeyResolver;
    }

    public ClientResponse<Map<String, Object>> queryCredentials(ObjectQueryRequest query, String baseHubUrl, PublicKey publicKey) {
        var privateKey = privateKeyResolver.get();
        var objectRequestJwe = new GenericJweWriter()
                .privateKey(privateKey)
                .publicKey((RSAPublicKey) publicKey)
                .objectMapper(objectMapper)
                .payload(query)
                .buildJwe();

        var objectQueryResponse = executeQuery(ObjectQueryResponse.class, objectRequestJwe, baseHubUrl + "query-objects");

        if (objectQueryResponse.getObjects().isEmpty()) {
            return new ClientResponse<>("No credential entries found");
        }

        var hubObject = objectQueryResponse.getObjects().get(0);
        var commitQuery = CommitQuery.Builder.newInstance().objectId(hubObject.getId()).build();
        var commitQueryRequest = CommitQueryRequest.Builder.newInstance()
                .query(commitQuery)
                .iss(query.getIss())
                .aud(query.getAud())
                .sub(query.getSub())
                .build();

        var commitRequestJwe = new GenericJweWriter()
                .privateKey(privateKey)
                .publicKey((RSAPublicKey) publicKey)
                .objectMapper(objectMapper)
                .payload(commitQueryRequest)
                .buildJwe();

        var commitQueryResponse = executeQuery(CommitQueryResponse.class, commitRequestJwe, baseHubUrl + "query-commits");

        if (commitQueryResponse.getCommits().isEmpty()) {
            return new ClientResponse<>("No credential commits found");
        }

        var commit = commitQueryResponse.getCommits().get(0);
        //noinspection unchecked
        return new ClientResponse<>((Map<String, Object>) commit.getPayload());

    }


    protected <M extends HubMessage> M executeQuery(Class<M> type, String jwe, String url) {
        var requestBody = RequestBody.create(jwe, MediaType.get("application/json"));
        var request = new Request.Builder().url(url).post(requestBody).build();

        try (var response = httpClient.newCall(request).execute()) {
            var body = response.body();
            assert body != null;
            return new GenericJweReader().mapper(objectMapper).jwe(body.string()).privateKey(privateKeyResolver.get()).readType(type);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
