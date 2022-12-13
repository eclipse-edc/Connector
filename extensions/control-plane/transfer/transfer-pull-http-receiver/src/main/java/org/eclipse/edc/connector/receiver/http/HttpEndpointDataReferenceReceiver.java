/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.receiver.http;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiver;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.eclipse.edc.util.string.StringUtils.isNullOrBlank;

/**
 * Implementation of a {@link EndpointDataReferenceReceiver} that posts
 * the {@link EndpointDataReference} to an existing http endpoint.
 */
public class HttpEndpointDataReferenceReceiver implements EndpointDataReferenceReceiver {

    private static final MediaType JSON = MediaType.get("application/json");

    private Monitor monitor;
    private EdcHttpClient httpClient;
    private TypeManager typeManager;
    private String endpoint;
    private String authKey;
    private String authToken;

    private HttpEndpointDataReferenceReceiver() {
    }

    @Override
    public CompletableFuture<Result<Void>> send(@NotNull EndpointDataReference edr) {
        var requestBody = RequestBody.create(typeManager.writeValueAsString(edr), JSON);
        var requestBuilder = new Request.Builder().url(endpoint).post(requestBody);
        if (!isNullOrBlank(authKey) && !isNullOrBlank(authToken)) {
            requestBuilder.header(authKey, authToken);
        }
        try (var response = httpClient.execute(requestBuilder.build())) {
            if (response.isSuccessful()) {
                var body = response.body();
                if (body == null) {
                    throw new EdcException(format("Received empty response body when receiving endpoint data reference at uri: %s", endpoint));
                }
                return completedFuture(Result.success());
            } else {
                throw new EdcException(format("Received error code %s when transferring endpoint data reference at uri: %s", response.code(), endpoint));
            }
        } catch (IOException e) {
            return failedFuture(e);
        }
    }

    public static class Builder {
        private final HttpEndpointDataReferenceReceiver receiver;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder monitor(Monitor monitor) {
            receiver.monitor = monitor;
            return this;
        }

        public Builder httpClient(EdcHttpClient httpClient) {
            receiver.httpClient = httpClient;
            return this;
        }

        public Builder typeManager(TypeManager typeManager) {
            receiver.typeManager = typeManager;
            return this;
        }

        public Builder endpoint(String endpoint) {
            receiver.endpoint = endpoint;
            return this;
        }

        public Builder authHeader(String key, String code) {
            receiver.authKey = key;
            receiver.authToken = code;
            return this;
        }

        public HttpEndpointDataReferenceReceiver build() {
            Objects.requireNonNull(receiver.monitor, "monitor");
            Objects.requireNonNull(receiver.endpoint, "endpoint");
            Objects.requireNonNull(receiver.httpClient, "httpClient");
            Objects.requireNonNull(receiver.typeManager, "typeManager");
            return receiver;
        }

        private Builder() {
            receiver = new HttpEndpointDataReferenceReceiver();
        }
    }
}
