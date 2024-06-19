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
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Implementation of a {@link DataPlaneClient} that uses a remote {@link DataPlaneManager} accessible from a REST API.
 *
 * @deprecated replaced by data-plane-signaling.
 */
@Deprecated(since = "0.6.0")
public class RemoteDataPlaneClient implements DataPlaneClient {
    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private final ControlApiHttpClient httpClient;
    private final ObjectMapper mapper;
    private final DataPlaneInstance dataPlane;

    public RemoteDataPlaneClient(ControlApiHttpClient httpClient, ObjectMapper mapper, DataPlaneInstance dataPlane) {
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
        var builder = new Request.Builder().post(body).url(dataPlane.getUrl());

        return httpClient.execute(builder)
                .map(it -> StatusResult.success(DataFlowResponseMessage.Builder.newInstance().build()))
                .orElse(f -> StatusResult.failure(FATAL_ERROR, f.getFailureDetail()));
    }

    @Override
    public StatusResult<Void> suspend(String transferProcessId) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public StatusResult<Void> terminate(String transferProcessId) {
        var builder = new Request.Builder().delete().url(dataPlane.getUrl() + "/" + transferProcessId);

        return httpClient.execute(builder)
                .map(it -> StatusResult.success())
                .orElse(f -> StatusResult.failure(FATAL_ERROR, f.getFailureDetail()));
    }

    @Override
    public StatusResult<Void> checkAvailability() {
        throw new UnsupportedOperationException("feature not implemented for deprecated client");
    }
}
