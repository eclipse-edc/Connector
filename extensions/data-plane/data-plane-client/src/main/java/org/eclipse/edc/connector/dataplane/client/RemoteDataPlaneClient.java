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
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.connector.dataplane.spi.response.TransferErrorResponse;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.io.IOException;
import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of a {@link DataPlaneClient} that uses a remote {@link DataPlaneManager} accessible from a REST API.
 */
public class RemoteDataPlaneClient implements DataPlaneClient {
    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private final EdcHttpClient httpClient;
    private final ObjectMapper mapper;
    private final DataPlaneInstance dataPlane;

    public RemoteDataPlaneClient(EdcHttpClient httpClient, ObjectMapper mapper, DataPlaneInstance dataPlane) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.dataPlane = dataPlane;
    }

    @WithSpan
    @Override
    public StatusResult<DataFlowResponseMessage> start(DataFlowStartMessage dataFlowStartMessage) {
        RequestBody body;
        try {
            body = RequestBody.create(mapper.writeValueAsString(dataFlowStartMessage), TYPE_JSON);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
        var request = new Request.Builder().post(body).url(dataPlane.getUrl()).build();

        try (var response = httpClient.execute(request)) {
            var result = handleResponse(response, dataFlowStartMessage.getId());

            if (result.failed()) {
                return StatusResult.failure(result.getFailure().status(), result.getFailureDetail());
            } else {
                return StatusResult.success(DataFlowResponseMessage.Builder.newInstance().build());
            }
        } catch (IOException e) {
            return StatusResult.failure(FATAL_ERROR, e.getMessage());
        }
    }

    @Override
    public StatusResult<Void> suspend(String transferProcessId) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public StatusResult<Void> terminate(String transferProcessId) {
        var request = new Request.Builder().delete().url(dataPlane.getUrl() + "/" + transferProcessId).build();

        try (var response = httpClient.execute(request)) {
            return handleResponse(response, transferProcessId);
        } catch (IOException e) {
            return StatusResult.<Void>failure(FATAL_ERROR, e.getMessage());
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
        return StatusResult.failure(FATAL_ERROR, format("Transfer request failed with status code %s for request %s: %s", response.code(), requestId, errorMsg));
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
