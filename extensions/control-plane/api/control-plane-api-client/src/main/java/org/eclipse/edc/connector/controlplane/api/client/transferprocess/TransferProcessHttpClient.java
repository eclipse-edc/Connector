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
import org.eclipse.edc.connector.controlplane.api.client.spi.transferprocess.TransferProcessApiClient;
import org.eclipse.edc.connector.controlplane.api.client.transferprocess.model.TransferProcessFailRequest;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;

import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusIsNotIn;

/**
 * Implementation of {@link TransferProcessApiClient} which talks to the Control Plane Transfer Process via HTTP APIs
 */
public class TransferProcessHttpClient implements TransferProcessApiClient {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private final EdcHttpClient httpClient;
    private final ObjectMapper mapper;
    private final Monitor monitor;

    public TransferProcessHttpClient(EdcHttpClient httpClient, ObjectMapper mapper, Monitor monitor) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    @Override
    public Result<Void> completed(DataFlowStartMessage dataFlowStartMessage) {
        return sendRequest(dataFlowStartMessage, "complete", null);
    }

    @Override
    public Result<Void> failed(DataFlowStartMessage dataFlowStartMessage, String reason) {
        return sendRequest(dataFlowStartMessage, "fail", TransferProcessFailRequest.Builder.newInstance().errorMessage(reason).build());
    }

    private Result<Void> sendRequest(DataFlowStartMessage dataFlowStartMessage, String action, Object body) {

        if (dataFlowStartMessage.getCallbackAddress() != null) {
            try {
                var request = createRequest(buildUrl(dataFlowStartMessage, action), body);
                try (var response = httpClient.execute(request, List.of(retryWhenStatusIsNotIn(200, 204)))) {
                    if (!response.isSuccessful()) {
                        var message = "Failed to send callback request: received %s from the TransferProcess API"
                                .formatted(response.code());
                        monitor.severe(message);
                        return Result.failure(message);
                    }
                }

            } catch (Exception e) {
                monitor.severe("Failed to send callback request", e);
                return Result.failure("Failed to send callback request: " + e.getMessage());
            }
        } else {
            monitor.warning(String.format("Missing callback address in DataFlowRequest %s", dataFlowStartMessage.getId()));
        }
        return Result.success();

    }

    @NotNull
    private String buildUrl(DataFlowStartMessage dataFlowStartMessage, String action) {
        var callbackAddress = dataFlowStartMessage.getCallbackAddress();
        var url = URI.create(callbackAddress + "/").resolve(String.format("./transferprocess/%s/%s", dataFlowStartMessage.getProcessId(), action)).normalize();
        return url.toString();
    }

    private Request createRequest(String url, Object body) throws JsonProcessingException {
        RequestBody requestBody;
        if (body != null) {
            requestBody =
                    RequestBody.create(mapper.writeValueAsString(body), TYPE_JSON);
        } else {
            requestBody = RequestBody.create("", null);
        }
        return new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
    }
}
