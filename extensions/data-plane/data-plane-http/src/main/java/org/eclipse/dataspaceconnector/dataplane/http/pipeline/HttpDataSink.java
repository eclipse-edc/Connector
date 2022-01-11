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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;

/**
 * Writes data in a streaming fashion to an HTTP endpoint.
 */
public class HttpDataSink implements DataSink {
    private String endpoint;
    private String requestId;
    private OkHttpClient httpClient;
    private ExecutorService executorService;
    private Monitor monitor;

    @Override
    public CompletableFuture<Result<Void>> transfer(DataSource source) {
        try {
            var futures = source.openStream().map(part -> supplyAsync(() -> postData(part), executorService)).collect(toList());
            return allOf(futures.toArray(CompletableFuture[]::new)).thenApply((s) -> {
                if (futures.stream().anyMatch(future -> future.getNow(null).failed())) {
                    return Result.failure("Error transferring data");
                }
                return Result.success();
            });
        } catch (Exception e) {
            monitor.severe("Error processing data transfer request: " + requestId, e);
            return CompletableFuture.completedFuture(Result.failure("Error processing data transfer request"));
        }
    }

    /**
     * Retrieves the part from the source endpoint using an HTTP GET.
     */
    private Result<Void> postData(DataSource.Part part) {
        var requestBody = new StreamingRequestBody(part);

        var request = new Request.Builder().url(endpoint + "/" + part.name()).post(requestBody).build();

        try (var response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return Result.success();
            }
            monitor.severe(format("Error received writing HTTP data %s to endpoint %s for request: %s", part.name(), endpoint, request));
        } catch (Exception e) {
            monitor.severe(format("Error writing HTTP data %s to endpoint %s for request: %s", part.name(), endpoint, request), e);
        }
        return Result.failure("Error writing data");
    }

    private HttpDataSink() {
    }

    public static class Builder {
        private HttpDataSink sink;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder endpoint(String endpoint) {
            sink.endpoint = endpoint;
            return this;
        }

        public Builder requestId(String requestId) {
            sink.requestId = requestId;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            sink.httpClient = httpClient;
            return this;
        }

        public Builder executorService(ExecutorService executorService) {
            sink.executorService = executorService;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            sink.monitor = monitor;
            return this;
        }

        public HttpDataSink build() {
            Objects.requireNonNull(sink.endpoint, "endpoint");
            Objects.requireNonNull(sink.requestId, "requestId");
            Objects.requireNonNull(sink.httpClient, "httpClient");
            Objects.requireNonNull(sink.executorService, "executorService");
            return sink;
        }

        private Builder() {
            sink = new HttpDataSink();
        }
    }
}
