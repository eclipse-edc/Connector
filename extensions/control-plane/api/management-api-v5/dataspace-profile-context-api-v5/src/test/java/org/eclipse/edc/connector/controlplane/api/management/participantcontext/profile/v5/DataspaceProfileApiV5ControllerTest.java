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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext.profile.v5;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.protocol.spi.service.DataspaceProfileService;
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
import static jakarta.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataspaceProfileApiV5ControllerTest extends RestControllerTestBase {

    private final DataspaceProfileService service = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    private static DataspaceProfile profile(String name) {
        return DataspaceProfile.Builder.newInstance().name(name).protocolVersion("2025-1").binding("HTTPS")
                .namespace("https://example.com/ns/").build();
    }

    @BeforeEach
    void setup() {
        when(transformerRegistry.transform(isA(DataspaceProfile.class), eq(JsonObject.class)))
                .thenReturn(Result.success(createObjectBuilder().build()));
    }

    private RequestSpecification baseRequest() {
        return given().baseUri("http://localhost:" + port + "/v5beta").when();
    }

    private String body() {
        return Json.createObjectBuilder().add("@type", "DataspaceProfile").build().toString();
    }

    @Test
    void create() {
        var profile = profile("dsp2025_1");
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DataspaceProfile.class))).thenReturn(Result.success(profile));
        when(service.create(profile)).thenReturn(ServiceResult.success(profile));

        baseRequest().contentType(JSON).body(body())
                .post("/dataspaceprofiles")
                .then().statusCode(200).contentType(JSON);

        verify(service).create(profile);
    }

    @Test
    void create_shouldReturnBadRequest_whenTransformationFails() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DataspaceProfile.class))).thenReturn(Result.failure("malformed"));

        baseRequest().contentType(JSON).body(body())
                .post("/dataspaceprofiles")
                .then().statusCode(400);

        verify(service, never()).create(any());
    }

    @Test
    void create_shouldReturnConflict_whenAlreadyExists() {
        var profile = profile("dsp2025_1");
        when(transformerRegistry.transform(isA(JsonObject.class), eq(DataspaceProfile.class))).thenReturn(Result.success(profile));
        when(service.create(profile)).thenReturn(ServiceResult.conflict("exists"));

        baseRequest().contentType(JSON).body(body())
                .post("/dataspaceprofiles")
                .then().statusCode(409);
    }

    @Test
    void query() {
        when(transformerRegistry.transform(isA(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.success(QuerySpec.max()));
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(profile("dsp2025_1"))));

        baseRequest().contentType(JSON).body("{}")
                .post("/dataspaceprofiles/request")
                .then().statusCode(200).contentType(JSON).body("size()", is(1));
    }

    @Test
    void get() {
        when(service.findById("dsp2025_1")).thenReturn(profile("dsp2025_1"));

        baseRequest()
                .get("/dataspaceprofiles/dsp2025_1")
                .then().statusCode(200).contentType(JSON);
    }

    @Test
    void get_shouldReturnNotFound_whenMissing() {
        when(service.findById("missing")).thenReturn(null);

        baseRequest()
                .get("/dataspaceprofiles/missing")
                .then().statusCode(404);
    }

    @Test
    void delete() {
        when(service.deleteById("dsp2025_1")).thenReturn(ServiceResult.success(profile("dsp2025_1")));

        baseRequest()
                .delete("/dataspaceprofiles/dsp2025_1")
                .then().statusCode(204);

        verify(service).deleteById("dsp2025_1");
    }

    @Test
    void delete_shouldReturnNotFound_whenMissing() {
        when(service.deleteById("missing")).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .delete("/dataspaceprofiles/missing")
                .then().statusCode(404);
    }

    @Override
    protected Object controller() {
        return new DataspaceProfileApiV5Controller(service, transformerRegistry, monitor);
    }
}
