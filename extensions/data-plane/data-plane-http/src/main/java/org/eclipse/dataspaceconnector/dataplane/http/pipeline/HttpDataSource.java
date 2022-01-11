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
package org.eclipse.dataspaceconnector.dataplane.http.pipeline;


import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static net.jodah.failsafe.Failsafe.with;

/**
 * Pulls data from a source using an HTTP GET.
 */
public class HttpDataSource implements DataSource {
    private String sourceEndpoint;
    private String name;
    private String requestId;
    private RetryPolicy<Object> retryPolicy;
    private OkHttpClient httpClient;
    private Monitor monitor;

    @Override
    public Stream<Part> openStream() {
        return Stream.of(getPart());
    }

    private HttpPart getPart() {
        var request = new Request.Builder().url(sourceEndpoint + "/" + name).get().build();
        try (var response = with(retryPolicy).get(() -> httpClient.newCall(request).execute())) {
            if (response.isSuccessful()) {
                var body = response.body();
                if (body == null) {
                    throw new EdcException(format("Received empty response body transferring HTTP data for request %s: %s", requestId, response.code()));
                }
                return new HttpPart(name, body.bytes());
            } else {
                throw new EdcException(format("Received code transferring HTTP data for request %s: %s", requestId, response.code()));
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private HttpDataSource() {
    }

    public static class Builder {
        private HttpDataSource dataSource;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder sourceUrl(String sourceUrl) {
            dataSource.sourceEndpoint = sourceUrl;
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

        public Builder retryPolicy(RetryPolicy<Object> retryPolicy) {
            dataSource.retryPolicy = retryPolicy;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            dataSource.httpClient = httpClient;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            dataSource.monitor = monitor;
            return this;
        }

        public HttpDataSource build() {
            Objects.requireNonNull(dataSource.sourceEndpoint, "sourceEndpoint");
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.httpClient, "httpClient");
            Objects.requireNonNull(dataSource.monitor, "monitor");
            Objects.requireNonNull(dataSource.retryPolicy, "retryPolicy");
            return dataSource;
        }

        private Builder() {
            dataSource = new HttpDataSource();
        }
    }

    private static class HttpPart implements Part {
        private final String name;
        private final byte[] content;

        public HttpPart(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long size() {
            return content.length;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(content);
        }

    }

}
