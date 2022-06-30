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
 *       Siemens AG - added additionalHeaders
 *
 */

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.ParallelSink;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Writes data in a streaming fashion to an HTTP endpoint.
 */
public class HttpDataSink extends ParallelSink {
    private static final StatusResult ERROR_WRITING_DATA = StatusResult.failure(ERROR_RETRY, "Error writing data");

    private String authKey;
    private String authCode;
    private String endpoint;
    private String contentType;
    private OkHttpClient httpClient;
    private Map<String, String> additionalHeaders = new HashMap<>();

    /**
     * Sends the parts to the destination endpoint using an HTTP POST.
     */
    @Override
    protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {
            var requestBody = new StreamingRequestBody(part, contentType);
            var requestBuilder = new Request.Builder();
            if (authKey != null) {
                requestBuilder.header(authKey, authCode);
            }

            if (additionalHeaders != null) {
                additionalHeaders.forEach(requestBuilder::header);
            }

            var request = requestBuilder.url(endpoint).post(requestBody).build();
            try (var response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    monitor.severe(format("Error {%s: %s} received writing HTTP data %s to endpoint %s for request: %s", response.code(), response.message(), part.name(), endpoint, request));
                    return ERROR_WRITING_DATA;
                }

                return StatusResult.success();
            } catch (Exception e) {
                monitor.severe(format("Error writing HTTP data %s to endpoint %s for request: %s", part.name(), endpoint, request), e);
                return ERROR_WRITING_DATA;
            }
        }
        return StatusResult.success();
    }

    private HttpDataSink() {
    }

    public static class Builder extends ParallelSink.Builder<Builder, HttpDataSink> {

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder endpoint(String endpoint) {
            sink.endpoint = endpoint;
            return this;
        }

        public Builder authKey(String authKey) {
            sink.authKey = authKey;
            return this;
        }

        public Builder authCode(String authCode) {
            sink.authCode = authCode;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            sink.httpClient = httpClient;
            return this;
        }

        public Builder additionalHeaders(Map<String, String> additionalHeaders) {
            sink.additionalHeaders.putAll(additionalHeaders);
            return this;
        }

        public Builder contentType(String contentType) {
            sink.contentType = contentType;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(sink.endpoint, "endpoint");
            Objects.requireNonNull(sink.httpClient, "httpClient");
            if (sink.authKey != null && sink.authCode == null) {
                throw new IllegalStateException("An authKey was set but authCode was null");
            }
            if (sink.authCode != null && sink.authKey == null) {
                throw new IllegalStateException("An authCode was set but authKey was null");
            }
        }

        private Builder() {
            super(new HttpDataSink());
        }
    }
}
