/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.dataplane.selector.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.common.string.StringUtils;
import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;

/**
 * Implementation of a {@link DataPlaneSelectorClient} that uses a remote {@link org.eclipse.dataspaceconnector.dataplane.selector.DataPlaneSelector}, that is
 * accessible via REST API.
 */
public class RemoteDataPlaneSelectorClient implements DataPlaneSelectorClient {
    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private static final String SELECT_PATH = "/select";
    private final String baseUrl;
    private final RetryPolicy<Object> retryStrategy;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public RemoteDataPlaneSelectorClient(OkHttpClient client, String baseUrl, RetryPolicy<Object> retryPolicy, ObjectMapper mapper) {
        this.baseUrl = baseUrl;
        retryStrategy = retryPolicy;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public List<DataPlaneInstance> getAll() {
        var rq = new Request.Builder().get().url(baseUrl).build();

        try (var response = executeRequest(rq)) {
            var tr = new TypeReference<List<DataPlaneInstance>>() {
            };
            return handleResponse(response, tr, Collections.emptyList());
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public DataPlaneInstance find(DataAddress source, DataAddress destination) {
        var selectionRequest = new HashMap<>();
        selectionRequest.put("source", source);
        selectionRequest.put("destination", destination);

        return selectDataPlane(selectionRequest);
    }

    @Override
    public DataPlaneInstance find(DataAddress source, DataAddress destination, String selectionStrategyName) {
        var selectionRequest = new HashMap<>();
        selectionRequest.put("source", source);
        selectionRequest.put("destination", destination);
        selectionRequest.put("strategy", selectionStrategyName);

        return selectDataPlane(selectionRequest);
    }

    private DataPlaneInstance selectDataPlane(HashMap<Object, Object> selectionRequest) {
        RequestBody body;
        try {
            body = RequestBody.create(mapper.writeValueAsString(selectionRequest), TYPE_JSON);
        } catch (JsonProcessingException e) {
            throw new EdcException(e);
        }
        var rq = new Request.Builder().post(body).url(baseUrl + SELECT_PATH).build();

        try (var response = executeRequest(rq)) {

            TypeReference<DataPlaneInstance> tr = new TypeReference<>() {
            };
            return handleResponse(response, tr, null);

        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @NotNull
    private Response executeRequest(Request rq) throws IOException {
        return Failsafe.with(retryStrategy).get(() -> client.newCall(rq).execute());
    }

    private <R> R handleResponse(Response response, TypeReference<? extends R> tr, R defaultValue) {
        if (response.isSuccessful()) {
            R r = handleSuccess(response, tr);
            return r == null ? defaultValue : r;
        } else {
            return handleError(response);
        }
    }

    private <R> R handleSuccess(Response response, TypeReference<R> tr) {
        try {
            var body = response.body().string();
            if (StringUtils.isNullOrEmpty(body)) {
                return null;
            }

            return mapper.readValue(body, tr);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    private <R> R handleError(Response response) {
        switch (response.code()) {
            case 400:
                throw new IllegalArgumentException("Remote API returned HTTP 400");
            case 404:
                return null;
            default:
                throw new IllegalArgumentException(format("An unknown error happened, HTTP Status = %d", response.code()));
        }
    }

}
