/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.client.transferprocess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.controlplane.api.client.transferprocess.model.TransferProcessFailRequest;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;

/**
 * Implementation of {@link TransferProcessApiClient} which talks to the Control Plane Transfer Process via HTTP APIs
 */
public class TransferProcessHttpClient implements TransferProcessApiClient {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private final ControlApiHttpClient httpClient;
    private final ObjectMapper mapper;
    private final Monitor monitor;

    public TransferProcessHttpClient(ControlApiHttpClient httpClient, ObjectMapper mapper, Monitor monitor) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    @Override
    public StatusResult<Void> completed(DataFlowStartMessage dataFlowStartMessage) {
        var url = dataFlowStartMessage.getCallbackAddress() + "/transferprocess/" + dataFlowStartMessage.getProcessId() + "/complete";
        return sendRequest(url, null);
    }

    @Override
    public StatusResult<Void> failed(DataFlowStartMessage dataFlowStartMessage, String reason) {
        var url = dataFlowStartMessage.getCallbackAddress() + "/transferprocess/" + dataFlowStartMessage.getProcessId() + "/fail";
        var body = TransferProcessFailRequest.Builder.newInstance().errorMessage(reason).build();
        return sendRequest(url, body);
    }

    @Override
    public StatusResult<Void> provisioned(DataFlow dataFlow) {
        var url = dataFlow.getCallbackAddress() + "/transferprocess/" + dataFlow.getId() + "/provisioned";
        return sendRequest(url, dataFlow.provisionedDataAddress());
    }

    private StatusResult<Void> sendRequest(String url, Object body) {
        try {
            var builder = new Request.Builder()
                    .url(url)
                    .post(createRequestBody(body));

            var result = httpClient.execute(builder);
            if (result.failed()) {
                var message = "Failed to send callback request: %s".formatted(result.getFailureDetail());
                monitor.severe(message);
                return StatusResult.failure(ERROR_RETRY, message);
            }
        } catch (Exception e) {
            monitor.severe("Failed to send callback request", e);
            return StatusResult.failure(ERROR_RETRY, "Failed to send callback request: " + e.getMessage());
        }

        return StatusResult.success();
    }

    private @NotNull RequestBody createRequestBody(Object body) throws JsonProcessingException {
        if (body != null) {
            return RequestBody.create(mapper.writeValueAsString(body), TYPE_JSON);
        } else {
            return RequestBody.create("", null);
        }
    }
}
