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

package org.eclipse.edc.connector.dataplane.selector.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelector;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.util.string.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;

/**
 * Implementation of a {@link DataPlaneSelectorClient} that uses a remote {@link DataPlaneSelector}, that is
 * accessible via REST API.
 */
public class RemoteDataPlaneSelectorClient implements DataPlaneSelectorClient {
    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private static final String SELECT_PATH = "/select";
    private final String baseUrl;
    private final EdcHttpClient client;
    private final ObjectMapper mapper;

    public RemoteDataPlaneSelectorClient(EdcHttpClient client, String baseUrl, ObjectMapper mapper) {
        this.baseUrl = baseUrl;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public List<DataPlaneInstance> getAll() {
        try {
            var request = new Request.Builder().get().url(baseUrl).build();

            try (var response = client.execute(request)) {
                var tr = new TypeReference<List<DataPlaneInstance>>() {
                };
                return handleResponse(response, tr, Collections.emptyList());
            }
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
        var request = new Request.Builder().post(body).url(baseUrl + SELECT_PATH).build();

        try {
            try (var response = client.execute(request)) {

                TypeReference<DataPlaneInstance> tr = new TypeReference<>() {
                };
                return handleResponse(response, tr, null);

            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
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
