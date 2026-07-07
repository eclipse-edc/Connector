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

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContext;
import org.eclipse.edc.jsonld.cache.spi.CachedJsonLdContextService;
import org.eclipse.edc.jsonld.cache.spi.PullStrategy;
import org.eclipse.edc.jsonld.cache.spi.store.CachedJsonLdContextStore;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
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

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.PARTICIPANT_CONTEXT_ID;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;

public class JsonLdContextCacheApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        protected String adminToken;

        private CachedJsonLdContext inlineContext(String url, String term, String iri) {
            var content = createObjectBuilder()
                    .add("@context", createObjectBuilder().add(term, iri))
                    .build();
            return CachedJsonLdContext.Builder.newInstance()
                    .url(url)
                    .content(content.toString())
                    .pullStrategy(PullStrategy.NEVER)
                    .build();
        }

        protected JsonObject requestBody(String url, JsonObjectBuilder content) {
            var builder = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, CachedJsonLdContext.CACHED_JSON_LD_CONTEXT_TYPE_TERM);
            if (url != null) {
                builder.add("url", url);
            }
            if (content != null) {
                builder.add("content", content);
            }
            return builder.build();
        }

        protected JsonObjectBuilder inlineContent(String term, String iri) {
            return createObjectBuilder().add("@context", createObjectBuilder().add(term, iri));
        }

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);
            adminToken = authServer.createAdminToken();
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, CachedJsonLdContextStore store) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            store.findAll(QuerySpec.max()).forEach(ctx -> store.delete(ctx.getId()));
        }

        @Test
        void create(ManagementEndToEndV5TestContext context, CachedJsonLdContextService service) {
            var requestBody = requestBody("https://example.com/context/create.jsonld",
                    inlineContent("foo", "https://example.com/ns/foo"));

            var id = context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/jsonldcontexts")
                    .then()
                    .log().ifValidationFails()
                    .contentType(JSON)
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            assertThat(service.findById(id))
                    .isNotNull()
                    .extracting(CachedJsonLdContext::getUrl)
                    .isEqualTo("https://example.com/context/create.jsonld");
        }

        @Test
        void create_validationFails_whenUrlMissing(ManagementEndToEndV5TestContext context) {
            var requestBody = requestBody(null, inlineContent("foo", "https://example.com/ns/foo"));

            context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/jsonldcontexts")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void get(ManagementEndToEndV5TestContext context, CachedJsonLdContextService service) {
            var created = service.create(inlineContext("https://example.com/context/get.jsonld", "bar", "https://example.com/ns/bar"))
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            context.baseRequest(adminToken)
                    .contentType(JSON)
                    .get("/v5beta/jsonldcontexts/" + created.getId())
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body(ID, is(created.getId()))
                    .body("url", is("https://example.com/context/get.jsonld"))
                    .body("pullStrategy", is("NEVER"))
                    .body("content['@context'].bar", is("https://example.com/ns/bar"));
        }

        @Test
        void get_notFound(ManagementEndToEndV5TestContext context) {
            context.baseRequest(adminToken)
                    .contentType(JSON)
                    .get("/v5beta/jsonldcontexts/not-found")
                    .then()
                    .log().ifError()
                    .statusCode(404);
        }

        @Test
        void getAll(ManagementEndToEndV5TestContext context, CachedJsonLdContextService service) {
            service.create(inlineContext("https://example.com/context/all.jsonld", "baz", "https://example.com/ns/baz"))
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            context.baseRequest(adminToken)
                    .contentType(JSON)
                    .get("/v5beta/jsonldcontexts")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1))
                    .body("[0].url", is("https://example.com/context/all.jsonld"));
        }

        @Test
        void update(ManagementEndToEndV5TestContext context, CachedJsonLdContextService service) {
            var created = service.create(inlineContext("https://example.com/context/update.jsonld", "old", "https://example.com/ns/old"))
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            var requestBody = requestBody("https://example.com/context/update.jsonld",
                    inlineContent("updated", "https://example.com/ns/updated"));

            context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .put("/v5beta/jsonldcontexts/" + created.getId())
                    .then()
                    .log().ifError()
                    .statusCode(204);

            assertThat(service.findById(created.getId()))
                    .isNotNull()
                    .extracting(CachedJsonLdContext::getContent, org.assertj.core.api.InstanceOfAssertFactories.STRING)
                    .contains("updated");
        }

        @Test
        void delete(ManagementEndToEndV5TestContext context, CachedJsonLdContextService service) {
            var created = service.create(inlineContext("https://example.com/context/delete.jsonld", "term", "https://example.com/ns/term"))
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            context.baseRequest(adminToken)
                    .delete("/v5beta/jsonldcontexts/" + created.getId())
                    .then()
                    .statusCode(204);

            context.baseRequest(adminToken)
                    .delete("/v5beta/jsonldcontexts/" + created.getId())
                    .then()
                    .statusCode(404);
        }

        @Test
        void create_notAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var requestBody = createObjectBuilder()
                    .add("url", "https://example.com/context/create-notadmin.jsonld")
                    .add("content", createObjectBuilder().add("@context", createObjectBuilder()))
                    .build();

            context.baseRequest(nonAdminToken(authServer))
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/jsonldcontexts")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:admin.*missing.*"));
        }

        @Test
        void getAll_notAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            context.baseRequest(nonAdminToken(authServer))
                    .contentType(JSON)
                    .get("/v5beta/jsonldcontexts")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:admin.*missing.*"));
        }

        @Test
        void get_notAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            context.baseRequest(nonAdminToken(authServer))
                    .contentType(JSON)
                    .get("/v5beta/jsonldcontexts/any-id")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:admin.*missing.*"));
        }

        @Test
        void update_notAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var requestBody = createObjectBuilder()
                    .add("url", "https://example.com/context/update-notadmin.jsonld")
                    .add("content", createObjectBuilder().add("@context", createObjectBuilder()))
                    .build();

            context.baseRequest(nonAdminToken(authServer))
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .put("/v5beta/jsonldcontexts/any-id")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:admin.*missing.*"));
        }

        @Test
        void delete_notAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            context.baseRequest(nonAdminToken(authServer))
                    .delete("/v5beta/jsonldcontexts/any-id")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:admin.*missing.*"));
        }

        @Test
        void refresh_notAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            context.baseRequest(nonAdminToken(authServer))
                    .contentType(JSON)
                    .post("/v5beta/jsonldcontexts/any-id/refresh")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:admin.*missing.*"));
        }

        private String nonAdminToken(OauthServer authServer) {
            return authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));
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

        // The whole point of the vertical: the API must keep the running JsonLd service in sync.
        @Test
        void create_registersContextIntoJsonLdService(ManagementEndToEndV5TestContext context, JsonLd jsonLd) {
            var url = "https://example.com/context/sync.jsonld";
            var requestBody = requestBody(url, inlineContent("synced", "https://example.com/ns/synced"));

            context.baseRequest(adminToken)
                    .body(requestBody.toString())
                    .contentType(JSON)
                    .post("/v5beta/jsonldcontexts")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            var document = createObjectBuilder()
                    .add("@context", url)
                    .add("synced", "value")
                    .build();

            var expanded = jsonLd.expand(document);

            assertThat(expanded).isSucceeded()
                    .satisfies(json -> assertThat(json.toString()).contains("https://example.com/ns/synced"));
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
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(() -> POSTGRES_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();
    }
}
