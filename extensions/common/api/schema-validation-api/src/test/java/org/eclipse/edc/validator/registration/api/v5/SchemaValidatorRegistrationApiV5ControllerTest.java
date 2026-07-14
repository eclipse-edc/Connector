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

package org.eclipse.edc.validator.registration.api.v5;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistrationService;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class SchemaValidatorRegistrationApiV5ControllerTest extends RestControllerTestBase {

    private final SchemaValidatorRegistrationService service = mock();
    private final TypeTransformerRegistry transformerRegistry = mock();

    private static SchemaValidatorRegistration registration() {
        return SchemaValidatorRegistration.Builder.newInstance()
                .id("id1").version("v5").validatedType("Asset").schema("https://example.com/schema/asset.json").build();
    }

    @Test
    void create_shouldReturnCreated() {
        var registration = registration();
        when(transformerRegistry.transform(any(), eq(SchemaValidatorRegistration.class))).thenReturn(Result.success(registration));
        when(service.create(any())).thenAnswer(i -> ServiceResult.success(i.getArgument(0)));
        when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add("@id", registration.getId()).build()));

        baseRequest()
                .contentType("application/json")
                .body("{ \"version\": \"v5\" }")
                .post("/v5beta/schemavalidators")
                .then()
                .statusCode(200)
                .body("@id", is(registration.getId()));

        verify(service).create(registration);
    }

    @Test
    void create_shouldReturnBadRequest_whenTransformationFails() {
        when(transformerRegistry.transform(any(), eq(SchemaValidatorRegistration.class))).thenReturn(Result.failure("error"));

        baseRequest()
                .contentType("application/json")
                .body("{ }")
                .post("/v5beta/schemavalidators")
                .then()
                .statusCode(400);

        verifyNoInteractions(service);
    }

    @Test
    void get_shouldReturnNotFound_whenMissing() {
        when(service.findById("missing")).thenReturn(null);

        baseRequest()
                .get("/v5beta/schemavalidators/missing")
                .then()
                .statusCode(404);
    }

    @Test
    void getAll_shouldReturnList() {
        var registration = registration();
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(registration)));
        when(transformerRegistry.transform(eq(registration), eq(JsonObject.class)))
                .thenReturn(Result.success(Json.createObjectBuilder().add("@id", "id1").build()));

        baseRequest()
                .get("/v5beta/schemavalidators")
                .then()
                .statusCode(200)
                .body("size()", is(1));
    }

    @Test
    void delete_shouldInvokeService() {
        when(service.deleteById("id1")).thenReturn(ServiceResult.success(registration()));

        baseRequest()
                .delete("/v5beta/schemavalidators/id1")
                .then()
                .statusCode(204);

        verify(service).deleteById(eq("id1"));
    }

    @Override
    protected Object controller() {
        return new SchemaValidatorRegistrationApiV5Controller(service, transformerRegistry);
    }

    private RequestSpecification baseRequest() {
        return given().baseUri("http://localhost:" + port).when();
    }
}
