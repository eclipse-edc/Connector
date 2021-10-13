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

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.transfer.demo.protocols.stream.StreamSession;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Publishes to an HTTP endpoint.
 */
public class HttpStreamSession implements StreamSession {
    private final URL endpointUrl;
    private final String destinationToken;
    private final HttpClient httpClient;

    public HttpStreamSession(URL endpointUrl, String destinationToken, HttpClient httpClient) {
        this.endpointUrl = endpointUrl;
        this.destinationToken = destinationToken;
        this.httpClient = httpClient;
    }

    @Override
    public void publish(byte[] data) {
        try {
            var request = HttpRequest.newBuilder(endpointUrl.toURI())
                    .header("Content-Type", "application/json")
                    .header("X-Authorization", destinationToken)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new EdcException("Invalid response received from destination: " + response.statusCode());
            }
        } catch (InterruptedException | IOException e) {
            throw new EdcException(e);
        } catch (URISyntaxException e) {
            throw new EdcException("Cannot parse URI " + endpointUrl);
        }
    }

    @Override
    public void close() {
        // no-op
    }
}
