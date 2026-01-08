/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.signaling.domain.DataFlowPrepareMessage;
import org.eclipse.edc.signaling.domain.DataFlowResponseMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartMessage;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Client that implements the Data Plane Signaling spec
 */
public class DataPlaneSignalingClient {

    private  static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private final DataPlaneInstance dataPlane;
    private final ControlApiHttpClient httpClient;
    private final Supplier<ObjectMapper> objectMapperSupplier;

    public DataPlaneSignalingClient(DataPlaneInstance dataPlane, ControlApiHttpClient httpClient,
                                    Supplier<ObjectMapper> objectMapperSupplier) {
        this.dataPlane = dataPlane;
        this.httpClient = httpClient;
        this.objectMapperSupplier = objectMapperSupplier;
    }

    public StatusResult<DataFlowResponseMessage> prepare(DataFlowPrepareMessage request) {
        var url = "%s/prepare".formatted(dataPlane.getUrl());
        return createRequestBuilder(request, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(this::handleResponse)
                                .orElse(this::failedResult)));
    }

    public StatusResult<DataFlowResponseMessage> start(DataFlowStartMessage request) {
        var url = "%s/start".formatted(dataPlane.getUrl());
        return createRequestBuilder(request, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(this::handleResponse)
                                .orElse(this::failedResult)));
    }

    public StatusResult<Void> suspend(String transferProcessId) {
        var url = "%s/%s/suspend".formatted(dataPlane.getUrl(), transferProcessId);
        var message = DataFlowSuspendMessage.Builder.newInstance().build();
        return createRequestBuilder(message, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(it -> StatusResult.success())
                                .orElse(this::failedResult)));
    }

    public StatusResult<Void> terminate(String transferProcessId) {
        var url = "%s/%s/terminate".formatted(dataPlane.getUrl(), transferProcessId);
        var message = DataFlowSuspendMessage.Builder.newInstance().build();
        return createRequestBuilder(message, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(it -> StatusResult.success())
                                .orElse(this::failedResult)));
    }

    public StatusResult<Void> completed(String flowId) {
        var url = "%s/%s/completed".formatted(dataPlane.getUrl(), flowId);
        var message = DataFlowSuspendMessage.Builder.newInstance().build();
        return createRequestBuilder(message, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(it -> StatusResult.success())
                                .orElse(this::failedResult)));
    }

    public StatusResult<Void> checkAvailability() {
        var requestBuilder = new Request.Builder().get().url(dataPlane.getUrl() + "/");
        return httpClient.request(requestBuilder)
                .flatMap(result -> result.map(it -> StatusResult.success()).orElse(this::failedResult));
    }

    private StatusResult<DataFlowResponseMessage> handleResponse(String responseBody) {
        return Optional.ofNullable(responseBody)
                .map(this::deserializeStartMessage)
                .orElseGet(() -> StatusResult.failure(FATAL_ERROR, "Body missing"));
    }

    private StatusResult<DataFlowResponseMessage> deserializeStartMessage(String responseBody) {
        try {
            var message = objectMapperSupplier.get().readValue(responseBody, DataFlowResponseMessage.class);
            return StatusResult.success(message);
        } catch (IOException e) {
            return StatusResult.failure(FATAL_ERROR, e.getMessage());
        }
    }

    private <T> @NotNull StatusResult<T> failedResult(ServiceFailure failure) {
        return StatusResult.failure(FATAL_ERROR, "Communication with data-plane %s failed: %s".formatted(dataPlane.getId(), failure.getFailureDetail()));
    }

    private StatusResult<Request.Builder> createRequestBuilder(Object message, String url) {
        return this.serialize(message)
                .map(rawBody -> RequestBody.create(rawBody, TYPE_JSON))
                .map(body -> new Request.Builder().post(body).url(url))
                .flatMap(it -> {
                    if (it.succeeded()) {
                        return StatusResult.success(it.getContent());
                    } else {
                        return StatusResult.failure(FATAL_ERROR, it.getFailureDetail());
                    }
                });
    }

    private Result<String> serialize(Object message) {
        try {
            return Result.success(objectMapperSupplier.get().writeValueAsString(message));
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }
}
