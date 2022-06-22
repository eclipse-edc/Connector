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
 *       Siemens AG - changes to make it compatible with AWS S3, Azure blob and AWS China S3 presigned URL for upload
 *
 */

package org.eclipse.dataspaceconnector.dataplane.http.pipeline;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.ParallelSink;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private boolean usePartName = true;
    private OkHttpClient httpClient;
    private Map<String, String> additionalHeaders = new HashMap<>();
    private HttpDataSinkRequest requestBuilder = new HttpDataSinkRequestPost();

    /**
     * Sends the parts to the destination endpoint using an HTTP POST.
     */
    @Override
    protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {

            var requestBuilder = new Request.Builder();
            if (authKey != null) {
                requestBuilder.header(authKey, authCode);
            }

            if (additionalHeaders != null) {
                additionalHeaders.forEach(requestBuilder::header);
            }

            var request = this.requestBuilder.makeRequestForPart(requestBuilder, part)
                    .orElseThrow(() -> new IllegalStateException("Failed to build a request"));

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

    private interface HttpDataSinkRequest {
        Optional<Request> makeRequestForPart(Request.Builder requestBuilder, DataSource.Part part);
    }

    private class HttpDataSinkRequestPut implements HttpDataSinkRequest {
        @Override
        public Optional<Request> makeRequestForPart(Request.Builder requestBuilder, DataSource.Part part) {
            RequestBody body;

            try (InputStream stream = part.openStream()) {
                body = RequestBody.create(stream.readAllBytes());
            } catch (IOException e) {
                monitor.severe(format("Error reading bytes for HTTP part data %s", part.name()));
                return Optional.empty();
            }

            return Optional.of(
                    requestBuilder
                            .url(makeUrl(part))
                            .put(body)
                            .build());
        }
    }

    private class HttpDataSinkRequestPost implements HttpDataSinkRequest {
        @Override
        public Optional<Request> makeRequestForPart(final Request.Builder requestBuilder, final DataSource.Part part) {
            var requestBody = new StreamingRequestBody(part);
            return Optional.of(
                    requestBuilder
                            .url(makeUrl(part))
                            .post(requestBody)
                            .build());
        }
    }

    private String makeUrl(DataSource.Part part) {
        return usePartName ? endpoint + "/" + part.name() : endpoint;
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
            sink.additionalHeaders = additionalHeaders;
            return this;
        }

        public Builder usePartName(boolean usePartName) {
            sink.usePartName = usePartName;
            return this;
        }

        public Builder httpVerb(String httpVerb) {
            sink.requestBuilder = makeSender(httpVerb);
            return this;
        }

        private HttpDataSinkRequest makeSender(String httpVerb) {
            return "PUT".equalsIgnoreCase(httpVerb) ? sink.new HttpDataSinkRequestPut() : sink.new HttpDataSinkRequestPost();
        }

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
