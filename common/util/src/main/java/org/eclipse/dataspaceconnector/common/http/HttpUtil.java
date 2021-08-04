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

package org.eclipse.dataspaceconnector.common.http;

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
