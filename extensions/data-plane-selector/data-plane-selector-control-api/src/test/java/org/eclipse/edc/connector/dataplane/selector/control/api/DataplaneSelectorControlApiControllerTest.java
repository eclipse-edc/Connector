/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.selector.control.api;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ApiTest
class DataplaneSelectorControlApiControllerTest extends RestControllerTestBase {

    private final JsonObjectValidatorRegistry validatorRegistry = mock();
    private final TypeTransformerRegistry typeTransformerRegistry = mock();
    private final DataPlaneSelectorService service = mock();
    private final Clock clock = mock();

    @Nested
    class Register {

        @Test
        void shouldRegisterDataplane() {
            var dataplaneInstance = DataPlaneInstance.Builder.newInstance().url("http://url").build();
            var response = Json.createObjectBuilder().add(ID, "id").build();
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(typeTransformerRegistry.transform(any(), eq(DataPlaneInstance.class))).thenReturn(Result.success(dataplaneInstance));
            when(service.addInstance(any())).thenReturn(ServiceResult.success());
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(response));

            given()
                    .port(port)
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/v1/dataplanes")
                    .then()
                    .statusCode(200)
                    .body(ID, is("id"));

            verify(service).addInstance(dataplaneInstance);
        }

        @Test
        void shouldReturnBadRequest_whenValidationFails() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("error", "/path")));

            given()
                    .port(port)
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/v1/dataplanes")
                    .then()
                    .statusCode(400);

            verifyNoInteractions(service, typeTransformerRegistry);
        }

        @Test
        void shouldReturnBadRequest_whenIngressTransformationFails() {
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(typeTransformerRegistry.transform(any(), eq(DataPlaneInstance.class))).thenReturn(Result.failure("error"));

            given()
                    .port(port)
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/v1/dataplanes")
                    .then()
                    .statusCode(400);

            verifyNoInteractions(service);
        }

        @Test
        void shouldFail_whenServiceFails() {
            var dataplaneInstance = DataPlaneInstance.Builder.newInstance().url("http://url").build();
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(typeTransformerRegistry.transform(any(), eq(DataPlaneInstance.class))).thenReturn(Result.success(dataplaneInstance));
            when(service.addInstance(any())).thenReturn(ServiceResult.conflict("conflict"));

            given()
                    .port(port)
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/v1/dataplanes")
                    .then()
                    .statusCode(409);
        }

        @Test
        void shouldFail_whenEgressTransformationFails() {
            var dataplaneInstance = DataPlaneInstance.Builder.newInstance().url("http://url").build();
            when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
            when(typeTransformerRegistry.transform(any(), eq(DataPlaneInstance.class))).thenReturn(Result.success(dataplaneInstance));
            when(service.addInstance(any())).thenReturn(ServiceResult.success());
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("error"));

            given()
                    .port(port)
                    .contentType(JSON)
                    .body(Json.createObjectBuilder().build())
                    .post("/v1/dataplanes")
                    .then()
                    .statusCode(500);
        }
    }

    @Override
    protected Object controller() {
        return new DataplaneSelectorControlApiController(validatorRegistry, typeTransformerRegistry, service, clock);
    }

}
