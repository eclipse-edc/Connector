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

import io.restassured.http.ContentType;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.service.ParticipantContextConfigService;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.hamcrest.Matchers.equalTo;

public class ParticipantContextConfigApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        @AfterEach
        void tearDown(ParticipantContextService pcService) {
            pcService.search(QuerySpec.max()).getContent()
                    .forEach(pc -> pcService.deleteParticipantContext(pc.getParticipantContextId()).getContent());

        }

        @Test
        void set(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextConfigService service) {
            var participantContextId = "test-user";

            var entries = Map.of("key1", "value1", "key2", "value2");

            var body = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "ParticipantContextConfig")
                    .add("entries", createObjectBuilder(entries))
                    .build()
                    .toString();

            var token = authServer.createProvisionerToken();

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .put("/v5alpha/participants/" + participantContextId + "/config")
                    .then()
                    .log().all()
                    .statusCode(204);

            var cfg = service.get(participantContextId)
                    .orElseThrow(f -> new AssertionError("Participant context config not found"));

            assertThat(cfg.getEntries()).isEqualTo(entries);
        }

        @Test
        void set_validationFails(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var participantContextId = "test-user";

            var body = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "ParticipantContextConfig")
                    .add("entries", "invalidValue") // should be an object
                    .build()
                    .toString();

            var token = authServer.createProvisionerToken();

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .put("/v5alpha/participants/" + participantContextId + "/config")
                    .then()
                    .statusCode(400)
                    .body("[0].message", equalTo("string found, object expected"))
                    .body("[0].path", equalTo("/entries"));
        }

        @Test
        void set_notAuthorized(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService srv) {
            var participantContextId = "test-user";
            createParticipant(srv, participantContextId);

            var body = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "ParticipantContext")
                    .add(ID, participantContextId)
                    .add("properties", createObjectBuilder().add("test", "test"))
                    .build()
                    .toString();

            var token = authServer.createToken(participantContextId);

            context.baseRequest(token)
                    .contentType(ContentType.JSON)
                    .body(body)
                    .put("/v5alpha/participants/" + participantContextId + "/config")
                    .then()
                    .statusCode(403);

        }

        @Test
        void get(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextConfigService srv) {
            var participantContextId = "test-user";
            var entries = Map.of("key1", "value1", "key2", "value2");

            srv.save(ParticipantContextConfiguration.Builder.newInstance().participantContextId(participantContextId)
                    .entries(entries)
                    .build());
            var token = authServer.createAdminToken();

            var su = context.baseRequest(token)
                    .get("/v5alpha/participants/" + participantContextId + "/config")
                    .then()
                    .statusCode(200)
                    .extract().body().as(Map.class);
            assertThat(su.get("entries")).isEqualTo(entries);
        }

        @Test
        void get_notAuthorized(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService srv) {
            var participantContextId = "test-user";
            createParticipant(srv, participantContextId);

            var token = authServer.createToken(participantContextId);

            context.baseRequest(token)
                    .get("/v5alpha/participants/" + participantContextId + "/config")
                    .then()
                    .statusCode(403);

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
