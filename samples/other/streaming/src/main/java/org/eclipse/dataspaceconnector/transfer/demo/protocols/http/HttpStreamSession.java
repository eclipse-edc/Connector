/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.demo.protocols.http;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.transfer.inline.StreamSession;

import java.io.IOException;
import java.net.URL;

/**
 * Publishes to an HTTP endpoint.
 */
public class HttpStreamSession implements StreamSession {
    private final URL endpointUrl;
    private final String destinationToken;
    private final OkHttpClient httpClient;

    public HttpStreamSession(URL endpointUrl, String destinationToken, OkHttpClient httpClient) {
        this.endpointUrl = endpointUrl;
        this.destinationToken = destinationToken;
        this.httpClient = httpClient;
    }

    @Override
    public void publish(byte[] data) {
        try {
            var body = RequestBody.create(data, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(endpointUrl)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Authorization", destinationToken)
                    .post(body)
                    .build();

            try (var response = httpClient.newCall(request).execute()) {
                if (response.code() != 200) {
                    throw new EdcException("Invalid response received from destination: " + response.code());
                }
            }

        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
