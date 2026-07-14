/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.document.cache.api.v5;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.CachedDocumentService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class CachedDocumentApiV5ControllerTest extends RestControllerTestBase {

    private final CachedDocumentService service = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    private static CachedDocument context() {
        return CachedDocument.Builder.newInstance()
                .id("id1")
                .url("https://example.com/context.jsonld")
                .content("{}")
                .build();
    }

    @Test
    void create_shouldReturnCreatedContext() {
        var context = context();
        when(transformerRegistry.transform(any(), eq(CachedDocument.class))).thenReturn(Result.success(context));
        when(service.create(any())).thenAnswer(i -> ServiceResult.success(i.getArgument(0)));
        when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add("@id", context.getId()).build()));

        baseRequest()
                .contentType("application/json")
                .body("{ \"url\": \"https://example.com/context.jsonld\" }")
                .post("/v5beta/cacheddocuments")
                .then()
                .statusCode(200)
                .body("@id", is(context.getId()));

        verify(service).create(context);
    }

    @Test
    void create_shouldReturnBadRequest_whenTransformationFails() {
        when(transformerRegistry.transform(any(), eq(CachedDocument.class))).thenReturn(Result.failure("error"));

        baseRequest()
                .contentType("application/json")
                .body("{ }")
                .post("/v5beta/cacheddocuments")
                .then()
                .statusCode(400);

        verifyNoInteractions(service);
    }

    @Test
    void get_shouldReturnNotFound_whenMissing() {
        when(service.findById("missing")).thenReturn(null);

        baseRequest()
                .get("/v5beta/cacheddocuments/missing")
                .then()
                .statusCode(404);
    }

    @Test
    void get_shouldReturnContext() {
        var context = context();
        when(service.findById("id1")).thenReturn(context);
        when(transformerRegistry.transform(eq(context), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add("@id", "id1").build()));

        baseRequest()
                .get("/v5beta/cacheddocuments/id1")
                .then()
                .statusCode(200)
                .body("'@id'", is("id1"));
    }

    @Test
    void getAll_shouldReturnList() {
        var context = context();
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(context)));
        when(transformerRegistry.transform(eq(context), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add("@id", "id1").build()));

        baseRequest()
                .get("/v5beta/cacheddocuments")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    void delete_shouldInvokeService() {
        when(service.deleteById("id1")).thenReturn(ServiceResult.success(context()));

        baseRequest()
                .delete("/v5beta/cacheddocuments/id1")
                .then()
                .statusCode(204);

        verify(service).deleteById(eq("id1"));
    }

    @Test
    void update_shouldSetIdFromPath() {
        var context = context();
        when(transformerRegistry.transform(any(), eq(CachedDocument.class))).thenReturn(Result.success(context));
        when(service.update(any())).thenAnswer(i -> ServiceResult.success(i.getArgument(0)));
        when(transformerRegistry.transform(any(CachedDocument.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add("@id", "id1").build()));

        baseRequest()
                .contentType("application/json")
                .body("{ \"url\": \"https://example.com/context.jsonld\" }")
                .put("/v5beta/cacheddocuments/id1")
                .then()
                .statusCode(204);

        verify(transformerRegistry).transform(isA(JsonObject.class), eq(CachedDocument.class));
        verify(service).update(any());
    }

    @Override
    protected Object controller() {
        return new CachedDocumentApiV5Controller(service, transformerRegistry);
    }

    private RequestSpecification baseRequest() {
        return given().baseUri("http://localhost:" + port).when();
    }
}
