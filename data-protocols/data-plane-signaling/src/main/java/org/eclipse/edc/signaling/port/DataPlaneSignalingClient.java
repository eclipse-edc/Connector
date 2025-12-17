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
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Client that implements the Data Plane Signaling spec
 */
public class DataPlaneSignalingClient implements DataPlaneClient {

    private  static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private final DataPlaneInstance dataPlane;
    private final ControlApiHttpClient httpClient;
    private final Supplier<ObjectMapper> objectMapperSupplier;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLd;
    private final String jsonLdScope;

    public DataPlaneSignalingClient(DataPlaneInstance dataPlane, ControlApiHttpClient httpClient,
                                    Supplier<ObjectMapper> objectMapperSupplier, TypeTransformerRegistry typeTransformerRegistry,
                                    JsonLd jsonLd, String jsonLdScope) {
        this.dataPlane = dataPlane;
        this.httpClient = httpClient;
        this.objectMapperSupplier = objectMapperSupplier;
        transformerRegistry = typeTransformerRegistry;
        this.jsonLd = jsonLd;
        this.jsonLdScope = jsonLdScope;
    }

    @Override
    public StatusResult<DataFlowResponseMessage> prepare(DataFlowProvisionMessage request) {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, "not implemented");
    }

    @Override
    public StatusResult<DataFlowResponseMessage> start(DataFlowStartMessage request) {
        var url = "%s/start".formatted(dataPlane.getUrl());
        return createRequestBuilder(request, url)
                .compose(builder -> httpClient.request(builder)
                        .flatMap(result -> result.map(this::handleResponse)
                                .orElse(this::failedResult)));
    }

    @Override
    public StatusResult<Void> suspend(String transferProcessId) {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, "not implemented");
    }

    @Override
    public StatusResult<Void> terminate(String transferProcessId) {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, "not implemented");
    }

    @Override
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

    private Result<JsonObject> compact(JsonObject object) {
        return jsonLd.compact(object, jsonLdScope);
    }

    private Result<String> serializeMessage(Object message) {
        try {
            return Result.success(objectMapperSupplier.get().writeValueAsString(message));
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }

}
