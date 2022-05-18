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
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

class DataLakeAuthInterceptorTest {

    @Test
    void intercept() throws IOException, InterruptedException {

        final ObjectMapper objectMapper = new ObjectMapper();

        final TechnicalUserTokenResponseDto technicalUserTokenResponseDto = new TechnicalUserTokenResponseDto();
        technicalUserTokenResponseDto.setAccessToken("access_token");

        MockWebServer authServer = new MockWebServer();
        authServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(technicalUserTokenResponseDto)));
        authServer.start(9991);

        MockWebServer targetServer = new MockWebServer();
        targetServer.enqueue(new MockResponse().setBody("not important for the test"));
        targetServer.start(9992);

        OkHttpClient clientForAuth = new OkHttpClient.Builder()
                .build();

        final String clientId = "clientId";
        final String clientSecret = "clientSecret";
        final String clientAppName = "clientAppName";
        final String appVersion = "v1.0.0";
        final String tenant = "tenant";
        final URL tokenUrl = authServer.url("/auth/token").url();
        final OauthClientDetails oauthClientDetails = new OauthClientDetails(clientId,
                clientSecret,
                clientAppName,
                appVersion,
                tenant,
                tokenUrl);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new DataLakeAuthInterceptor(clientForAuth, oauthClientDetails, objectMapper))
                .build();

        final RequestBody requestPayload = RequestBody.create("requestPayload", MediaType.parse("application/text"));
        final Request request = new Request.Builder()
                .method("GET", null)
                .url(targetServer.url("/any/path"))
                .build();

        client.newCall(request).execute();

        RecordedRequest targetServerRequest = targetServer.takeRequest();

        assertEquals("/any/path", targetServerRequest.getPath());
        assertEquals("Bearer access_token", targetServerRequest.getHeader("Authorization"));

        targetServer.shutdown();
        authServer.shutdown();
    }
}
