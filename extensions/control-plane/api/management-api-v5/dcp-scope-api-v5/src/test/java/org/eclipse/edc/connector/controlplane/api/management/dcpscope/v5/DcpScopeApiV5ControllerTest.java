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

package org.eclipse.edc.connector.controlplane.api.management.dcpscope.v5;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class DcpScopeApiV5ControllerTest extends RestControllerTestBase {

    private static final String BASE_URL = "/v5beta/dcpscopes";

    private final DcpScopeRegistry scopeRegistry = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    private static String requestBody() {
        return Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", "https://w3id.org/edc/v0.0.1/ns/"))
                .add("@type", "DcpScope")
                .add("@id", "scope-1")
                .add("value", "org.example.scope")
                .build()
                .toString();
    }

    private static DcpScope scope() {
        return DcpScope.Builder.newInstance().id("scope-1").value("org.example.scope").profile("p").build();
    }

    @BeforeEach
    void setUp() {
        when(transformerRegistry.transform(isA(IdResponse.class), eq(JsonObject.class)))
                .thenAnswer(a -> Result.success(Json.createObjectBuilder().add("@id", ((IdResponse) a.getArgument(0)).getId()).build()));
    }

    @Test
    void create_shouldReturnId() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DcpScope.class))).thenReturn(Result.success(scope()));
        when(scopeRegistry.create(any())).thenReturn(ServiceResult.success());

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody())
                .post(BASE_URL)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("'@id'", is("scope-1"));

        verify(scopeRegistry).create(any());
    }

    @Test
    void create_shouldReturnBadRequest_whenTransformationFails() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DcpScope.class))).thenReturn(Result.failure("invalid"));

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody())
                .post(BASE_URL)
                .then()
                .statusCode(400);

        verify(scopeRegistry, never()).create(any());
    }

    @Test
    void create_shouldReturnConflict_whenAlreadyExists() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DcpScope.class))).thenReturn(Result.success(scope()));
        when(scopeRegistry.create(any())).thenReturn(ServiceResult.conflict("already exists"));

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody())
                .post(BASE_URL)
                .then()
                .statusCode(409);
    }

    @Test
    void update_shouldReturnNoContent() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DcpScope.class))).thenReturn(Result.success(scope()));
        when(scopeRegistry.update(any())).thenReturn(ServiceResult.success());

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody())
                .put(BASE_URL + "/scope-1")
                .then()
                .statusCode(204);

        verify(scopeRegistry).update(any());
    }

    @Test
    void update_shouldReturnNotFound_whenMissing() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DcpScope.class))).thenReturn(Result.success(scope()));
        when(scopeRegistry.update(any())).thenReturn(ServiceResult.notFound("not found"));

        given()
                .port(port)
                .contentType(JSON)
                .body(requestBody())
                .put(BASE_URL + "/scope-1")
                .then()
                .statusCode(404);
    }

    @Test
    void delete_shouldReturnNoContent() {
        when(scopeRegistry.remove("scope-1")).thenReturn(ServiceResult.success());

        given()
                .port(port)
                .delete(BASE_URL + "/scope-1")
                .then()
                .statusCode(204);

        verify(scopeRegistry).remove("scope-1");
    }

    @Test
    void delete_shouldReturnNotFound_whenMissing() {
        when(scopeRegistry.remove(any())).thenReturn(ServiceResult.notFound("not found"));

        given()
                .port(port)
                .delete(BASE_URL + "/scope-1")
                .then()
                .statusCode(404);
    }

    @Test
    void query_shouldReturnScopes() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.max()));
        when(scopeRegistry.query(any())).thenReturn(ServiceResult.success(List.of(scope())));
        when(transformerRegistry.transform(isA(DcpScope.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add("@id", "scope-1").build()));

        given()
                .port(port)
                .contentType(JSON)
                .body(Json.createObjectBuilder().add("@type", "QuerySpec").build().toString())
                .post(BASE_URL + "/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));

        verify(scopeRegistry).query(any());
    }

    @Test
    void query_shouldReturnBadRequest_whenTransformationFails() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.failure("invalid"));

        given()
                .port(port)
                .contentType(JSON)
                .body(Json.createObjectBuilder().add("@type", "QuerySpec").build().toString())
                .post(BASE_URL + "/request")
                .then()
                .statusCode(400);

        verifyNoInteractions(scopeRegistry);
    }

    @Override
    protected Object controller() {
        return new DcpScopeApiV5Controller(scopeRegistry, transformerRegistry, monitor);
    }
}
