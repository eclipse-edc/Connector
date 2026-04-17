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
import jakarta.json.JsonObject;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
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

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.hamcrest.Matchers.notNullValue;

/**
 * DataPlaneRegistration v5alpha endpoints end-to-end tests
 */
public class DataPlaneRegistrationApiV5EndToEndTest {


    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";

        private String participantTokenJwt;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);

            participantTokenJwt = authServer.createToken(PARTICIPANT_CONTEXT_ID);

        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }

        @Test
        void registerDataPlane(ManagementEndToEndV5TestContext context, DataPlaneSelectorService selectorService) {
            var message = createDataPlaneRegistrationMessage("dataplane-1", null);

            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body(notNullValue());

            var dataPlaneInstance = selectorService.findById("dataplane-1")
                    .orElseThrow((e) -> new AssertionError("Data plane instance not found"));

            assertThat(dataPlaneInstance).isNotNull();
            assertThat(dataPlaneInstance.getParticipantContextId()).isEqualTo(PARTICIPANT_CONTEXT_ID);
        }


        @Test
        void registerDataPlane_tokenBearerDoesNotOwnResource(ManagementEndToEndV5TestContext context,
                                                             OauthServer authServer,
                                                             ParticipantContextService srv) {

            var message = createDataPlaneRegistrationMessage("dataplane-1", null);

            var otherParticipantId = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipantId);
            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes")
                    .then()
                    .log().all()
                    .statusCode(403)
                    .body(notNullValue());
        }

        @Test
        void registerDataPlane_tokenLacksRequiredScope(ManagementEndToEndV5TestContext context,
                                                       OauthServer authServer) {

            var message = createDataPlaneRegistrationMessage("dataplane-1", null);
            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes")
                    .then()
                    .log().all()
                    .statusCode(403)
                    .body(notNullValue());
        }

        @Test
        void registerDataPlane_tokenBearerIsAdmin(ManagementEndToEndV5TestContext context,
                                                  OauthServer authServer) {

            var message = createDataPlaneRegistrationMessage("dataplane-1", null);

            context.baseRequest(authServer.createAdminToken())
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body(notNullValue());
        }

        @Test
        void registerDataPlane_tokenBearerIsProvisioner(ManagementEndToEndV5TestContext context,
                                                        OauthServer authServer) {

            var message = createDataPlaneRegistrationMessage("dataplane-1", null);

            context.baseRequest(authServer.createProvisionerToken())
                    .contentType(ContentType.JSON)
                    .body(message)
                    .put("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body(notNullValue());
        }

        @Test
        void delete(ManagementEndToEndV5TestContext context, DataPlaneSelectorService selectorService) {

            var instance = DataPlaneInstance.Builder.newInstance().id(UUID.randomUUID().toString())
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .url("http://example.com/dataflows")
                    .build();
            selectorService.register(instance).orElseThrow(f -> new AssertionError("Failed to register data plane instance for test setup: " + f.getFailureDetail()));


            context.baseRequest(participantTokenJwt)
                    .contentType(ContentType.JSON)
                    .delete("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/" + instance.getId())
                    .then()
                    .log().all()
                    .statusCode(200);
        }

        @Test
        void delete_tokenBearerDoesNotOwnResource(ManagementEndToEndV5TestContext context,
                                                  DataPlaneSelectorService selectorService,
                                                  ParticipantContextService srv,
                                                  OauthServer authServer) {


            var instance = DataPlaneInstance.Builder.newInstance().id(UUID.randomUUID().toString())
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .url("http://example.com/dataflows")
                    .build();
            selectorService.register(instance).orElseThrow(f -> new AssertionError("Failed to register data plane instance for test setup: " + f.getFailureDetail()));

            var otherParticipantId = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipantId);
            var token = authServer.createToken(otherParticipantId);


            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .delete("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/" + instance.getId())
                    .then()
                    .log().all()
                    .statusCode(403);
        }

        @Test
        void delete_tokenLacksRequiredScope(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                            DataPlaneSelectorService selectorService) {

            var instance = DataPlaneInstance.Builder.newInstance().id(UUID.randomUUID().toString())
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .url("http://example.com/dataflows")
                    .build();
            selectorService.register(instance).orElseThrow(f -> new AssertionError("Failed to register data plane instance for test setup: " + f.getFailureDetail()));

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .delete("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/dataplanes/" + instance.getId())
                    .then()
                    .log().all()
                    .statusCode(403);
        }

        public JsonObject createDataPlaneRegistrationMessage(String dataPlaneId, JsonObject authorizationProfile) {
            var builder = createObjectBuilder()
                    .add("dataplaneId", dataPlaneId)
                    .add("endpoint", "http://example.com/dataflows")
                    .add("transferTypes", createArrayBuilder()
                            .add("Finite-PUSH")
                            .add("Finite-PULL")
                            .add("NonFinite-PUSH")
                            .add("NonFinite-PULL")
                            .add("AsyncPrepare-PUSH")
                            .add("AsyncStart-PULL")
                    );

            if (authorizationProfile != null) {
                builder.add("authorization", authorizationProfile);
            }

            return builder.build();
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
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(() -> POSTGRES_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();

    }

}
