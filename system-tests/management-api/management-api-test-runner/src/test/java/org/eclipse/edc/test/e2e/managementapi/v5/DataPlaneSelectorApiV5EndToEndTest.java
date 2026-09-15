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

package org.eclipse.edc.test.e2e.managementapi.v5;

import io.restassured.http.ContentType;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.dataplane.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.controlplane.dataplane.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * DataPlane Selector v5 endpoints end-to-end tests
 */
public class DataPlaneSelectorApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
        private static final String OTHER_PARTICIPANT_CONTEXT_ID = "other-participant";

        private String participantTokenJwt;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);
            createParticipant(participantContextService, OTHER_PARTICIPANT_CONTEXT_ID);

            participantTokenJwt = authServer.createToken(PARTICIPANT_CONTEXT_ID);
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, DataPlaneSelectorService selectorService) {
            selectorService.getAll().orElseThrow(f -> new AssertionError(f.getFailureDetail()))
                    .forEach(instance -> selectorService.delete(instance.getId()));
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            participantContextService.deleteParticipantContext(OTHER_PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }

        @Test
        void query_shouldReturnOnlyInstancesOfParticipantContext(ManagementEndToEndV5TestContext context, DataPlaneSelectorService selectorService) {
            var own = registerInstance(selectorService, PARTICIPANT_CONTEXT_ID, "http://example.com/own");
            registerInstance(selectorService, OTHER_PARTICIPANT_CONTEXT_ID, "http://example.com/other");

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(context.query())
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(1))
                    .body("[0].@id", is(own.getId()))
                    .body("[0].@context", notNullValue())
                    .body("[0].url", is("http://example.com/own"));
        }

        @Test
        void query_shouldApplyFilterExpression(ManagementEndToEndV5TestContext context, DataPlaneSelectorService selectorService) {
            var first = registerInstance(selectorService, PARTICIPANT_CONTEXT_ID, "http://example.com/first");
            var second = registerInstance(selectorService, PARTICIPANT_CONTEXT_ID, "http://example.com/second");

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(context.query())
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(2))
                    .body("@id", containsInAnyOrder(first.getId(), second.getId()));

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(context.query(new Criterion("id", "=", second.getId())))
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(1))
                    .body("[0].@id", is(second.getId()));
        }

        @Test
        void query_shouldReturnEmptyArray_whenNoInstances(ManagementEndToEndV5TestContext context) {
            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(context.query())
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(0));
        }

        @Test
        void query_tokenBearerDoesNotOwnResource(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var token = authServer.createToken(OTHER_PARTICIPANT_CONTEXT_ID);

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(context.query())
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void query_tokenLacksRequiredScope(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:assets:read"));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(context.query())
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void query_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer, DataPlaneSelectorService selectorService) {
            var own = registerInstance(selectorService, PARTICIPANT_CONTEXT_ID, "http://example.com/own");

            context.baseRequest(authServer.createAdminToken())
                    .contentType(ContentType.JSON)
                    .body(context.query())
                    .post("/v5/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(1))
                    .body("[0].@id", is(own.getId()));
        }

        private DataPlaneInstance registerInstance(DataPlaneSelectorService selectorService, String participantContextId, String url) {
            var instance = DataPlaneInstance.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .participantContextId(participantContextId)
                    .url(url)
                    .build();
            selectorService.register(instance)
                    .orElseThrow(f -> new AssertionError("Failed to register data plane instance for test setup: " + f.getFailureDetail()));
            return instance;
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
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();
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
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(() -> POSTGRES_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();
    }
}
