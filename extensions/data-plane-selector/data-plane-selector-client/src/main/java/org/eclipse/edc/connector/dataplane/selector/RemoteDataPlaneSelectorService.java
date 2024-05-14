/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
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
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;

public class RemoteDataPlaneSelectorService implements DataPlaneSelectorService {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private static final String SELECT_PATH = "/select";
    private static final TypeReference<JsonObject> JSON_OBJECT = new TypeReference<>() {
    };
    private static final TypeReference<List<JsonObject>> LIST_JSON_OBJECT = new TypeReference<>() {
    };
    private final EdcHttpClient httpClient;
    private final String url;
    private final ObjectMapper mapper;
    private final TypeTransformerRegistry typeTransformerRegistry;
    private final String selectionStrategy;

    public RemoteDataPlaneSelectorService(EdcHttpClient httpClient, String url, ObjectMapper mapper,
                                          TypeTransformerRegistry typeTransformerRegistry, String selectionStrategy) {
        this.httpClient = httpClient;
        this.url = url;
        this.mapper = mapper;
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.selectionStrategy = selectionStrategy;
    }

    @Override
    public List<DataPlaneInstance> getAll() {
        try {
            var request = new Request.Builder().get().url(url).build();

            try (var response = httpClient.execute(request)) {

                return handleResponse(response, LIST_JSON_OBJECT, Collections.emptyList()).stream()
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
    public DataPlaneInstance select(DataAddress source, DataAddress destination, String selectionStrategy, String transferType) {
        var srcAddress = typeTransformerRegistry.transform(source, JsonObject.class).orElseThrow(f -> new EdcException(f.getFailureDetail()));
        var dstAddress = typeTransformerRegistry.transform(destination, JsonObject.class).orElseThrow(f -> new EdcException(f.getFailureDetail()));
        var jsonObject = Json.createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, EDC_NAMESPACE + "SelectionRequest")
                .add(EDC_NAMESPACE + "source", srcAddress)
                .add(EDC_NAMESPACE + "destination", dstAddress);

        if (selectionStrategy != null) {
            jsonObject.add(EDC_NAMESPACE + "strategy", selectionStrategy);
        } else {
            jsonObject.add(EDC_NAMESPACE + "strategy", this.selectionStrategy);
        }

        if (transferType != null) {
            jsonObject.add(EDC_NAMESPACE + "transferType", transferType);
        }

        var body = RequestBody.create(jsonObject.build().toString(), TYPE_JSON);

        var request = new Request.Builder().post(body).url(url + SELECT_PATH).build();

        try {
            try (var response = httpClient.execute(request)) {

                var jo = handleResponse(response, JSON_OBJECT, null);
                return jo != null ?
                        typeTransformerRegistry.transform(jo, DataPlaneInstance.class)
                                .orElseThrow(f -> new EdcException(f.getFailureDetail())) : null;
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public ServiceResult<Void> addInstance(DataPlaneInstance instance) {
        var transform = typeTransformerRegistry.transform(instance, JsonObject.class);
        if (transform.failed()) {
            return ServiceResult.badRequest(transform.getFailureDetail());
        }

        var requestBody = Json.createObjectBuilder(transform.getContent())
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .build();
        var body = RequestBody.create(requestBody.toString(), TYPE_JSON);

        var request = new Request.Builder().post(body).url(url).build();

        try {
            try (var response = httpClient.execute(request)) {

                handleResponse(response, null, null);
                return ServiceResult.success();
            }
        } catch (IOException e) {
            return ServiceResult.badRequest(e.getMessage());
        }
    }

    private <R> R handleResponse(Response response, TypeReference<? extends R> tr, R defaultValue) {
        try (var responseBody = response.body()) {
            var bodyAsString = responseBody == null ? null : responseBody.string();
            if (response.isSuccessful()) {
                if (StringUtils.isNullOrEmpty(bodyAsString)) {
                    return null;
                }

                if (tr != null) {
                    var r = mapper.readValue(bodyAsString, tr);
                    return r == null ? defaultValue : r;
                } else {
                    return null;
                }

            } else {
                return switch (response.code()) {
                    case 400 -> throw new IllegalArgumentException("Remote API returned HTTP 400. " + bodyAsString);
                    case 404 -> null;
                    default ->
                            throw new IllegalArgumentException(format("An unknown error happened, HTTP Status = %d", response.code()));
                };
            }
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

}
