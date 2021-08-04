/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.client.command.http;

import org.eclipse.dataspaceconnector.client.command.ExecutionContext;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Creates OK Http Clients for use by command executors.
 */
public class HttpFactory {
    private static final String X_CONTROL_AUTHORIZATION = "TODO";

    public static OkHttpClient create(ExecutionContext context) {
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).addInterceptor(new AuthClientInterceptor(context)).build();
    }

    /**
     * Adds the auth code header to a request.
     */
    private static class AuthClientInterceptor implements Interceptor {
        private ExecutionContext context;

        public AuthClientInterceptor(ExecutionContext context) {
            this.context = context;
        }

        @NotNull
        @Override
        public Response intercept(@NotNull Chain chain) throws IOException {
            Request.Builder builder = chain.request().newBuilder();
//            builder.addHeader(X_CONTROL_AUTHORIZATION, TODO);
            builder.addHeader("Content-Type", "application/json");
            Request newRequest = builder.build();
            return chain.proceed(newRequest);
        }
    }
}
