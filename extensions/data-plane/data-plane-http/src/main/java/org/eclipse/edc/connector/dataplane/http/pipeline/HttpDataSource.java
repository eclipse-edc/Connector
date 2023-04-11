/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.http.pipeline;


import org.eclipse.edc.connector.dataplane.http.params.HttpRequestFactory;
import org.eclipse.edc.connector.dataplane.http.spi.HttpRequestParams;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

public class HttpDataSource implements DataSource {
    private String name;
    private HttpRequestParams params;
    private String requestId;
    private Monitor monitor;
    private EdcHttpClient httpClient;
    private HttpRequestFactory requestFactory;

    @Override
    public Stream<Part> openPartStream() {
        return Stream.of(getPart());
    }

    private HttpPart getPart() {
        var request = requestFactory.toRequest(params);
        monitor.debug(() -> "HttpDataSource sends request: " + request.toString());
        try  {
            // NB: Do not close the response as the body input stream needs to be read after this method returns. The response closes the body stream.
            var response = httpClient.execute(request);
            if (response.isSuccessful()) {
                var body = response.body();
                if (body == null) {
                    throw new EdcException(format("Received empty response body transferring HTTP data for request %s: %s", requestId, response.code()));
                }
                return new HttpPart(name, body.byteStream());
            } else {
                throw new EdcException(format("Received code transferring HTTP data for request %s: %s - %s.", requestId, response.code(), response.message()));
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }

    }

    private HttpDataSource() {
    }

    public static class Builder {
        private final HttpDataSource dataSource;

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            dataSource = new HttpDataSource();
        }

        public Builder params(HttpRequestParams params) {
            dataSource.params = params;
            return this;
        }

        public Builder name(String name) {
            dataSource.name = name;
            return this;
        }

        public Builder requestId(String requestId) {
            dataSource.requestId = requestId;
            return this;
        }

        public Builder httpClient(EdcHttpClient httpClient) {
            dataSource.httpClient = httpClient;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }

        public Builder requestFactory(HttpRequestFactory requestFactory) {
            dataSource.requestFactory = requestFactory;
            return this;
        }

        public HttpDataSource build() {
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.httpClient, "httpClient");
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(dataSource.requestFactory, "requestFactory");
            return dataSource;
        }
    }

    private static class HttpPart implements Part {
        private final String name;
        private final InputStream content;

        HttpPart(String name, InputStream content) {
            this.name = name;
            this.content = content;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long size() {
            return SIZE_UNKNOWN;
        }

        @Override
        public InputStream openStream() {
            return content;
        }

    }
}
