/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.test.e2e.managementapi.v5;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.policy.cel.model.CelExpression;
import org.eclipse.edc.policy.cel.service.CelPolicyExpressionService;
import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;


public class CelExpressionApiV5EndToEndTest {


    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
        protected String adminToken;

        private CelExpression expression(String leftOperand, String expr) {
            return CelExpression.Builder.newInstance().id(UUID.randomUUID().toString())
                    .leftOperand(leftOperand)
                    .expression(expr)
                    .description("description")
                    .build();
        }

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);

            adminToken = authServer.createAdminToken();
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, PolicyDefinitionStore store) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            store.findAll(QuerySpec.max()).forEach(pd -> store.delete(pd.getId()));
        }

        @Test
        void create(ManagementEndToEndV5TestContext context, CelPolicyExpressionService service) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CelExpression")
                    .add("leftOperand", "leftOperand")
                    .add("expression", "ctx.agent.id == 'agent-123'")
                    .add("description", "desc")
                    .build();

            var id = context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/celexpressions")
                    .then()
                    .log().ifValidationFails()
                    .contentType(JSON)
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            assertThat(service.findById(id)).isSucceeded()
                    .satisfies(c -> {
                        Assertions.assertThat(c.getId()).isEqualTo(id);
                    });

        }

        @Test
        void create_validationFails(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CelExpression")
                    .build();

            context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/celexpressions")
                    .then()
                    .log().ifValidationFails()
                    .contentType(JSON)
                    .statusCode(400);
        }

        @Test
        void create_NotAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CelExpression")
                    .add("leftOperand", "leftOperand")
                    .add("expression", "ctx.agent.id == 'agent-123'")
                    .build();

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:write"));

            context.baseRequest(token)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/celexpressions")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(containsString("Required user role not satisfied."));
        }

        @Test
        void get(ManagementEndToEndV5TestContext context, CelExpressionStore store) {
            var expr = expression("leftOperand", "ctx.agent.id == 'agent-123'");
            store.create(expr)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            context.baseRequest(adminToken)
                    .contentType(JSON)
                    .get("/v5beta/celexpressions/" + expr.getId())
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(expr.getId()))
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body("leftOperand", is(expr.getLeftOperand()))
                    .body("expression", is(expr.getExpression()))
                    .body("description", is(expr.getDescription()))
                    .body("scopes", is(new ArrayList<>(expr.getScopes())));
        }


        @Test
        void get_NotAdmin(ManagementEndToEndV5TestContext context, CelExpressionStore store, OauthServer authServer) {

            var expr = expression("leftOperand", "ctx.agent.id == 'agent-123'");
            store.create(expr)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .get("/v5beta/celexpressions/" + expr.getId())
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(containsString("Required user role not satisfied."));

        }

        @Test
        void query(ManagementEndToEndV5TestContext context, CelExpressionStore store) {
            var expr = expression("leftOperand", "ctx.agent.id == 'agent-123'");
            store.create(expr)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            var matchingQuery = context.query(
                    criterion("id", "=", expr.getId())
            );

            context.baseRequest(adminToken)
                    .body(matchingQuery.toString())
                    .contentType(JSON)
                    .post("/v5beta/celexpressions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1));


            var nonMatchingQuery = context.query(
                    criterion("id", "=", "notFound")
            );

            context.baseRequest(adminToken)
                    .body(nonMatchingQuery.toString())
                    .contentType(JSON)
                    .post("/v5beta/celexpressions/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(0));
        }

        @Test
        void query_NotAdmin(ManagementEndToEndV5TestContext context, CelExpressionStore store, OauthServer authServer) {
            var expr = expression("leftOperand", "ctx.agent.id == 'agent-123'");
            store.create(expr)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            var matchingQuery = context.query(
                    criterion("id", "=", expr.getId())
            );

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .body(matchingQuery.toString())
                    .contentType(JSON)
                    .post("/v5beta/celexpressions/request")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(containsString("Required user role not satisfied."));

        }

        @Test
        void update(ManagementEndToEndV5TestContext context, CelPolicyExpressionService service) {
            var expr = expression("leftOperand", "ctx.agent.id == 'agent-123'");
            service.create(expr)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CelExpression")
                    .add(ID, expr.getId())
                    .add("leftOperand", "leftOperand")
                    .add("expression", "ctx.agent.id == 'agent-125'")
                    .add("description", "desc")
                    .build();

            context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .put("/v5beta/celexpressions/" + expr.getId())
                    .then()
                    .statusCode(204);

            assertThat(service.findById(expr.getId())).isSucceeded()
                    .extracting(CelExpression::getExpression)
                    .isEqualTo("ctx.agent.id == 'agent-125'");
        }


        @Test
        void update_whenSchemaValidationFails(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CelExpression")
                    .build();

            context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .put("/v5beta/celexpressions/id")
                    .then()
                    .log().ifValidationFails()
                    .contentType(JSON)
                    .statusCode(400);
        }

        @Test
        void update_NotAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CelExpression")
                    .build();

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:write"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .put("/v5beta/celexpressions/id")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("Required user role not satisfied."));

        }


        @Test
        void delete(ManagementEndToEndV5TestContext context, CelPolicyExpressionService service) {
            var expr = expression("leftOperand", "ctx.agent.id == 'agent-123'");
            service.create(expr)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));


            context.baseRequest(adminToken)
                    .delete("/v5beta/celexpressions/" + expr.getId())
                    .then()
                    .statusCode(204);

            context.baseRequest(adminToken)
                    .delete("/v5beta/celexpressions/" + expr.getId())
                    .then()
                    .statusCode(404);
        }


        @Test
        void delete_NotAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:write"));

            context.baseRequest(token)
                    .delete("/v5beta/celexpressions/id")
                    .then()
                    .statusCode(403)
                    .body(containsString("Required user role not satisfied."));
        }
    }

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @Order(1)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .build();

        // We can just test in memory
        @Test
        void test(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CelExpressionTestRequest")
                    .add("leftOperand", "leftOperand")
                    .add("expression", "ctx.agent.id == 'agent-125'")
                    .add("rightOperand", "rightOperand")
                    .add("operator", "EQ")
                    .add("params", createObjectBuilder().add("agent", createObjectBuilder().add("id", "agent-125")))
                    .build();

            context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/celexpressions/test")
                    .then()
                    .statusCode(200)
                    .log().ifError()
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body(TYPE, is("CelExpressionTestResponse"))
                    .body("error", nullValue())
                    .body("evaluationResult", is(true));
        }

        @Test
        void test_evaluateFalse(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CelExpressionTestRequest")
                    .add("leftOperand", "leftOperand")
                    .add("expression", "ctx.agent.id == 'agent-125'")
                    .add("rightOperand", "rightOperand")
                    .add("operator", "EQ")
                    .add("params", createObjectBuilder().add("agent", createObjectBuilder().add("id", "agent-600")))
                    .build();

            context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/celexpressions/test")
                    .then()
                    .statusCode(200)
                    .log().ifError()
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body(TYPE, is("CelExpressionTestResponse"))
                    .body("error", nullValue())
                    .body("evaluationResult", is(false));
        }

        @Test
        void test_evaluationError(ManagementEndToEndV5TestContext context) {
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "CelExpressionTestRequest")
                    .add("leftOperand", "leftOperand")
                    .add("expression", "ctx.agent.id == 'agent-125'")
                    .add("rightOperand", "rightOperand")
                    .add("operator", "EQ")
                    .build();

            context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/celexpressions/test")
                    .then()
                    .statusCode(200)
                    .log().ifError()
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body(TYPE, is("CelExpressionTestResponse"))
                    .body("error", containsString("key 'agent' is not present in map"))
                    .body("evaluationResult", nullValue());
        }
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final OauthServerEndToEndExtension AUTH_SERVER_EXTENSION = OauthServerEndToEndExtension.Builder.newInstance().build();

        @RegisterExtension
        @Order(0)
        static final PostgresqlEndToEndExtension POSTGRES_EXTENSION = new PostgresqlEndToEndExtension();

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback SETUP = context -> {
            POSTGRES_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());
        };

        @Order(2)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(() -> POSTGRES_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();

    }

}
