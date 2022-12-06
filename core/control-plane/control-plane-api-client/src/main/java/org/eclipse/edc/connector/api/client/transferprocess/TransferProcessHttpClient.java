/*
 *  Copyright (c) 2020-2022 Microsoft Corporation
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

package org.eclipse.edc.connector.api.client.transferprocess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.api.client.spi.transferprocess.TransferProcessApiClient;
import org.eclipse.edc.connector.api.client.transferprocess.model.TransferProcessFailRequest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Implementation of {@link TransferProcessApiClient} which talks to the Control Plane Transfer Process via HTTP APIs
 */
public class TransferProcessHttpClient implements TransferProcessApiClient {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");

    private final RetryPolicy<Object> retryPolicy;
    private final OkHttpClient okHttpClient;

    private final ObjectMapper mapper;
    private final Monitor monitor;

    public TransferProcessHttpClient(OkHttpClient okHttpClient, RetryPolicy<Object> retryPolicy, ObjectMapper mapper, Monitor monitor) {
        this.okHttpClient = okHttpClient;
        this.retryPolicy = retryPolicy;
        this.mapper = mapper;
        this.monitor = monitor;
    }


    @Override
    public void completed(DataFlowRequest dataFlowRequest) {
        sendRequest(dataFlowRequest, "complete", null);
    }

    @Override
    public void failed(DataFlowRequest dataFlowRequest, String reason) {
        sendRequest(dataFlowRequest, "fail", TransferProcessFailRequest.Builder.newInstance().errorMessage(reason).build());
    }


    private void sendRequest(DataFlowRequest dataFlowRequest, String action, Object body) {

        if (dataFlowRequest.getCallbackAddress() != null) {
            try {
                var rq = createRequest(buildUrl(dataFlowRequest, action), body);
                try (var response = executeRequest(rq)) {
                    if (!response.isSuccessful()) {
                        monitor.severe(String.format("Failed to send callback request: received %s from the TransferProcess API", response.code()));
                    }
                }

            } catch (Exception e) {
                monitor.severe("Failed to send callback request", e);
            }
        } else {
            monitor.warning(String.format("Missing callback address in DataFlowRequest %s", dataFlowRequest.getId()));
        }
    }

    @NotNull
    private String buildUrl(DataFlowRequest dataFlowRequest, String action) throws URISyntaxException {
        var url = new URI(dataFlowRequest.getCallbackAddress().toString() + "/").resolve(String.format("./transferprocess/%s/%s", dataFlowRequest.getProcessId(), action)).normalize();
        return url.toString();
    }

    @NotNull
    private Response executeRequest(Request rq) {
        return Failsafe.with(retryPolicy).get(() -> okHttpClient.newCall(rq).execute());
    }


    private Request createRequest(String url, Object body) throws JsonProcessingException {
        RequestBody requestBody = null;
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
