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
import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.CommitQueryResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.HubMessage;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryResponse;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.String.format;


public class IdentityHubClientImpl implements IdentityHubClient {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Supplier<PrivateKeyWrapper> privateKeySupplier;

    public IdentityHubClientImpl(Supplier<PrivateKeyWrapper> privateKeySupplier, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.privateKeySupplier = privateKeySupplier;
    }

    @Override
    public Result<Map<String, Object>> queryCredentials(ObjectQueryRequest query, String baseHubUrl, PublicKeyWrapper publicKey) {
        var privateKey = privateKeySupplier.get();
        var objectRequestJwe = new GenericJweWriter()
                .privateKey(privateKey)
                .publicKey(publicKey)
                .objectMapper(objectMapper)
                .payload(query)
                .buildJwe();

        var objectQueryResponse = executeQuery(ObjectQueryResponse.class, objectRequestJwe, baseHubUrl + "query-objects");

        if (objectQueryResponse.getObjects().isEmpty()) {
            return Result.failure("No credential entries found");
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
                .publicKey(publicKey)
                .objectMapper(objectMapper)
                .payload(commitQueryRequest)
                .buildJwe();

        var commitQueryResponse = executeQuery(CommitQueryResponse.class, commitRequestJwe, baseHubUrl + "query-commits");

        if (commitQueryResponse.getCommits().isEmpty()) {
            return Result.failure("No credential commits found");
        }

        var commit = commitQueryResponse.getCommits().get(0);
        return Result.success((Map<String, Object>) commit.getPayload());

    }

    protected <M extends HubMessage> M executeQuery(Class<M> type, String jwe, String url) {
        var requestBody = RequestBody.create(jwe, MediaType.get("application/json"));
        var request = new Request.Builder().url(url).post(requestBody).build();

        try (var response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                var body = response.body();
                assert body != null;
                return new GenericJweReader().mapper(objectMapper).jwe(body.string()).privateKey(privateKeySupplier.get()).readType(type);
            }
            throw new EdcException(format("Identity Hub request was not successful: %s - %s", response.code(), response.message()));
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
