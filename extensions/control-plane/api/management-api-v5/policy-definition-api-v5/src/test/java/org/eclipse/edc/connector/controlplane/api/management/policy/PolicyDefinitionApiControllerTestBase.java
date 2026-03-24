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

package org.eclipse.edc.connector.controlplane.api.management.policy;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyEvaluationPlanRequest;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyValidationResult;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.policy.engine.spi.plan.PolicyEvaluationPlan;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public abstract class PolicyDefinitionApiControllerTestBase extends RestControllerTestBase {

    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final PolicyDefinitionService service = mock();
    protected final AuthorizationService authorizationService = mock();
    private final String participantContextId = "test-participant-context-id";

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

    private RequestSpecification baseRequest(String participantContextId) {
        return given()
                .baseUri("http://localhost:" + port + "/" + versionPath() + "/participants/" + participantContextId)
                .when();
    }

    protected abstract String versionPath();

    @NotNull
    private PolicyDefinition.Builder createPolicyDefinition() {
        var policy = Policy.Builder.newInstance().build();

        return PolicyDefinition.Builder.newInstance()
                .id("policyDefinitionId")
                .createdAt(1234)
                .policy(policy);
    }

    @Nested
    class EvaluationPlane {
        @Test
        void createEvaluationPlan() {

            var policyScope = "scope";
            var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();
            var plan = PolicyEvaluationPlan.Builder.newInstance().build();
            var response = Json.createObjectBuilder().build();
            var body = Json.createObjectBuilder().add("policyScope", policyScope).build();

            when(service.findById(any())).thenReturn(policyDefinition);
            when(service.createEvaluationPlan(policyScope, policyDefinition.getPolicy())).thenReturn(ServiceResult.success(plan));

            when(transformerRegistry.transform(any(JsonObject.class), eq(PolicyEvaluationPlanRequest.class)))
                    .thenReturn(Result.success(new PolicyEvaluationPlanRequest(policyScope)));

            when(transformerRegistry.transform(any(PolicyEvaluationPlan.class), eq(JsonObject.class))).thenReturn(Result.success(response));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(body)
                    .post("/policydefinitions/id/evaluationplan")
                    .then()
                    .statusCode(200)
                    .contentType(JSON);
        }

        @Test
        void createEvaluationPlan_fails_whenPolicyNotFound() {

            var policyScope = "scope";
            var body = Json.createObjectBuilder().add("policyScope", policyScope).build();

            when(service.findById(any())).thenReturn(null);
            when(transformerRegistry.transform(any(JsonObject.class), eq(PolicyEvaluationPlanRequest.class)))
                    .thenReturn(Result.success(new PolicyEvaluationPlanRequest(policyScope)));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(body)
                    .post("/policydefinitions/id/evaluationplan")
                    .then()
                    .statusCode(404);
        }

        @Test
        void createEvaluationPlan_authorizationFailed() {

            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));
            var policyScope = "scope";
            var body = Json.createObjectBuilder().add("policyScope", policyScope).build();

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(body.toString())
                    .post("/policydefinitions/id/evaluationplan")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    class Validate {
        @Test
        void validate_shouldReturnNotFound_whenNotFound() {
            when(service.findById(any())).thenReturn(null);

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/policydefinitions/id/validate")
                    .then()
                    .statusCode(404);
        }

        @Test
        void validate_shouldReturnValid_whenValidationSucceed() {

            var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();

            when(service.findById(any())).thenReturn(policyDefinition);
            when(service.validate(policyDefinition.getPolicy())).thenReturn(ServiceResult.success());
            when(transformerRegistry.transform(any(PolicyValidationResult.class), eq(JsonObject.class))).then(answer -> {
                PolicyValidationResult result = answer.getArgument(0);
                var response = Json.createObjectBuilder()
                        .add("isValid", result.isValid())
                        .add("errors", Json.createArrayBuilder(result.errors()))
                        .build();
                return Result.success(response);
            });

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/policydefinitions/id/validate")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("isValid", is(true))
                    .body("errors.size()", is(0));
        }

        @Test
        void validate_shouldReturnInvalidValid_whenValidationFails() {

            var policyDefinition = PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).build();


            when(service.findById(any())).thenReturn(policyDefinition);
            when(service.validate(policyDefinition.getPolicy())).thenReturn(ServiceResult.badRequest(List.of("error1", "error2")));
            when(transformerRegistry.transform(any(PolicyValidationResult.class), eq(JsonObject.class))).then(answer -> {
                PolicyValidationResult result = answer.getArgument(0);
                var response = Json.createObjectBuilder()
                        .add("isValid", result.isValid())
                        .add("errors", Json.createArrayBuilder(result.errors()))
                        .build();
                return Result.success(response);
            });

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/policydefinitions/id/validate")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("isValid", is(false))
                    .body("errors.size()", is(2));
        }

        @Test
        void validate_authorizationFailed() {
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .post("/policydefinitions/id/validate")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    class FindById {
        @Test
        void get_shouldReturnPolicyDefinition() {
            var policyDefinition = createPolicyDefinition().build();
            var expandedBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
            when(service.findById(any())).thenReturn(policyDefinition);
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedBody));

            baseRequest(participantContextId)
                    .get("/policydefinitions/id")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("id", is("id"))
                    .body("createdAt", is(1234));
            verify(service).findById("id");
            verify(transformerRegistry).transform(policyDefinition, JsonObject.class);
        }

        @Test
        void get_shouldReturnNotFound_whenNotFound() {
            when(service.findById(any())).thenReturn(null);

            baseRequest(participantContextId)
                    .get("/policydefinitions/id")
                    .then()
                    .statusCode(404)
                    .contentType(JSON);
            verifyNoInteractions(transformerRegistry);
        }

        @Test
        void get_shouldReturnNotFound_whenTransformFails() {
            when(service.findById(any())).thenReturn(createPolicyDefinition().build());
            when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));

            baseRequest(participantContextId)
                    .get("/policydefinitions/id")
                    .then()
                    .statusCode(404)
                    .contentType(JSON);
        }

        @Test
        void get_authorizationFailed() {
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .get("/policydefinitions/id")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    class Search {
        @Test
        void search_shouldReturnQueriedPolicyDefinitions() {
            var querySpec = QuerySpec.none();
            var policyDefinition = createPolicyDefinition().id("id").build();
            var expandedResponseBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.success(List.of(policyDefinition)));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedResponseBody));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/policydefinitions/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1))
                    .body("[0].id", is("id"))
                    .body("[0].createdAt", is(1234));

            verify(transformerRegistry).transform(isA(JsonObject.class), eq(QuerySpec.class));
            verify(service).search(argThat(s -> s.getOffset() == querySpec.getOffset() &&
                    s.getFilterExpression().stream().anyMatch(c -> c.getOperandLeft().equals("participantContextId") &&
                            c.getOperator().equals("=") &&
                            c.getOperandRight().equals(participantContextId))));
            verify(transformerRegistry).transform(policyDefinition, JsonObject.class);
        }

        @Test
        void search_shouldReturn400_whenInvalidQuery() {
            var requestBody = Json.createObjectBuilder()
                    .add("offset", -1)
                    .build();
            when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.failure("failure"));


            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/policydefinitions/request")
                    .then()
                    .statusCode(400);

            verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
            verifyNoInteractions(service);
            verifyNoMoreInteractions(transformerRegistry);
        }

        @Test
        void search_shouldReturnBadRequest_whenQuerySpecTransformFails() {
            var requestBody = Json.createObjectBuilder().build();
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.failure("error"));

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/policydefinitions/request")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
            verifyNoInteractions(service);
        }

        @Test
        void search_shouldReturnBadRequest_whenServiceReturnsBadRequest() {
            var querySpec = QuerySpec.none();
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.badRequest("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/policydefinitions/request")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
        }

        @Test
        void search_shouldFilterOutResults_whenTransformFails() {
            var querySpec = QuerySpec.none();
            var policyDefinition = createPolicyDefinition().id("id").build();
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.search(any())).thenReturn(ServiceResult.success(List.of(policyDefinition)));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("error"));
            var requestBody = Json.createObjectBuilder().build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/policydefinitions/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(0));
        }

        @Test
        void search_authorizationFailed() {
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .body("{}")
                    .contentType(JSON)
                    .post("/policydefinitions/request")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    class Delete {
        @Test
        void delete_shouldCallService() {
            var policyDefinition = createPolicyDefinition().build();
            when(service.deleteById(any())).thenReturn(ServiceResult.success(policyDefinition));

            baseRequest(participantContextId)
                    .delete("/policydefinitions/id")
                    .then()
                    .statusCode(204);

            verify(service).deleteById("id");
        }

        @Test
        void delete_shouldReturnNotFound_whenNotFound() {
            when(service.deleteById(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest(participantContextId)
                    .delete("/policydefinitions/id")
                    .then()
                    .statusCode(404);
        }

        @Test
        void delete_authorizationFailed() {
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .delete("/policydefinitions/id")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    class Update {
        @Test
        void update_shouldCallService() {
            var policyDefinition = createPolicyDefinition().build();
            when(transformerRegistry.transform(any(), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
            when(service.update(any())).thenReturn(ServiceResult.success(policyDefinition));
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .put("/policydefinitions/id")
                    .then()
                    .statusCode(204);
            verify(transformerRegistry).transform(isA(JsonObject.class), eq(PolicyDefinition.class));
            verify(service).update(policyDefinition);
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

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .put("/policydefinitions/id")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }

        @Test
        void update_shouldReturnNotFound_whenNotFound() {
            var policyDefinition = createPolicyDefinition().build();
            when(transformerRegistry.transform(any(), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
            when(service.update(any())).thenReturn(ServiceResult.notFound("not found"));
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .put("/policydefinitions/id")
                    .then()
                    .statusCode(404);
        }

        @Test
        void update_authorizationFailed() {
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .put("/policydefinitions/id")
                    .then()
                    .statusCode(403);
        }
    }

    @Nested
    class Create {
        @Test
        void create_shouldReturnDefinitionId() {
            var policyDefinition = createPolicyDefinition().id("policyDefinitionId").createdAt(1234).build();
            var response = Json.createObjectBuilder()
                    .add("id", policyDefinition.getId())
                    .add("createdAt", policyDefinition.getCreatedAt())
                    .build();

            when(transformerRegistry.transform(any(), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
            when(service.create(any())).thenReturn(ServiceResult.success(policyDefinition));
            when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class))).thenReturn(Result.success(response));

            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/policydefinitions")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("id", is("policyDefinitionId"))
                    .body("createdAt", is(1234));

            verify(transformerRegistry).transform(isA(JsonObject.class), eq(PolicyDefinition.class));
            verify(service).create(policyDefinition);
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

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/policydefinitions")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
            verifyNoInteractions(service);
        }

        @Test
        void create_shouldReturnConflict_whenItAlreadyExists() {
            var policyDefinition = createPolicyDefinition().id("policyDefinitionId").createdAt(1234).build();
            when(transformerRegistry.transform(any(), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
            when(service.create(any())).thenReturn(ServiceResult.conflict("already exists"));
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest(participantContextId)
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/policydefinitions")
                    .then()
                    .statusCode(409)
                    .contentType(JSON);
        }

        @Test
        void create_authorizationFailed() {
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();
            when(authorizationService.authorize(any(), any(), any(), any()))
                    .thenReturn(ServiceResult.unauthorized("unauthorized"));

            baseRequest(participantContextId)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/policydefinitions")
                    .then()
                    .statusCode(403);
        }
    }
}
