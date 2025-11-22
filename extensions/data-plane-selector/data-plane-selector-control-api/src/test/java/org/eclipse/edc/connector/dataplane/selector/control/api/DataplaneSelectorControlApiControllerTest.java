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
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
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

    private final ParticipantContext participantContext = ParticipantContext.Builder.newInstance()
            .participantContextId("participantContextId")
            .identity("participantId")
            .build();

    @Override
    protected Object controller() {
        return new DataplaneSelectorControlApiController(validatorRegistry, typeTransformerRegistry, service, () -> ServiceResult.success(participantContext), clock);
    }

    @Nested
    class Register {

        @Test
        void shouldRegisterDataplane() {
            var dataplaneInstance = DataPlaneInstance.Builder.newInstance().url("http://url")
                    .participantContextId("participantContextId")
                    .build();
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

            var captor = ArgumentCaptor.forClass(DataPlaneInstance.class);
            verify(service).addInstance(captor.capture());

            assertThat(captor.getValue()).usingRecursiveComparison()
                    .isEqualTo(dataplaneInstance);
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

    @Nested
    class Unregister {

        @Test
        void shouldUnregisterInstance() {
            when(service.unregister(any())).thenReturn(ServiceResult.success());
            var instanceId = UUID.randomUUID().toString();

            given()
                    .port(port)
                    .put("/v1/dataplanes/{id}/unregister", instanceId)
                    .then()
                    .statusCode(204);

            verify(service).unregister(instanceId);
        }

        @Test
        void shouldReturnNotFound_whenServiceReturnsNotFound() {
            when(service.unregister(any())).thenReturn(ServiceResult.notFound("not found"));
            var instanceId = UUID.randomUUID().toString();

            given()
                    .port(port)
                    .put("/v1/dataplanes/{id}/unregister", instanceId)
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteInstance() {
            when(service.delete(any())).thenReturn(ServiceResult.success());
            var instanceId = UUID.randomUUID().toString();

            given()
                    .port(port)
                    .delete("/v1/dataplanes/{id}", instanceId)
                    .then()
                    .statusCode(204);

            verify(service).delete(instanceId);
        }

        @Test
        void shouldReturnNotFound_whenServiceReturnsNotFound() {
            when(service.delete(any())).thenReturn(ServiceResult.notFound("not found"));
            var instanceId = UUID.randomUUID().toString();

            given()
                    .port(port)
                    .delete("/v1/dataplanes/{id}", instanceId)
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    class GetAll {

        @Test
        void shouldReturnAllDataplaneInstances() {
            var dataPlane = DataPlaneInstance.Builder.newInstance()
                    .url("http://any-url")
                    .build();
            when(service.getAll()).thenReturn(ServiceResult.success(List.of(dataPlane)));
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class)))
                    .thenReturn(Result.success(Json.createObjectBuilder().build()));

            given()
                    .port(port)
                    .get("/v1/dataplanes")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1));
        }

        @Test
        void shouldIgnoreFailedTransformations() {
            var dataPlane = DataPlaneInstance.Builder.newInstance()
                    .url("http://any-url")
                    .build();
            when(service.getAll()).thenReturn(ServiceResult.success(List.of(dataPlane)));
            when(typeTransformerRegistry.transform(any(), eq(JsonObject.class)))
                    .thenReturn(Result.failure("error"));

            given()
                    .port(port)
                    .get("/v1/dataplanes")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(0));
        }

        @Test
        void shouldReturnInternalServerError_whenServiceFails() {
            when(service.getAll()).thenReturn(ServiceResult.unexpected("error"));

            given()
                    .port(port)
                    .get("/v1/dataplanes")
                    .then()
                    .statusCode(500);
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnDataPlaneInstance() {
            var instance = DataPlaneInstance.Builder.newInstance().url("http://any").build();
            var output = Json.createObjectBuilder().add(ID, "anId").build();
            when(service.findById(any())).thenReturn(ServiceResult.success(instance));
            when(typeTransformerRegistry.transform(any(), any())).thenReturn(Result.success(output));

            given()
                    .port(port)
                    .get("/v1/dataplanes/anId")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("anId"));

            verify(service).findById("anId");
            verify(typeTransformerRegistry).transform(instance, JsonObject.class);
        }

        @Test
        void shouldReturnNotFound_whenInstanceDoesNotExist() {
            when(service.findById(any())).thenReturn(ServiceResult.notFound("not found"));

            given()
                    .port(port)
                    .get("/v1/dataplanes/anId")
                    .then()
                    .statusCode(404)
                    .contentType(JSON);

            verifyNoInteractions(typeTransformerRegistry);
        }

        @Test
        void shouldReturnInternalServerError_whenTransformationFails() {
            var instance = DataPlaneInstance.Builder.newInstance().url("http://any").build();
            when(service.findById(any())).thenReturn(ServiceResult.success(instance));
            when(typeTransformerRegistry.transform(any(), any())).thenReturn(Result.failure("an error"));

            given()
                    .port(port)
                    .get("/v1/dataplanes/anId")
                    .then()
                    .statusCode(500);
        }
    }

}
