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

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class RemoteDataPlaneSelectorService implements DataPlaneSelectorService {

    public static final MediaType TYPE_JSON = MediaType.parse("application/json");
    private final ControlApiHttpClient httpClient;
    private final String url;
    private final TypeManager typeManager;
    private final String typeContext;
    private final TypeTransformerRegistry typeTransformerRegistry;
    private final JsonLd jsonLd;

    public RemoteDataPlaneSelectorService(ControlApiHttpClient controlClient, String url, TypeManager typeManager,
                                          String typeContext, TypeTransformerRegistry typeTransformerRegistry,
                                          JsonLd jsonLd) {
        this.httpClient = controlClient;
        this.url = url;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.typeTransformerRegistry = typeTransformerRegistry;
        this.jsonLd = jsonLd;
    }

    @Override
    public ServiceResult<List<DataPlaneInstance>> getAll() {
        var requestBuilder = new Request.Builder().get().url(url);

        return httpClient.request(requestBuilder)
                .compose(this::toJsonArray)
                .map(it -> it.stream()
                        .map(JsonValue::asJsonObject)
                        .map(jo -> jsonLd.expand(jo).compose(joo -> typeTransformerRegistry.transform(joo, DataPlaneInstance.class)))
                        .filter(Result::succeeded)
                        .map(Result::getContent)
                        .toList()
                );
    }

    @Override
    public ServiceResult<DataPlaneInstance> select(@Nullable String selectionStrategy, Predicate<DataPlaneInstance> filter) {
        return ServiceResult.unexpected("DataPlaneSelectorService.select can only be called as embedded in the control-plane");
    }

    @Override
    public ServiceResult<Void> register(DataPlaneInstance instance) {
        var transform = typeTransformerRegistry.transform(instance, JsonObject.class);
        if (transform.failed()) {
            return ServiceResult.badRequest(transform.getFailureDetail());
        }

        var requestBody = Json.createObjectBuilder(transform.getContent())
                .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .build();
        var body = RequestBody.create(requestBody.toString(), TYPE_JSON);

        var requestBuilder = new Request.Builder().post(body).url(url);

        return httpClient.request(requestBuilder).mapEmpty();
    }

    @Override
    public ServiceResult<Void> delete(String instanceId) {
        var requestBuilder = new Request.Builder().delete().url(url + "/" + instanceId);

        return httpClient.request(requestBuilder).mapEmpty();
    }

    @Override
    public ServiceResult<Void> unregister(String instanceId) {
        var requestBuilder = new Request.Builder().put(RequestBody.create(new byte[0])).url("%s/%s/unregister".formatted(url, instanceId));

        return httpClient.request(requestBuilder).mapEmpty();
    }

    @Override
    public ServiceResult<Void> update(DataPlaneInstance instance) {
        return ServiceResult.unexpected("DataPlaneSelectorService.update can only be called as embedded in the control-plane");
    }

    @Override
    public ServiceResult<DataPlaneInstance> findById(String id) {
        var requestBuilder = new Request.Builder().get().url(url + "/" + id);

        return httpClient.request(requestBuilder).compose(this::toJsonObject)
                .compose(it -> jsonLd.expand(it).flatMap(ServiceResult::from))
                .map(it -> typeTransformerRegistry.transform(it, DataPlaneInstance.class).getContent());
    }

    private ServiceResult<JsonObject> toJsonObject(String it) {
        try {
            return ServiceResult.success(typeManager.getMapper(typeContext).readValue(it, JsonObject.class));
        } catch (JsonProcessingException e) {
            return ServiceResult.unexpected("Cannot deserialize response body as JsonObject");
        }
    }

    private ServiceResult<JsonArray> toJsonArray(String it) {
        try {
            return ServiceResult.success(typeManager.getMapper(typeContext).readValue(it, JsonArray.class));
        } catch (JsonProcessingException e) {
            return ServiceResult.unexpected("Cannot deserialize response body as JsonObject");
        }
    }

}
