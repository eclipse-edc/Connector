/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.receiver.http.dynamic;

import dev.failsafe.RetryPolicy;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceReceiver;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static dev.failsafe.Failsafe.with;
import static java.lang.String.format;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.util.string.StringUtils.isNullOrBlank;

/**
 * Implementation of a {@link EndpointDataReferenceReceiver} that posts
 * the {@link EndpointDataReference} to an existing http endpoint sent by clients when initiating the data transfer.
 */
public class HttpDynamicEndpointDataReferenceReceiver implements EndpointDataReferenceReceiver {


    public static final String HTTP_RECEIVER_ENDPOINT = EDC_NAMESPACE + "receiverHttpEndpoint";

    private static final MediaType JSON = MediaType.get("application/json");

    private Monitor monitor;
    private OkHttpClient httpClient;
    private TypeManager typeManager;
    private RetryPolicy<Object> retryPolicy;

    private TransferProcessStore transferProcessStore;

    private String authKey;
    private String authToken;
    private String fallbackEndpoint;

    private HttpDynamicEndpointDataReferenceReceiver() {
    }

    @Override
    public CompletableFuture<Result<Void>> send(@NotNull EndpointDataReference edr) {

        var processId = transferProcessStore.processIdForDataRequestId(edr.getId());

        if (processId == null) {
            return CompletableFuture.completedFuture(Result.failure(format("Failed to found processId for DataRequestId %s", edr.getId())));
        }
        var transferProcess = transferProcessStore.findById(processId);

        if (transferProcess == null) {
            return CompletableFuture.completedFuture(Result.failure(format("Failed to found transfer process for id %s", processId)));
        }

        var endpoint = transferProcess.getPrivateProperties().get(HTTP_RECEIVER_ENDPOINT);

        if (endpoint == null) {
            endpoint = fallbackEndpoint;
        }

        if (endpoint != null) {
            monitor.debug(format("Sending EDR to %s", endpoint));
            return sendEdr(edr, endpoint);
        } else {
            monitor.debug(format("Missing %s property in the transfer process properties or fallback endpoint in configuration", HTTP_RECEIVER_ENDPOINT));
            return CompletableFuture.completedFuture(Result.success());
        }


    }

    @NotNull
    private CompletableFuture<Result<Void>> sendEdr(@NotNull EndpointDataReference edr, String endpoint) {


        var requestBody = RequestBody.create(typeManager.writeValueAsString(edr), JSON);
        var requestBuilder = new Request.Builder().url(endpoint).post(requestBody);
        if (!isNullOrBlank(authKey) && !isNullOrBlank(authToken)) {
            requestBuilder.header(authKey, authToken);
        }
        try (var response = with(retryPolicy).get(() -> httpClient.newCall(requestBuilder.build()).execute())) {
            if (response.isSuccessful()) {
                return CompletableFuture.completedFuture(Result.success());
            } else {
                return CompletableFuture.completedFuture(Result.failure(format("Received error code %s when transferring endpoint data reference at uri: %s", response.code(), endpoint)));
            }
        }
    }

    public static class Builder {
        private final HttpDynamicEndpointDataReferenceReceiver receiver;

        private Builder() {
            receiver = new HttpDynamicEndpointDataReferenceReceiver();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder monitor(Monitor monitor) {
            receiver.monitor = monitor;
            return this;
        }

        public Builder httpClient(OkHttpClient httpClient) {
            receiver.httpClient = httpClient;
            return this;
        }

        public Builder typeManager(TypeManager typeManager) {
            receiver.typeManager = typeManager;
            return this;
        }

        public Builder retryPolicy(RetryPolicy<Object> retryPolicy) {
            receiver.retryPolicy = retryPolicy;
            return this;
        }


        public Builder transferProcessStore(TransferProcessStore transferProcessStore) {
            receiver.transferProcessStore = transferProcessStore;
            return this;
        }

        public Builder authHeader(String key, String code) {
            receiver.authKey = key;
            receiver.authToken = code;
            return this;
        }

        public Builder fallbackEndpoint(String fallbackEndpoint) {
            receiver.fallbackEndpoint = fallbackEndpoint;
            return this;
        }


        public HttpDynamicEndpointDataReferenceReceiver build() {
            Objects.requireNonNull(receiver.monitor, "monitor");
            Objects.requireNonNull(receiver.httpClient, "httpClient");
            Objects.requireNonNull(receiver.typeManager, "typeManager");
            Objects.requireNonNull(receiver.retryPolicy, "retryPolicy");
            Objects.requireNonNull(receiver.transferProcessStore, "transferProcessStore");
            return receiver;
        }
    }
}
