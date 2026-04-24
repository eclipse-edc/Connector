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

package org.eclipse.edc.connector.controlplane.api.management.cel;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.model.CelExpressionTestRequest;
import org.eclipse.edc.policy.cel.model.CelExpressionTestResponse;
import org.eclipse.edc.policy.cel.service.CelPolicyExpressionService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public abstract class CelExpressionApiControllerTestBase extends RestControllerTestBase {

    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final CelPolicyExpressionService service = mock();

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port + "/" + versionPath())
                .when();
    }

    protected abstract String versionPath();

    private CelExpression celExpression() {
        return CelExpression.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .leftOperand("leftOperand")
                .expression("expr")
                .description("description")
                .build();
    }

    private CelExpression celExpressionTestRequest() {
        return CelExpression.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .leftOperand("leftOperand")
                .expression("expr")
                .description("description")
                .build();
    }

    private ParticipantContextConfiguration.Builder createParticipantContextBuilder() {
        return ParticipantContextConfiguration.Builder.newInstance();
    }

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
    }

    @Nested
    class Create {
        @Test
        void create() {
            var expr = celExpression();
            when(transformerRegistry.transform(any(), eq(CelExpression.class))).thenReturn(Result.success(expr));

            when(service.create(any())).thenReturn(ServiceResult.success());
            var requestBody = Json.createObjectBuilder()
                    .add("policy", Json.createObjectBuilder()
                            .add(CONTEXT, "context")
                            .add(TYPE, "Set")
                            .build())
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/celexpressions")
                    .then()
                    .statusCode(200);
            verify(transformerRegistry).transform(isA(JsonObject.class), eq(CelExpression.class));
            verify(service).create(expr);
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
                    .post("/celexpressions")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }
    }

    @Nested
    class FindById {
        @Test
        void findById() {
            var expr = celExpression();
            var expandedBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
            when(service.findById(any())).thenReturn(ServiceResult.success(expr));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedBody));

            baseRequest()
                    .get("/celexpressions/" + expr.getId())
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("id", is("id"))
                    .body("createdAt", is(1234));
            verify(service).findById(expr.getId());
            verify(transformerRegistry).transform(expr, JsonObject.class);
        }

        @Test
        void get_shouldReturnNotFound_whenNotFound() {
            when(service.findById(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest()
                    .get("/celexpressions/id")
                    .then()
                    .statusCode(404)
                    .contentType(JSON);
            verifyNoInteractions(transformerRegistry);
        }

    }

    @Nested
    class Update {

        @Test
        void update() {
            var expr = celExpression();
            when(transformerRegistry.transform(any(), eq(CelExpression.class))).thenReturn(Result.success(expr));

            when(service.update(any())).thenReturn(ServiceResult.success());

            baseRequest()
                    .body("{}")
                    .contentType(JSON)
                    .put("/celexpressions/" + expr.getId())
                    .then()
                    .statusCode(204);
            verify(transformerRegistry).transform(isA(JsonObject.class), eq(CelExpression.class));
            verify(service).update(expr);
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
                    .put("/celexpressions/1")
                    .then()
                    .statusCode(400);
            verifyNoInteractions(service);
        }
    }

    @Nested
    class Query {
        @Test
        void query() {
            var querySpec = QuerySpec.none();
            var expr = celExpression();
            var expandedBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
            when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
            when(service.query(any())).thenReturn(ServiceResult.success(List.of(expr)));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedBody));

            baseRequest()
                    .contentType(JSON)
                    .body("{}")
                    .post("/celexpressions/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1))
                    .body("[0].id", is("id"))
                    .body("[0].createdAt", is(1234));

            verify(service).query(querySpec);
            verify(transformerRegistry).transform(expr, JsonObject.class);
        }

        @Test
        void query_whenTransformFails() {
            when(service.findById(any())).thenReturn(ServiceResult.notFound("not found"));
            when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.failure("failure"));

            baseRequest()
                    .body("{}")
                    .contentType(JSON)
                    .post("/celexpressions/request")
                    .then()
                    .statusCode(400)
                    .contentType(JSON);
        }

    }

    @Nested
    class Delete {

        @Test
        void delete() {
            var expr = celExpression();
            when(service.delete(any())).thenReturn(ServiceResult.success());

            baseRequest()
                    .delete("/celexpressions/" + expr.getId())
                    .then()
                    .statusCode(204);

            verify(service).delete(expr.getId());
        }

        @Test
        void delete_shouldReturnNotFound_whenNotFound() {
            when(service.delete(any())).thenReturn(ServiceResult.notFound("not found"));

            baseRequest()
                    .delete("/celexpressions/id")
                    .then()
                    .statusCode(404);

        }

    }

    @Nested
    class CelTest {

        @Test
        void test() {

            var req = CelExpressionTestRequest.Builder.newInstance()
                    .expression("expr")
                    .leftOperand("leftOperand")
                    .rightOperand("rightOperand")
                    .operator("operator")
                    .build();

            var res = CelExpressionTestResponse.Builder
                    .newInstance().build();

            var expandedBody = Json.createObjectBuilder().build();

            when(service.test(any())).thenReturn(ServiceResult.success(res));
            when(transformerRegistry.transform(any(), eq(CelExpressionTestRequest.class))).thenReturn(Result.success(req));
            when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedBody));

            baseRequest()
                    .body("{}")
                    .contentType(JSON)
                    .post("/celexpressions/test")
                    .then()
                    .statusCode(200);

            verify(service).test(req);

        }


    }


}
