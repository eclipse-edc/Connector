/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of a {@link DataPlaneClient} that uses a remote {@link DataPlaneManager} accessible from a REST API using
 * the data plane signaling protocol.
 */
public class DataPlaneSignalingClient implements DataPlaneClient {
    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private final ControlApiHttpClient httpClient;
    private final String typeContext;
    private final DataPlaneInstance dataPlane;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLd;

    private final String jsonLdScope;
    private final TypeManager typeManager;

    public DataPlaneSignalingClient(ControlApiHttpClient httpClient, TypeTransformerRegistry transformerRegistry, JsonLd jsonLd, String jsonLdScope,
                                    TypeManager typeManager, String typeContext, DataPlaneInstance dataPlane) {
        this.httpClient = httpClient;
        this.transformerRegistry = transformerRegistry;
        this.jsonLd = jsonLd;
        this.jsonLdScope = jsonLdScope;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.dataPlane = dataPlane;
    }

    @Override
    public StatusResult<DataFlowResponseMessage> provision(DataFlowProvisionMessage message) {
        var url = "%s/prepare".formatted(dataPlane.getUrl());
        return createRequestBuilder(message, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(this::handleResponse)
                                .orElse(failure -> failedResult(message.getProcessId(), failure))));
    }

    @WithSpan
    @Override
    public StatusResult<DataFlowResponseMessage> start(DataFlowStartMessage message) {
        var url = "%s/start".formatted(dataPlane.getUrl());
        return createRequestBuilder(message, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(this::handleResponse)
                                .orElse(failure -> failedResult(message.getProcessId(), failure))));
    }

    @Override
    public StatusResult<Void> suspend(String transferProcessId) {
        var url = "%s/%s/suspend".formatted(dataPlane.getUrl(), transferProcessId);
        var message = DataFlowSuspendMessage.Builder.newInstance().build();
        return createRequestBuilder(message, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(it -> StatusResult.success())
                                .orElse(failure -> failedResult(transferProcessId, failure))));
    }

    @Override
    public StatusResult<Void> terminate(String transferProcessId) {
        var url = "%s/%s/terminate".formatted(dataPlane.getUrl(), transferProcessId);
        var message = DataFlowTerminateMessage.Builder.newInstance().build();
        return createRequestBuilder(message, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(it -> StatusResult.success())
                                .orElse(failure -> failedResult(transferProcessId, failure))));
    }

    @Override
    public StatusResult<Void> checkAvailability() {
        var requestBuilder = new Request.Builder().get().url(dataPlane.getUrl() + "/check");
        return httpClient.request(requestBuilder)
                .flatMap(result -> result.map(it -> StatusResult.success())
                        .orElse(failure -> failedResult(null, failure)));
    }

    private StatusResult<Request.Builder> createRequestBuilder(Object message, String url) {
        return transformerRegistry.transform(message, JsonObject.class)
                .compose(this::compact)
                .compose(this::serializeMessage)
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

    private StatusResult<DataFlowResponseMessage> handleResponse(String responseBody) {
        return Optional.ofNullable(responseBody)
                .map(this::deserializeStartMessage)
                .orElseGet(() -> StatusResult.failure(FATAL_ERROR, "Body missing"));
    }

    private StatusResult<DataFlowResponseMessage> deserializeStartMessage(String responseBody) {
        try {
            var jsonObject = typeManager.getMapper(typeContext).readValue(responseBody, JsonObject.class);
            var result = jsonLd.expand(jsonObject)
                    .compose(expanded -> transformerRegistry.transform(expanded, DataFlowResponseMessage.class));
            if (result.succeeded()) {
                return StatusResult.success(result.getContent());
            } else {
                return StatusResult.failure(FATAL_ERROR, result.getFailureDetail());
            }
        } catch (IOException e) {
            return StatusResult.failure(FATAL_ERROR, e.getMessage());
        }
    }

    private <T> @NotNull StatusResult<T> failedResult(String processId, ServiceFailure failure) {
        return StatusResult.failure(FATAL_ERROR, format("Transfer request for process %s failed: %s", processId, failure.getFailureDetail()));
    }

    private Result<JsonObject> compact(JsonObject object) {
        return jsonLd.compact(object, jsonLdScope);
    }

    private Result<String> serializeMessage(Object message) {
        try {
            return Result.success(typeManager.getMapper(typeContext).writeValueAsString(message));
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }

}
