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

package org.eclipse.dataspaceconnector.transfer.dataplane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.extension.annotations.WithSpan;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.dataplane.selector.client.DataPlaneSelectorClient;
import org.eclipse.dataspaceconnector.dataplane.spi.response.TransferErrorResponse;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Implementation of a {@link DataPlaneTransferClient} that uses a remote {@link org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager}, that is
 * accessible via REST API.
 */
public class RemoteDataPlaneTransferClient implements DataPlaneTransferClient {
    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private final DataPlaneSelectorClient selectorClient;
    private final String selectorStrategy;
    private final RetryPolicy<Object> retryStrategy;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public RemoteDataPlaneTransferClient(OkHttpClient client, DataPlaneSelectorClient selectorClient, String selectorStrategy, RetryPolicy<Object> retryPolicy, ObjectMapper mapper) {
        this.selectorClient = selectorClient;
        this.selectorStrategy = selectorStrategy;
        retryStrategy = retryPolicy;
        this.client = client;
        this.mapper = mapper;
    }

    @WithSpan
    @Override
    public StatusResult<Void> transfer(DataFlowRequest request) {
        var instance = selectorClient.find(request.getSourceDataAddress(), request.getDestinationDataAddress(), selectorStrategy);
        if (instance == null) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, "Failed to find data plane instance supporting request: " + request.getId());
        }

        RequestBody body;
        try {
            body = RequestBody.create(mapper.writeValueAsString(request), TYPE_JSON);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
        var rq = new Request.Builder().post(body).url(instance.getUrl()).build();

        try (var response = executeRequest(rq)) {
            return handleResponse(response, request.getId());
        }
    }

    @NotNull
    private Response executeRequest(Request rq) {
        return Failsafe.with(retryStrategy).get(() -> client.newCall(rq).execute());
    }

    private StatusResult<Void> handleResponse(Response response, String requestId) {
        if (response.isSuccessful()) {
            return StatusResult.success();
        } else {
            return handleError(response, requestId);
        }
    }

    private StatusResult<Void> handleError(Response response, String requestId) {
        var errorMsg = Optional.ofNullable(response.body())
                .map(this::formatErrorMessage)
                .orElse("null response body");
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, format("Transfer request failed with status code %s for request %s: %s", response.code(), requestId, errorMsg));
    }

    private String formatErrorMessage(ResponseBody body) {
        try {
            var errorResponse = mapper.readValue(body.string(), TransferErrorResponse.class);
            return String.join(", ", errorResponse.getErrors());
        } catch (IOException e) {
            return "failed to read response body";
        }
    }
}
