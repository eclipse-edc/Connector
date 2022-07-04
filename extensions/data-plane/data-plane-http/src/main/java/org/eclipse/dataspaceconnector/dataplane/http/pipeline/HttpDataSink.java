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
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.ParallelSink;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;

import java.util.List;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Writes data in a streaming fashion to an HTTP endpoint.
 */
public class HttpDataSink extends ParallelSink {
    private static final StatusResult<Void> ERROR_WRITING_DATA = StatusResult.failure(ERROR_RETRY, "Error writing data");

    private HttpRequestParams params;
    private OkHttpClient httpClient;
    
    @Override
    protected StatusResult<Void> transferParts(List<DataSource.Part> parts) {
        for (DataSource.Part part : parts) {
            var request = params.toRequest(part::openStream);
            try (var response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    monitor.severe(format("Error {%s: %s} received writing HTTP data %s to endpoint %s for request: %s",
                            response.code(), response.message(), part.name(), request.url().url(), request));
                    return ERROR_WRITING_DATA;
                }

                return StatusResult.success();
            } catch (Exception e) {
                monitor.severe(format("Error writing HTTP data %s to endpoint %s for request: %s", part.name(), request.url().url(), request), e);
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

        public Builder params(HttpRequestParams params) {
            sink.params = params;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            sink.httpClient = httpClient;
            return this;
        }

        protected void validate() {
        }

        private Builder() {
            super(new HttpDataSink());
        }
    }
}
