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
import okhttp3.Response;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.signaling.domain.DataFlowPrepareMessage;
import org.eclipse.edc.signaling.domain.DataFlowResponseMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartedNotificationMessage;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Client that implements the Data Plane Signaling spec
 */
public class DataPlaneSignalingClient {

    private  static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private final DataPlaneInstance dataPlane;
    private final EdcHttpClient httpClient;
    private final Supplier<ObjectMapper> objectMapperSupplier;
    private final SignalingAuthorizationRegistry authorizationRegistry;

    public DataPlaneSignalingClient(DataPlaneInstance dataPlane, EdcHttpClient httpClient,
                                    Supplier<ObjectMapper> objectMapperSupplier,
                                    SignalingAuthorizationRegistry authorizationRegistry) {
        this.dataPlane = dataPlane;
        this.httpClient = httpClient;
        this.objectMapperSupplier = objectMapperSupplier;
        this.authorizationRegistry = authorizationRegistry;
    }

    public StatusResult<DataFlowResponseMessage> prepare(DataFlowPrepareMessage request) {
        var url = "%s/prepare".formatted(dataPlane.getUrl());
        return createRequestBuilder(request, url)
                .compose(builder -> execute(builder, this::handleResponse));
    }

    public StatusResult<DataFlowResponseMessage> start(DataFlowStartMessage request) {
        var url = "%s/start".formatted(dataPlane.getUrl());
        return createRequestBuilder(request, url)
                .compose(builder -> execute(builder, this::handleResponse));
    }

    public StatusResult<Void> suspend(String flowId) {
        return sendMessage(flowId, "suspend", DataFlowSuspendMessage.Builder.newInstance().build());
    }

    public StatusResult<Void> terminate(String flowId) {
        return sendMessage(flowId, "terminate", emptyMap());
    }

    public StatusResult<Void> started(String flowId, DataFlowStartedNotificationMessage message) {
        return sendMessage(flowId, "started", message);
    }

    public StatusResult<Void> completed(String flowId) {
        return sendMessage(flowId, "completed", emptyMap());
    }

    private StatusResult<Void> sendMessage(String flowId, String name, Object message) {
        var url = dataPlane.getUrl() + "/" + flowId + "/" + name;
        return createRequestBuilder(message, url)
                .compose(builder -> execute(builder, r -> Result.success(null)));
    }

    private @NotNull <T> StatusResult<T> execute(Request.Builder builder, Function<Response, Result<T>> extractBody) {
        var response = httpClient.execute(builder.build(), extractBody);
        if (response.succeeded()) {
            return StatusResult.success(response.getContent());
        } else {
            return StatusResult.fatalError(response.getFailureDetail());
        }
    }

    private Result<DataFlowResponseMessage> handleResponse(Response response) {
        if (!response.isSuccessful()) {
            return Result.failure("Data-plane responded with %d - %s".formatted(response.code(), response.message()));
        }

        try {
            var inputStream = response.body().byteStream();
            var message = objectMapperSupplier.get().readValue(inputStream, DataFlowResponseMessage.class);
            return Result.success(message);
        } catch (IOException e) {
            return Result.failure("Cannot parse response body: " + e.getMessage());
        }
    }

    private StatusResult<Request.Builder> createRequestBuilder(Object message, String url) {
        return this.serialize(message)
                .map(rawBody -> RequestBody.create(rawBody, TYPE_JSON))
                .map(body -> new Request.Builder().post(body).url(url))
                .compose(this::setupAuthorization)
                .flatMap(it -> {
                    if (it.succeeded()) {
                        return StatusResult.success(it.getContent());
                    } else {
                        return StatusResult.failure(FATAL_ERROR, it.getFailureDetail());
                    }
                });
    }

    private Result<Request.Builder> setupAuthorization(Request.Builder requestBuilder) {
        var profiles = dataPlane.getAuthorizationProfiles();
        if (profiles.isEmpty()) {
            return Result.success(requestBuilder);
        }
        var authorizationProfile = profiles.get(0);
        var authorization = authorizationRegistry.findByType(authorizationProfile.type());
        if (authorization == null) {
            return Result.failure("Authorization %s not supported".formatted(authorizationProfile.type()));
        }

        return authorization
                .evaluate(authorizationProfile)
                .map(header -> requestBuilder.addHeader(header.key(), header.value()));
    }

    private Result<String> serialize(Object message) {
        try {
            return Result.success(objectMapperSupplier.get().writeValueAsString(message));
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }
}
