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
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.participantContext;
import static org.hamcrest.Matchers.equalTo;

public class DataspaceProfileContextApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        @AfterEach
        void tearDown(ParticipantContextService pcService) {
            pcService.search(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

        }

        @Test
        void getProfiles(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService service) {
            var participantContextId = "test-user";

            service.createParticipantContext(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build());

            var token = authServer.createToken(participantContextId);

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .get("/v5beta/participants/" + participantContextId + "/profiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(1))
                    .body("[0].@type", equalTo("DataspaceProfile"))
                    .body("[0].name", equalTo("http-dsp-profile-2025-1"))
                    .body("[0].protocol.version", equalTo("2025-1"))
                    .body("[0].protocol.binding", equalTo("HTTPS"))
                    .body("[0].protocol.path", equalTo("/http-dsp-profile-2025-1"))
                    .body("[0].protocol.namespace", equalTo("https://w3id.org/dspace/2025/1/"))
                    .body("[0].jsonLdContextsUrl", equalTo(List.of("https://w3id.org/dspace/2025/1/context.jsonld",
                            "https://w3id.org/edc/dspace/v0.0.1")));
        }

        @Test
        void getProfiles_withProvisioner(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService service) {
            var participantContextId = "test-user";

            service.createParticipantContext(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build());

            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .get("/v5beta/participants/" + participantContextId + "/profiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(1));
        }

        @Test
        void getProfiles_withAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService service) {
            var participantContextId = "test-user";

            service.createParticipantContext(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build());

            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .get("/v5beta/participants/" + participantContextId + "/profiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(1));
        }


        @Test
        void getProfiles_tokenBearerWrong(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                          ParticipantContextService service) {

            var participantContextId = "test-user";

            service.createParticipantContext(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build());

            var otherParticipantId = UUID.randomUUID().toString();

            service.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError("ParticipantContext " + otherParticipantId + " not created."));

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .get("/v5beta/participants/" + participantContextId + "/profiles")
                    .then()
                    .statusCode(403);
        }

        @Test
        void associateProfiles(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService service) {
            var participantContextId = "test-user";

            service.createParticipantContext(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build());

            var token = authServer.createAdminToken();

            var profiles = associateProfileJson(List.of("http-dsp-profile-2025-1"));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(profiles)
                    .put("/v5beta/participants/" + participantContextId + "/profiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);
        }

        @Test
        void associateProfiles_validationFails(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService service) {
            var participantContextId = "test-user";

            service.createParticipantContext(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build());

            var token = authServer.createAdminToken();

            var profiles = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "AssociateDataspaceProfile")
                    .build();

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(profiles)
                    .put("/v5beta/participants/" + participantContextId + "/profiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body("[0].message", equalTo("required property 'profiles' not found"));
        }

        @Test
        void associateProfiles_profileNotValid(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService service) {
            var participantContextId = "test-user";

            service.createParticipantContext(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build());

            var token = authServer.createAdminToken();

            var profiles = associateProfileJson(List.of("unknown-profile"));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(profiles)
                    .put("/v5beta/participants/" + participantContextId + "/profiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body("[0].message", equalTo("Failed to associate profiles to ParticipantContext: Profile unknown-profile does not exist"));
        }

        @Test
        void associateProfiles_tokenBearerWrong(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService service) {
            var participantContextId = "test-user";

            service.createParticipantContext(ParticipantContext.Builder.newInstance().participantContextId(participantContextId).identity(participantContextId).build());

            var token = authServer.createToken(participantContextId);

            var profiles = associateProfileJson(List.of("http-dsp-profile-2025-1"));

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(profiles)
                    .put("/v5beta/participants/" + participantContextId + "/profiles")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        private JsonObject associateProfileJson(List<String> profiles) {
            return createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "AssociateDataspaceProfile")
                    .add("profiles", createArrayBuilder(profiles))
                    .build();
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
