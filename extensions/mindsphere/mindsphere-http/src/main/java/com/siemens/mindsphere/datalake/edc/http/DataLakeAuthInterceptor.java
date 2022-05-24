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
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Intercepts any call on the provided http client and uses the oauth client details to add the authentication header
 */
public class DataLakeAuthInterceptor implements Interceptor {
    public DataLakeAuthInterceptor(OkHttpClient httpClient, OauthClientDetails oauthClientDetails, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.oauthClientDetails = oauthClientDetails;
        this.objectMapper = objectMapper;
    }

    private OkHttpClient httpClient;

    private OauthClientDetails oauthClientDetails;

    private ObjectMapper objectMapper;

    private static final String AUTHORIZATION = "authorization";
    private static final String X_SPACE_AUTH_KEY = "X-SPACE-AUTH-KEY";

    @NotNull
    @Override
    public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {

        TechnicalUserTokenRequestDto technicalUserTokenRequestDto = new TechnicalUserTokenRequestDto(oauthClientDetails.getTenant(),
                oauthClientDetails.getTenant(), oauthClientDetails.getClientAppName(), oauthClientDetails.getClientAppVersion());

        final String requestPayload = objectMapper.writeValueAsString(technicalUserTokenRequestDto);
        final RequestBody requestBody = RequestBody.create(requestPayload, MediaType.parse("application/json"));

        final Request tokenRequest = new Request.Builder().url(oauthClientDetails.getAccessTokenUrl())
                .method("POST", requestBody)
                .header(X_SPACE_AUTH_KEY, String.format("Bearer %s", oauthClientDetails.getBase64Credentials()))
                .build();
        final Call call = httpClient.newCall(tokenRequest);
        final Response response = call.execute();

        final TechnicalUserTokenResponseDto technicalUserTokenResponseDto = objectMapper.readValue(response.body()
                .bytes(), TechnicalUserTokenResponseDto.class);


        final Request originRequest = chain.request();

        final Request modifiedRequest = originRequest.newBuilder()
                .header(AUTHORIZATION, composeAuthorizationHeader(technicalUserTokenResponseDto.getAccessToken()))
                .build();

        return chain.proceed(modifiedRequest);
    }

    private String composeAuthorizationHeader(String token) {
        return "Bearer " + token;
    }
}
