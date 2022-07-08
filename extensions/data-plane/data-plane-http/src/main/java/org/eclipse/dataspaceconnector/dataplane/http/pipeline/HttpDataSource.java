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


import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.stream.Stream;

import static dev.failsafe.Failsafe.with;
import static java.lang.String.format;

public class HttpDataSource implements DataSource {
    private String name;
    private HttpRequestParams params;
    private String requestId;
    private RetryPolicy<Object> retryPolicy;
    private OkHttpClient httpClient;

    @Override
    public Stream<Part> openPartStream() {
        return Stream.of(getPart());
    }

    private HttpPart getPart() {
        try (var response = with(retryPolicy).get(() -> httpClient.newCall(params.toRequest()).execute())) {
            var body = response.body();
            var stringBody = body != null ? body.string() : null;
            if (stringBody == null) {
                throw new EdcException(format("Received empty response body transferring HTTP data for request %s: %s", requestId, response.code()));
            }
            if (response.isSuccessful()) {
                return new HttpPart(name, stringBody.getBytes());
            } else {
                throw new EdcException(format("Received code transferring HTTP data for request %s: %s - %s. %s", requestId, response.code(), response.message(), stringBody));
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

        public Builder retryPolicy(RetryPolicy<Object> retryPolicy) {
            dataSource.retryPolicy = retryPolicy;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            dataSource.httpClient = httpClient;
            return this;
        }

        public HttpDataSource build() {
            Objects.requireNonNull(dataSource.requestId, "requestId");
            Objects.requireNonNull(dataSource.httpClient, "httpClient");
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

        HttpPart(String name, byte[] content) {
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
