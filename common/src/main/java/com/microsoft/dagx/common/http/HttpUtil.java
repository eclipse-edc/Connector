/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.common.http;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;

public class HttpUtil {


    /**
     * Creates a new OkHttpClient that uses a Basic Auth authenticator by adding the "Authorization" header
     */
    public static OkHttpClient addBasicAuth(OkHttpClient client, String username, String password) {
        return client.newBuilder().authenticator((route, response) -> {
            var credential = Credentials.basic(username, password);
            return response.request().newBuilder().header("Authorization", credential).build();
        }).build();
    }
}
