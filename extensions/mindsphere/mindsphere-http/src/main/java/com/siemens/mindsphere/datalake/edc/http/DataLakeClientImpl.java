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
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * It has the responsibility to:
 * - create upload presign url (used to upload files to)
 * - create download presign url (used to get the file content from)
 *
 * It needs a
 */
public class DataLakeClientImpl implements DataLakeClient {
    public DataLakeClientImpl(URL dataLakeBaseUrl, OkHttpClient client, ObjectMapper objectMapper) {
        this.dataLakeBaseUrl = dataLakeBaseUrl;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public DataLakeClientImpl(URL dataLakeBaseUrl) {
        this(dataLakeBaseUrl, new OkHttpClient(), new ObjectMapper());
    }

    public DataLakeClientImpl(OauthClientDetails oauthClientDetails, URL dataLakeBaseUrl) {
        this(dataLakeBaseUrl, new OkHttpClient(), new ObjectMapper());
        this.oauthClientDetails = oauthClientDetails;
    }

    public static void setInstance(DataLakeClientImpl clientImpl) {
        instance = clientImpl;
    }

    public static DataLakeClientImpl getInstance() {
        return instance;
    }

    private static DataLakeClientImpl instance;


    private OauthClientDetails oauthClientDetails;

    private final URL dataLakeBaseUrl;

    private final OkHttpClient client;

    private final ObjectMapper objectMapper;

    private static final String DATA_LAKE_SIGN_REQ_UPLOAD_URL = "/generateUploadObjectUrls";

    private static final String DATA_LAKE_SIGN_REQ_DOWNLOAD_URL = "/generateDownloadObjectUrls";

    private static final String TENANT_IDENTIFIER = "ten=";

    private static final String X_SPACE_AUTH_KEY = "X-SPACE-AUTH-KEY";

    private static final String AUTHORIZATION = "authorization";

    private static class DatalakePaths {
        private List<DatalakePath> paths = new ArrayList<>();

        public List<DatalakePath> getPaths() {
            return paths;
        }

        public DatalakePaths setPaths(List<DatalakePath> paths) {
            this.paths = paths;
            return this;
        }
    }

    private static class DatalakePath {
        private String path;

        public String getPath() {
            return path;
        }

        public DatalakePath setPath(String path) {
            this.path = path;
            return this;
        }
    }

    /**
     * see https://documentation.mindsphere.io/MindSphere/apis/iot-integrated-data-lake/api-integrated-data-lake-samples-download-data.html
     */
    @Override
    public URL getPresignedDownloadUrl(final String datalakePath) throws DataLakeException {
        try {
            final String path = objectMapper.writeValueAsString(new DatalakePaths()
                    .setPaths(Collections.singletonList(new DatalakePath().setPath(datalakePath))));

            final String extractedTenant = extractTenantFromPath(path)
                    .orElseThrow(() -> new DataLakeException("Could not identify tenant for path " + datalakePath));

            final String accessToken = getAccessToken(extractedTenant)
                    .orElseThrow(() -> new DataLakeException("Getting token failed"));

            final String payloadString = objectMapper.writeValueAsString(new DatalakePaths()
                    .setPaths(Collections.singletonList(new DatalakePath().setPath(extractFilenameFromPath(datalakePath)))));
            final RequestBody requestPayload = RequestBody.create(payloadString, MediaType.parse("application/json"));
            final Request request = new Request.Builder()
                    .method("POST", requestPayload)
                    .url(dataLakeBaseUrl + DATA_LAKE_SIGN_REQ_DOWNLOAD_URL)
                    .header(AUTHORIZATION, accessToken)
                    .build();
            final Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new DataLakeException("Request to DataLake was not successful " + response);
            }

            final SignUrlResponseContainerDto signUrlResponseContainerDto = objectMapper.readValue(response.body()
                    .bytes(), SignUrlResponseContainerDto.class);

            final SignUrlResponseContainerDto.SignUrlResponseDto signedUrl = signUrlResponseContainerDto.getObjectUrls().stream().findFirst().orElseThrow(() -> new DataLakeException("No such path in Data Lake"));

            return new URL(signedUrl.getSignedUrl());
        } catch (IOException e) {
            throw new DataLakeException("Error getting signed URL", e);
        }
    }

    private String extractFilenameFromPath(String datalakePath) {
        return datalakePath.startsWith("data/ten=") ? datalakePath.replace("data/ten=", "").split("/")[1] : datalakePath;
    }

    @Override
    public URL getPresignedUploadUrl(String datalakePath) throws DataLakeException {
        try {
            final String extractedTenant = extractTenantFromPath(datalakePath)
                    .orElseThrow(() -> new DataLakeException("Could not identify tenant for path " + datalakePath));

            final String filePath = extractFilePath(datalakePath)
                    .orElseThrow(() -> new DataLakeException("Could not identify filepath for path " + datalakePath));

            final String accessToken = getAccessToken(extractedTenant)
                    .orElseThrow(() -> new DataLakeException("Getting token failed"));

            final SignUrlRequestContainerDto requestContainerDto = SignUrlRequestContainerDto.composeForSinglePath(filePath);
            final String payloadString = objectMapper.writeValueAsString(requestContainerDto);
            final RequestBody requestPayload = RequestBody.create(payloadString, MediaType.parse("application/json"));
            final Request request = new Request.Builder()
                    .method("POST", requestPayload)
                    .url(dataLakeBaseUrl  + DATA_LAKE_SIGN_REQ_UPLOAD_URL)
                    .header(AUTHORIZATION, accessToken)
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

    private Optional<String> extractFilePath(String path) {
        final Optional<String> tenant = extractTenantFromPath(path);
        return tenant
                .map(s -> path.substring(path.indexOf(s) + s.length() + 1));
    }

    private Optional<String> extractTenantFromPath(String path) {
        if (path == null || !path.contains(TENANT_IDENTIFIER)) {
            return Optional.empty();
        }
        int tenantStartIndex = path.indexOf(TENANT_IDENTIFIER);
        int firstSlashAfterTenantIndex = path.indexOf("/", tenantStartIndex);

        if (firstSlashAfterTenantIndex == -1) {
            return Optional.empty();
        }
        return Optional.of(path.substring(tenantStartIndex + TENANT_IDENTIFIER.length(), firstSlashAfterTenantIndex));
    }

    private Optional<String> getAccessToken(String tenant) throws IOException {
        TechnicalUserTokenRequestDto technicalUserTokenRequestDto = new TechnicalUserTokenRequestDto(oauthClientDetails.getTenant(),
                tenant, oauthClientDetails.getClientAppName(), oauthClientDetails.getClientAppVersion());

        final String requestPayload = objectMapper.writeValueAsString(technicalUserTokenRequestDto);
        final RequestBody requestBody = RequestBody.create(requestPayload, MediaType.parse("application/json"));


        final Request tokenRequest = new Request.Builder().url(oauthClientDetails.getAccessTokenUrl())
                .method("POST", requestBody)
                .header(X_SPACE_AUTH_KEY, String.format("Bearer %s", oauthClientDetails.getBase64Credentials()))
                .build();

        final Call call = client.newCall(tokenRequest);
        final Response response = call.execute();


        final TechnicalUserTokenResponseDto technicalUserTokenResponseDto = objectMapper.readValue(response.body()
                .bytes(), TechnicalUserTokenResponseDto.class);

        return technicalUserTokenResponseDto.getAccessToken() == null ?
                Optional.empty() : Optional.of("Bearer " + technicalUserTokenResponseDto.getAccessToken());
    }

    @Override
    public boolean isPresent(String path) {
        throw new UnsupportedOperationException("TODO");
    }
}
