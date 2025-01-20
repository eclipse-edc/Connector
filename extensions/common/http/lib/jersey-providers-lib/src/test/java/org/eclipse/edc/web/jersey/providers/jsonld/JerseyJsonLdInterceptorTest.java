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

package org.eclipse.edc.web.jersey.providers.jsonld;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class JerseyJsonLdInterceptorTest extends RestControllerTestBase {

    private static final String SCOPE = "scope";
    private final JsonLd jsonLd = mock();
    private final JerseyJsonLdInterceptor interceptor = new JerseyJsonLdInterceptor(jsonLd, typeManager, "test", SCOPE);

    @Test
    void expansion_shouldSucceed_whenInputIsJsonObject() {
        when(jsonLd.expand(any())).thenReturn(Result.success(expandedJson()));

        given()
                .port(port)
                .contentType(JSON)
                .body(compactedJson())
                .post("/create/json-object")
                .then()
                .statusCode(204);

        verify(jsonLd).expand(compactedJson());
    }

    @Test
    void expansion_shouldReturnBadRequest_whenExpansionFails() {
        when(jsonLd.expand(any())).thenReturn(Result.failure("expansion failure"));

        given()
                .port(port)
                .contentType(JSON)
                .body(compactedJson())
                .post("/create/json-object")
                .then()
                .statusCode(400);

        verify(jsonLd).expand(compactedJson());
    }

    @Test
    void expansion_shouldNotHappen_whenInputIsNullJsonObject() {
        given()
                .port(port)
                .contentType(JSON)
                .post("/create/json-object")
                .then()
                .statusCode(204);

        verifyNoInteractions(jsonLd);
    }

    @Test
    void expansion_shouldNotHappen_whenInputIsNotJsonObject() {
        given()
                .port(port)
                .contentType(JSON)
                .body(notJsonObject())
                .post("/create/not-json-object")
                .then()
                .statusCode(204);

        verifyNoInteractions(jsonLd);
    }

    @Test
    void compaction_single_shouldSucceed_whenOutputIsJsonObject() {
        when(jsonLd.compact(any(), eq(SCOPE))).thenReturn(Result.success(compactedJson()));

        given()
                .port(port)
                .accept(JSON)
                .get("/get/single/json-object")
                .then()
                .statusCode(200)
                .body("compacted-key", is("compacted-value"));

        verify(jsonLd).compact(expandedJson(), SCOPE);
    }

    @Test
    void compaction_single_shouldReturnInternalServerError_whenCompactionFails() {
        when(jsonLd.compact(any(), eq(SCOPE))).thenReturn(Result.failure("compaction failure"));

        given()
                .port(port)
                .accept(JSON)
                .get("/get/single/json-object")
                .then()
                .statusCode(500);
    }

    @Test
    void compaction_single_shouldNotHappen_whenOutputIsNotJsonObject() {
        given()
                .port(port)
                .accept(JSON)
                .get("/get/single/not-json-object")
                .then()
                .statusCode(200);

        verifyNoInteractions(jsonLd);
    }

    @Test
    void compaction_multiple_shouldSucceed_whenOutputIsJsonObject() {
        when(jsonLd.compact(any(), eq(SCOPE))).thenReturn(Result.success(compactedJson()));

        given()
                .port(port)
                .accept(JSON)
                .get("/get/multiple/json-object")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].compacted-key", is("compacted-value"));

        verify(jsonLd).compact(expandedJson(), SCOPE);
    }

    @Test
    void compaction_multiple_shouldReturnInternalServerError_whenCompactionFails() {
        when(jsonLd.compact(any(), eq(SCOPE))).thenReturn(Result.failure("compaction failure"));

        given()
                .port(port)
                .accept(JSON)
                .get("/get/multiple/json-object")
                .then()
                .statusCode(500);
    }

    @Test
    void compaction_multiple_shouldNotHappen_whenOutputIsNotJsonObject() {
        given()
                .port(port)
                .accept(JSON)
                .get("/get/multiple/not-json-object")
                .then()
                .statusCode(200);

        verifyNoInteractions(jsonLd);
    }

    @Override
    protected Object controller() {
        return new TestController();
    }

    @Override
    protected Object additionalResource() {
        return interceptor;
    }

    private JsonObject expandedJson() {
        return Json.createObjectBuilder().add("expanded-key", "expanded-value").add("bau", 3).build();
    }

    private JsonObject compactedJson() {
        return Json.createObjectBuilder().add("compacted-key", "compacted-value").add("bau", 3).build();
    }

    private Map<String, String> notJsonObject() {
        return Map.of("key", "value");
    }

    @Path("/")
    public class TestController {

        @POST
        @Path("/create/json-object")
        public void createJsonObject(JsonObject jsonObject) {
            if (jsonObject != null && !jsonObject.equals(expandedJson())) {
                throw new RuntimeException("expansion not happened");
            }
        }

        @POST
        @Path("/create/not-json-object")
        public void createNotJsonObject(Map<String, String> notJsonObject) {

        }

        @GET
        @Path("/get/single/json-object")
        public JsonObject getSingleJsonObject() {
            return expandedJson();
        }

        @GET
        @Path("/get/single/not-json-object")
        public Map<String, String> getSingleNotJsonObject() {
            return notJsonObject();
        }

        @GET
        @Path("/get/multiple/json-object")
        public JsonArray getMultipleJsonObject() {
            return Json.createArrayBuilder().add(expandedJson()).build();
        }

        @GET
        @Path("/get/multiple/not-json-object")
        public List<Map<String, String>> getMultipleNotJsonObject() {
            return List.of(notJsonObject());
        }

    }
}
