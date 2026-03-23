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

package org.eclipse.edc.connector.controlplane.api.management.participantcontext;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_CREATED_AT;
import static org.eclipse.edc.api.model.IdResponse.ID_RESPONSE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public abstract class ParticipantContextApiControllerTestBase extends RestControllerTestBase {

    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final ParticipantContextService service = mock();
    protected final AuthorizationService authorizationService = mock();

    @BeforeEach
    void setup() {
        when(transformerRegistry.transform(isA(IdResponse.class), eq(JsonObject.class))).thenAnswer(a -> {
            var idResponse = (IdResponse) a.getArgument(0);
            return Result.success(createObjectBuilder()
                    .add(TYPE, ID_RESPONSE_TYPE)
                    .add(ID, idResponse.getId())
                    .add(ID_RESPONSE_CREATED_AT, idResponse.getCreatedAt())
                    .build()
            );
        });
        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(ServiceResult.success());
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/" + versionPath())
                .when();
    }

    protected abstract String versionPath();

    private ParticipantContext createParticipantContext() {
        return createParticipantContextBuilder()
                .participantContextId(UUID.randomUUID().toString())
                .identity(UUID.randomUUID().toString())
                .properties(Map.of())
                .build();
    }

    private ParticipantContext.Builder createParticipantContextBuilder() {
        return ParticipantContext.Builder.newInstance();
    }

    @Nested
    class FindById {
        @Test
        void get() {
            var participantContext = createParticipantContext();
            var expandedBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
            when(service.getParticipantContext(any())).thenReturn(ServiceResult.success(participantContext));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedBody));

            baseRequest()
                    .get("/participants/" + participantContext.getParticipantContextId())
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("id", is("id"))
                    .body("createdAt", is(1234));
            verify(service).getParticipantContext(participantContext.getParticipantContextId());
            verify(transformerRegistry).transform(participantContext, JsonObject.class);
        }

        @Test
        void get_shouldReturnNotFound_whenNotFound() {
            when(service.getParticipantContext(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest()
                    .get("/participants/id")
                    .then()
                    .statusCode(404)
                    .contentType(JSON);
            verifyNoInteractions(transformerRegistry);
        }

    }

    @Nested
    class Search {
        @Test
        void search() {
            var participantContext = createParticipantContext();
            var expandedResponseBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
            when(service.search(any())).thenReturn(ServiceResult.success(List.of(participantContext)));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedResponseBody));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .get("/participants")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1))
                    .body("[0].id", is("id"))
                    .body("[0].createdAt", is(1234));

            verify(service).search(argThat(querySpec -> querySpec.getOffset() == 0 && querySpec.getLimit() == 50));
            verify(transformerRegistry).transform(participantContext, JsonObject.class);
        }

        @Test
        void search_shouldReturnBadRequest_whenServiceReturnsBadRequest() {
            when(service.search(any())).thenReturn(ServiceResult.badRequest("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .get("/participants")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
        }

        @Test
        void search_shouldFilterOutResults_whenTransformFails() {
            var querySpec = QuerySpec.none();
            var participantContext = createParticipantContext();
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.success(List.of(participantContext)));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .get("/participants")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(0));
        }

    }

    @Nested
    class Delete {
        @Test
        void delete_shouldCallService() {
            var participantContext = createParticipantContext();
            when(service.deleteParticipantContext(any())).thenReturn(ServiceResult.success());

            baseRequest()
                    .delete("/participants/" + participantContext.getParticipantContextId())
                    .then()
                    .statusCode(204);

            verify(service).deleteParticipantContext(participantContext.getParticipantContextId());
        }

        @Test
        void delete_shouldReturnNotFound_whenNotFound() {
            when(service.deleteParticipantContext(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest()
                    .delete("/participants/id")
                    .then()
                    .statusCode(404);
        }

    }

    @Nested
    class Update {
        @Test
        void update_shouldCallService() {
            var participantContext = createParticipantContext();
            when(transformerRegistry.transform(any(), eq(ParticipantContext.class))).thenReturn(Result.success(participantContext));

            when(service.getParticipantContext(any())).thenReturn(ServiceResult.success(participantContext));
            when(service.updateParticipantContext(any())).thenReturn(ServiceResult.success());
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .put("/participants/" + participantContext.getParticipantContextId())
                    .then()
                    .statusCode(204);
            verify(transformerRegistry).transform(isA(JsonObject.class), eq(ParticipantContext.class));
            verify(service).updateParticipantContext(participantContext);
        }

        @Test
        void update_shouldReturnBadRequest_whenTransformationFails() {
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .put("/participants/id")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }

        @Test
        void update_shouldReturnNotFound_whenNotFound() {
            var participantContext = createParticipantContext();
            when(transformerRegistry.transform(any(), eq(ParticipantContext.class))).thenReturn(Result.success(participantContext));
            when(service.updateParticipantContext(participantContext)).thenReturn(ServiceResult.notFound("not found"));
            var requestBody = Json.createObjectBuilder()
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .put("/participants/id")
                    .then()
                    .statusCode(404);
        }

    }

    @Nested
    class Create {
        @Test
        void create() {
            var participantContext = createParticipantContext();
            var response = Json.createObjectBuilder()
                    .add("id", participantContext.getParticipantContextId())
                    .add("createdAt", participantContext.getCreatedAt())
                    .build();

            when(transformerRegistry.transform(any(), eq(ParticipantContext.class))).thenReturn(Result.success(participantContext));

            when(service.createParticipantContext(any())).thenReturn(ServiceResult.success(participantContext));
            when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class))).thenReturn(Result.success(response));

            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/participants")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("id", is(participantContext.getParticipantContextId()))
                    .body("createdAt", is(participantContext.getCreatedAt()));

            verify(transformerRegistry).transform(isA(JsonObject.class), eq(ParticipantContext.class));
            verify(service).createParticipantContext(participantContext);
        }

        @Test
        void create_shouldReturnBadRequest_whenTransformationFails() {
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/participants")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
            verifyNoInteractions(service);
        }

        @Test
        void create_shouldReturnConflict_whenItAlreadyExists() {
            var participantContext = createParticipantContext();
            when(transformerRegistry.transform(any(), eq(ParticipantContext.class))).thenReturn(Result.success(participantContext));
            when(service.createParticipantContext(any())).thenReturn(ServiceResult.conflict("already exists"));
            var requestBody = Json.createObjectBuilder()
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/participants")
                    .then()
                    .statusCode(409)
                    .contentType(JSON);
        }

    }
}
