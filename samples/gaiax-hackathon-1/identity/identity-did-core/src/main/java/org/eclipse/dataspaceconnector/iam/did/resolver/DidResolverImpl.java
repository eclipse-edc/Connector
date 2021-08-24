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
package org.eclipse.dataspaceconnector.iam.did.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidResolver;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class DidResolverImpl implements DidResolver {
    private String didResolverUrl;
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;

    public LinkedHashMap<String, Object> resolveDid(String didKey) {
        var request = new Request.Builder().url(didResolverUrl + didKey).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            var responseBody = response.body();
            if (responseBody == null) {
                throw new EdcException("Null response returned from DID resolver service");
            }
            //noinspection unchecked
            return (LinkedHashMap<String, Object>) objectMapper.readValue(responseBody.string(), Map.class);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    public DidResolverImpl(String didResolverUrl, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.didResolverUrl = didResolverUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }
}
