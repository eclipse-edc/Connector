/*
 *  Copyright (c) 2021, 2022 Siemens AG
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

package com.siemens.mindsphere.datalake.edc.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class DataLakeClientImpl implements DataLakeClient {
    public DataLakeClientImpl(URI dataLakeBaseUrl, OkHttpClient client, ObjectMapper objectMapper) {
        this.dataLakeBaseUrl = dataLakeBaseUrl;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public DataLakeClientImpl(URI dataLakeBaseUrl) {
        this(dataLakeBaseUrl, new OkHttpClient(), new ObjectMapper());
    }

    private final URI dataLakeBaseUrl;

    private final OkHttpClient client;

    private final ObjectMapper objectMapper;

    private static final String DATA_LAKE_SIGN_REQ_URL = "/generateUploadObjectUrls";

    @Override
    public URL getUrl(String path) throws DataLakeException {
        try {
            final SignUrlRequestContainerDto requestContainerDto = SignUrlRequestContainerDto.composeForSinglePath(path);
            final String payloadString = objectMapper.writeValueAsString(requestContainerDto);
            final RequestBody requestPayload = RequestBody.create(payloadString, MediaType.parse("application/json"));
            final Request request = new Request.Builder()
                    .method("POST", requestPayload)
                    .url(dataLakeBaseUrl.resolve(DATA_LAKE_SIGN_REQ_URL).toURL())
                    .build();
            final Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new DataLakeException("Request to DataLake was not successful");
            }

            final SignUrlResponseContainerDto signUrlResponseContainerDto = objectMapper.readValue(response.body()
                    .bytes(), SignUrlResponseContainerDto.class);

            final SignUrlResponseContainerDto.SignUrlResponseDto signedUrl = signUrlResponseContainerDto.getObjectUrls().stream().findFirst().orElseThrow(() -> new DataLakeException("No such path in Data Lake"));

            return new URL(signedUrl.getSignedUrl());
        } catch (IOException e) {
            throw new DataLakeException("Error getting signed URL", e);
        }
    }

    @Override
    public boolean isPresent(String path) {
        throw new UnsupportedOperationException("TODO");
    }
}
