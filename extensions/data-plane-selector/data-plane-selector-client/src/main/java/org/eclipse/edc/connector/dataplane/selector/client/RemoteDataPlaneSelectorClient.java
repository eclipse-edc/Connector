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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelector;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.util.string.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static jakarta.json.Json.createObjectBuilder;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;

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
    private final TypeTransformerRegistry typeTransformerRegistry;

    public RemoteDataPlaneSelectorClient(EdcHttpClient client, String baseUrl, ObjectMapper mapper, TypeTransformerRegistry typeTransformerRegistry) {
        this.baseUrl = baseUrl;
        this.client = client;
        this.mapper = mapper;
        this.typeTransformerRegistry = typeTransformerRegistry;
    }

    @Override
    public List<DataPlaneInstance> getAll() {
        try {
            var request = new Request.Builder().get().url(baseUrl).build();

            try (var response = client.execute(request)) {

                var tr = new TypeReference<List<JsonObject>>() {
                };
                return handleResponse(response, tr, Collections.emptyList()).stream()
                        .map(j -> typeTransformerRegistry.transform(j, DataPlaneInstance.class))
                        .filter(Result::succeeded)
                        .map(Result::getContent)
                        .toList();
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public DataPlaneInstance find(DataAddress source, DataAddress destination) {
        return selectDataPlane(source, destination, null);
    }

    @Override
    public DataPlaneInstance find(DataAddress source, DataAddress destination, String selectionStrategyName) {
        return selectDataPlane(source, destination, selectionStrategyName);
    }

    private DataPlaneInstance selectDataPlane(DataAddress source, DataAddress destination, String selectionStrategy) {
        var srcAddress = typeTransformerRegistry.transform(source, JsonObject.class).orElseThrow(f -> new EdcException(f.getFailureDetail()));
        var dstAddress = typeTransformerRegistry.transform(destination, JsonObject.class).orElseThrow(f -> new EdcException(f.getFailureDetail()));
        var jsonObject = Json.createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, EDC_NAMESPACE + "SelectionRequest")
                .add(EDC_NAMESPACE + "source", srcAddress)
                .add(EDC_NAMESPACE + "destination", dstAddress);
        if (selectionStrategy != null) {
            jsonObject.add(EDC_NAMESPACE + "strategy", selectionStrategy);
        }
        RequestBody body = RequestBody.create(jsonObject.build().toString(), TYPE_JSON);

        var request = new Request.Builder().post(body).url(baseUrl + SELECT_PATH).build();

        try {
            try (var response = client.execute(request)) {

                var tr = new TypeReference<JsonObject>() {
                };
                var jo = handleResponse(response, tr, null);
                return jo != null ?
                        typeTransformerRegistry.transform(jo, DataPlaneInstance.class)
                                .orElseThrow(f -> new EdcException(f.getFailureDetail())) : null;
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
        return switch (response.code()) {
            case 400 -> throw new IllegalArgumentException("Remote API returned HTTP 400");
            case 404 -> null;
            default ->
                    throw new IllegalArgumentException(format("An unknown error happened, HTTP Status = %d", response.code()));
        };
    }

}
