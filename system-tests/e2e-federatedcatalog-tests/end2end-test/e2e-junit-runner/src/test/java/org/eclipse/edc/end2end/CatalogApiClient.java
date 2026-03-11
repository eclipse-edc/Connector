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

package org.eclipse.edc.end2end;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.String.format;

class CatalogApiClient {
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final MediaType JSON = MediaType.parse("application/json");
    private final String managementBaseUrl;
    private final String catalogBaseUrl;
    private final ObjectMapper mapper;
    private final Supplier<JsonLd> jsonLdSupplier;
    private final TypeTransformerRegistry typeTransformerRegistry;

    CatalogApiClient(Endpoint catalogManagement, Endpoint connectorManagement,
                     ObjectMapper mapper, Supplier<JsonLd> jsonLdSupplier,
                     TypeTransformerRegistry typeTransformerRegistry) {
        this.mapper = mapper;
        this.jsonLdSupplier = jsonLdSupplier;
        this.typeTransformerRegistry = typeTransformerRegistry;
        managementBaseUrl = "http://localhost:%s%s".formatted(connectorManagement.port(), connectorManagement.path());
        catalogBaseUrl = "http://localhost:%s%s".formatted(catalogManagement.port(), catalogManagement.path());
    }

    Result<String> postAsset(JsonObject entry) {
        return postObjectWithId(createPostRequest(entry, managementBaseUrl + "/v3/assets"));
    }

    Result<String> postPolicy(String policyJsonLd) {
        return postObjectWithId(createPostRequest(policyJsonLd, managementBaseUrl + "/v3/policydefinitions"));
    }

    Result<String> postContractDefinition(JsonObject definition) {
        return postObjectWithId(createPostRequest(definition, managementBaseUrl + "/v3/contractdefinitions"));
    }

    public String queryCatalogs(JsonObject querySpec) {
        var rq = createPostRequest(querySpec, catalog("/v1alpha/catalog/query"));

        try (var response = getClient().newCall(rq).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            }
            throw new RuntimeException(format("Error getting catalog: %s", response));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Catalog> deserializeCatalogs(String json) {
        try {
            return mapper.readValue(json, LIST_TYPE_REFERENCE).stream()
                    .map(m -> jsonLdSupplier.get().expand(Json.createObjectBuilder(m).build())
                            .compose(jsonObject -> typeTransformerRegistry.transform(jsonObject, Catalog.class))
                            .orElseThrow(f -> new EdcException(f.getFailureDetail()))
                    )
                    .toList();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String catalog(String path) {
        return catalogBaseUrl + path;
    }

    @NotNull
    private Result<String> postObjectWithId(Request policy) {
        try (var response = getClient()
                .newCall(policy)
                .execute()) {
            var stringbody = response.body().string();
            return response.isSuccessful() ?
                    Result.success(fromJson(stringbody, JsonObject.class).getString("@id")) :
                    Result.failure(response.message());


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T fromJson(String string, Class<T> clazz) {
        try {
            return mapper.readValue(string, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Request createPostRequest(Object body, String path) {
        return new Request.Builder().url(path).post(RequestBody.create(asJson(body), JSON)).build();
    }

    private String asJson(Object entry) {
        try {
            return entry instanceof String ? (String) entry : mapper.writeValueAsString(entry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private OkHttpClient getClient() {
        return new OkHttpClient();
    }
}
