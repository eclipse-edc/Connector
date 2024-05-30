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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.api.auth.spi.ControlClientAuthenticationProvider;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowSuspendMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowTerminateMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of a {@link DataPlaneClient} that uses a remote {@link DataPlaneManager} accessible from a REST API using
 * the data plane signaling protocol.
 */
public class DataPlaneSignalingClient implements DataPlaneClient {
    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private final EdcHttpClient httpClient;
    private final DataPlaneInstance dataPlane;
    private final ControlClientAuthenticationProvider authenticationProvider;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLd;

    private final ObjectMapper mapper;

    public DataPlaneSignalingClient(EdcHttpClient httpClient, TypeTransformerRegistry transformerRegistry, JsonLd jsonLd,
                                    ObjectMapper mapper, DataPlaneInstance dataPlane,
                                    ControlClientAuthenticationProvider authenticationProvider) {
        this.httpClient = httpClient;
        this.transformerRegistry = transformerRegistry;
        this.jsonLd = jsonLd;
        this.mapper = mapper;
        this.dataPlane = dataPlane;
        this.authenticationProvider = authenticationProvider;
    }

    @WithSpan
    @Override
    public StatusResult<DataFlowResponseMessage> start(DataFlowStartMessage message) {
        return Optional.ofNullable(dataPlane)
                .map(instance -> send(message, instance.getUrl().toString(), message.getProcessId(), this::handleStartResponse))
                .orElseGet(() -> StatusResult.failure(FATAL_ERROR, noDataPlaneInstanceFound(message)));
    }

    @Override
    public StatusResult<Void> suspend(String transferProcessId) {
        var url = "%s/%s/suspend".formatted(dataPlane.getUrl(), transferProcessId);
        var message = DataFlowSuspendMessage.Builder.newInstance().build();
        return send(message, url, transferProcessId, r -> StatusResult.success());
    }

    @Override
    public StatusResult<Void> terminate(String transferProcessId) {
        var url = "%s/%s/terminate".formatted(dataPlane.getUrl(), transferProcessId);
        var message = DataFlowTerminateMessage.Builder.newInstance().build();
        return send(message, url, transferProcessId, r -> StatusResult.success());
    }

    private String noDataPlaneInstanceFound(DataFlowStartMessage message) {
        var source = message.getSourceDataAddress().getType();
        var destination = message.getDestinationDataAddress().getType();
        var processId = message.getProcessId();
        return "Unable to process transfer %s: No data plane found for source: %s and destination: %s".formatted(processId, source, destination);
    }

    private <T> StatusResult<T> send(Object message, String url, String processId, Function<Response, StatusResult<T>> handleStartResponse) {
        var requestBuilder = transformerRegistry.transform(message, JsonObject.class)
                .compose(jsonLd::compact)
                .compose(this::serializeMessage)
                .map(rawBody -> RequestBody.create(rawBody, TYPE_JSON))
                .map(body -> new Request.Builder().post(body).url(url));

        if (requestBuilder.succeeded()) {
            var builder = requestBuilder.getContent();
            authenticationProvider.authenticationHeaders().forEach(builder::header);
            try (var response = httpClient.execute(builder.build())) {
                if (response.isSuccessful()) {
                    return handleStartResponse.apply(response);
                } else {
                    // TODO: extract body and add it on t
                    var string = response.body().string();

                    return StatusResult.failure(FATAL_ERROR, format("Transfer request failed with status code %s for request %s", response.code(), processId));
                }
            } catch (IOException e) {
                return StatusResult.failure(FATAL_ERROR, e.getMessage());
            }
        } else {
            return StatusResult.failure(FATAL_ERROR, requestBuilder.getFailureDetail());
        }
    }

    private StatusResult<DataFlowResponseMessage> handleStartResponse(Response response) {
        try (var body = response.body()) {
            return Optional.ofNullable(body)
                    .map(this::deserializeStartMessage)
                    .orElseGet(() -> StatusResult.failure(FATAL_ERROR, "Body missing"));
        }
    }

    private StatusResult<DataFlowResponseMessage> deserializeStartMessage(ResponseBody body) {
        try {
            var jsonObject = mapper.readValue(body.string(), JsonObject.class);
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

    private Result<String> serializeMessage(Object message) {
        try {
            return Result.success(mapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            return Result.failure(e.getMessage());
        }
    }

}
