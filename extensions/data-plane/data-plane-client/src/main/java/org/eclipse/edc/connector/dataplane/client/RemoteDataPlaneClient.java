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

package org.eclipse.edc.connector.dataplane.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.response.TransferErrorResponse;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Implementation of a {@link DataPlaneClient} that uses a remote {@link DataPlaneManager} accessible from a REST API.
 */
public class RemoteDataPlaneClient implements DataPlaneClient {
    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private final DataPlaneSelectorClient selectorClient;
    private final String selectorStrategy;
    private final EdcHttpClient httpClient;
    private final ObjectMapper mapper;

    public RemoteDataPlaneClient(EdcHttpClient httpClient, DataPlaneSelectorClient selectorClient, String selectorStrategy, ObjectMapper mapper) {
        this.selectorClient = Objects.requireNonNull(selectorClient, "Data plane selector client");
        this.selectorStrategy = Objects.requireNonNull(selectorStrategy, "Selector strategy");
        this.httpClient = Objects.requireNonNull(httpClient, "Http client");
        this.mapper = Objects.requireNonNull(mapper, "Object mapper");
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

        try (var response = httpClient.execute(rq)) {
            return handleResponse(response, request.getId());
        } catch (IOException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, e.getMessage());
        }
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
