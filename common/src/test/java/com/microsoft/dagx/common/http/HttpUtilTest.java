/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.common.http;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUtilTest {

    private OkHttpClient okHttpClient;

    @BeforeEach
    void setUp() {
        okHttpClient = new OkHttpClient();
    }

    @Test
    void addBasicAuth_verifyNewInstance() {
        var newClient = HttpUtil.addBasicAuth(okHttpClient, "somuser", "somepwd");
        assertThat(newClient).isNotEqualTo(okHttpClient);
        assertThat(newClient.authenticator()).isNotNull();
    }

    @Test
    void addBasicAuth_verifyAuthorizationHeader() throws IOException {
        var newClient = HttpUtil.addBasicAuth(okHttpClient, "somuser", "somepwd");

        final Response response = new Response.Builder()
                .code(200)
                .protocol(Protocol.HTTP_2)
                .message("yey, it works")
                .request(new Request.Builder().url("http://localhost/api/test").build())
                .build();
        var rq = newClient.authenticator().authenticate(null, response);

        assertThat(rq.headers()).isNotNull().anyMatch(p -> p.getFirst().equals("Authorization") && p.getSecond().startsWith("Basic "));

    }
}