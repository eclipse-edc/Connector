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
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.signaling.domain.DataFlowPrepareMessage;
import org.eclipse.edc.signaling.domain.DataFlowResumeMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartMessage;
import org.eclipse.edc.signaling.domain.DataFlowStartedNotificationMessage;
import org.eclipse.edc.signaling.domain.DataFlowStatusMessage;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;

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

    public StatusResult<DataFlowStatusMessage> prepare(DataFlowPrepareMessage request) {
        return send("prepare", request, this::dataFlowStatusMessage);
    }

    public StatusResult<DataFlowStatusMessage> start(DataFlowStartMessage request) {
        return send("start", request, this::dataFlowStatusMessage);
    }

    public StatusResult<Void> suspend(String flowId, DataFlowSuspendMessage message) {
        return send(flowId + "/suspend", message, this::discardResponseBody);
    }

    public StatusResult<DataFlowStatusMessage> resume(String flowId, DataFlowResumeMessage message) {
        return send(flowId + "/resume", message, this::dataFlowStatusMessage);
    }

    public StatusResult<Void> terminate(String flowId) {
        return send(flowId + "/terminate", emptyMap(), this::discardResponseBody);
    }

    public StatusResult<Void> started(String flowId, DataFlowStartedNotificationMessage message) {
        return send(flowId + "/started", message, this::discardResponseBody);
    }

    public StatusResult<Void> completed(String flowId) {
        return send(flowId + "/completed", emptyMap(), this::discardResponseBody);
    }

    private <T> StatusResult<T> send(String path, Object message, Function<ResponseBody, Result<T>> extractBody) {
        return createRequestBuilder(message, dataPlane.getUrl() + "/" + path)
                .compose(builder -> {
                    var response = httpClient.execute(builder.build(), r -> handleResponse(r, extractBody));
                    if (response.succeeded()) {
                        return StatusResult.success(response.getContent());
                    } else {
                        return StatusResult.fatalError(response.getFailureDetail());
                    }
                });
    }

    private <T> Result<T> handleResponse(Response response, Function<ResponseBody, Result<T>> handleResponseBody) {
        try (var responseBody = response.body()) {
            if (!response.isSuccessful()) {
                return Result.failure("Data-plane responded with %d - %s. Response body: %s"
                        .formatted(response.code(), response.message(), responseBody.string()));
            }
            return handleResponseBody.apply(responseBody);

        } catch (IOException e) {
            return Result.failure("Data-plane responded with %d - %s. Cannot read response body: %s"
                    .formatted(response.code(), response.message(), e.getMessage()));
        }
    }

    private Result<Void> discardResponseBody(ResponseBody responseBody) {
        return Result.success();
    }

    private Result<DataFlowStatusMessage> dataFlowStatusMessage(ResponseBody responseBody) {
        try {
            var inputStream = responseBody.byteStream();
            var message = objectMapperSupplier.get().readValue(inputStream, DataFlowStatusMessage.class);
            return Result.success(message);
        } catch (IOException e) {
            return Result.failure("Data-plane responded with %d - %s. Cannot read response body: " + e.getMessage());
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
        var authorizationProfile = dataPlane.getAuthorizationProfile();
        if (authorizationProfile == null) {
            return Result.success(requestBuilder);
        }
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
