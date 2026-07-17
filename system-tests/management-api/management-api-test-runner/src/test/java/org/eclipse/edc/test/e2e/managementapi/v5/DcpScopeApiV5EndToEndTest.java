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

import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;
import java.util.Optional;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class DcpScopeApiV5EndToEndTest {

    private static final String DCP_SCOPES_PATH = "/v5beta/dcpscopes";

    private static Config dcpConfig() {
        return ConfigFactory.fromMap(Map.of(
                "edc.iam.sts.oauth.token.url", "https://sts.com/token",
                "edc.iam.sts.oauth.client.id", "test-client",
                "edc.iam.sts.oauth.client.secret.alias", "test-alias",
                "edc.iam.sts.privatekey.alias", "privatekey",
                "edc.iam.sts.publickey.id", "publickey",
                "edc.participant.did", "did:web:someone"
        ));
    }

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        @AfterEach
        void tearDown(DcpScopeRegistry registry, ParticipantContextService participantContextService) {
            var list = participantContextService.search(QuerySpec.max())
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            for (var p : list) {
                participantContextService.deleteParticipantContext(p.getParticipantContextId()).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            }
            registry.query(QuerySpec.max()).getContent()
                    .forEach(scope -> registry.remove(scope.getId()).getContent());
        }

        @Test
        void create(ManagementEndToEndV5TestContext context, OauthServer authServer, DcpScopeRegistry registry) {
            var scopeId = "scope-1";
            var token = authServer.createAdminToken();

            var response = context.baseRequest(token)
                    .contentType(JSON)
                    .body(dcpScopeBody(scopeId, "DEFAULT", "org.example.scope", "*", null))
                    .post(DCP_SCOPES_PATH)
                    .then()
                    .statusCode(200)
                    .extract().body().as(Map.class);

            assertThat(response.get("@id")).isEqualTo(scopeId);
            assertThat(find(registry, scopeId)).isPresent()
                    .get().satisfies(scope -> assertThat(scope.getValue()).isEqualTo("org.example.scope"));
        }

        @Test
        void create_policyScope(ManagementEndToEndV5TestContext context, OauthServer authServer, DcpScopeRegistry registry) {
            var scopeId = "scope-policy";
            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(dcpScopeBody(scopeId, "POLICY", "org.example.scope", "*", "someMapping"))
                    .post(DCP_SCOPES_PATH)
                    .then()
                    .statusCode(200);

            assertThat(find(registry, scopeId)).isPresent()
                    .get().satisfies(scope -> {
                        assertThat(scope.getType()).isEqualTo(DcpScope.Type.POLICY);
                        assertThat(scope.getPrefixMapping()).isEqualTo("someMapping");
                    });
        }

        @Test
        void create_alreadyExists(ManagementEndToEndV5TestContext context, OauthServer authServer, DcpScopeRegistry registry) {
            var scopeId = "scope-1";
            seedScope(registry, scopeId, "org.example.scope");

            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(dcpScopeBody(scopeId, "DEFAULT", "org.example.scope", "*", null))
                    .post(DCP_SCOPES_PATH)
                    .then()
                    .statusCode(409);
        }

        @Test
        void create_validationFails(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var body = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "DcpScope")
                    .add(ID, "scope-1")
                    .add("type", "DEFAULT")
                    .add("profile", "*")
                    .build() // missing required 'value'
                    .toString();

            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(body)
                    .post(DCP_SCOPES_PATH)
                    .then()
                    .statusCode(400)
                    .body("[0].message", containsString("required property 'value' not found"));
        }

        @Test
        void create_notAuthorized(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService srv) {
            var participantContextId = "test-user";
            createParticipant(srv, participantContextId);

            var token = authServer.createToken(participantContextId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(dcpScopeBody("scope-1", "DEFAULT", "org.example.scope", "*", null))
                    .post(DCP_SCOPES_PATH)
                    .then()
                    .statusCode(403);
        }

        @Test
        void update(ManagementEndToEndV5TestContext context, OauthServer authServer, DcpScopeRegistry registry) {
            var scopeId = "scope-1";
            seedScope(registry, scopeId, "org.example.scope");

            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(dcpScopeBody(scopeId, "DEFAULT", "org.example.updated", "*", null))
                    .put(DCP_SCOPES_PATH + "/" + scopeId)
                    .then()
                    .statusCode(204);

            assertThat(find(registry, scopeId)).isPresent()
                    .get().satisfies(scope -> assertThat(scope.getValue()).isEqualTo("org.example.updated"));
        }

        @Test
        void update_notFound(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(dcpScopeBody("unknown", "DEFAULT", "org.example.scope", "*", null))
                    .put(DCP_SCOPES_PATH + "/unknown")
                    .then()
                    .statusCode(404);
        }

        @Test
        void update_notAuthorized(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService srv) {
            var participantContextId = "test-user";
            createParticipant(srv, participantContextId);

            var token = authServer.createToken(participantContextId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(dcpScopeBody("scope-1", "DEFAULT", "org.example.scope", "*", null))
                    .put(DCP_SCOPES_PATH + "/scope-1")
                    .then()
                    .statusCode(403);
        }

        @Test
        void delete(ManagementEndToEndV5TestContext context, OauthServer authServer, DcpScopeRegistry registry) {
            var scopeId = "scope-1";
            seedScope(registry, scopeId, "org.example.scope");

            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .delete(DCP_SCOPES_PATH + "/" + scopeId)
                    .then()
                    .statusCode(204);

            assertThat(find(registry, scopeId)).isEmpty();
        }

        @Test
        void delete_notFound(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .delete(DCP_SCOPES_PATH + "/unknown")
                    .then()
                    .statusCode(404);
        }

        @Test
        void delete_notAuthorized(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService srv) {
            var participantContextId = "test-user";
            createParticipant(srv, participantContextId);

            var token = authServer.createToken(participantContextId);

            context.baseRequest(token)
                    .delete(DCP_SCOPES_PATH + "/scope-1")
                    .then()
                    .statusCode(403);
        }

        @Test
        void query(ManagementEndToEndV5TestContext context, OauthServer authServer, DcpScopeRegistry registry) {
            range(0, 3).forEach(i -> seedScope(registry, "scope-" + i, "org.example.scope" + i));

            var token = authServer.createAdminToken();

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(context.query().toString())
                    .post(DCP_SCOPES_PATH + "/request")
                    .then()
                    .statusCode(200)
                    .body("size()", equalTo(3));
        }

        @Test
        void query_notAuthorized(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService srv) {
            var participantContextId = "test-user";
            createParticipant(srv, participantContextId);

            var token = authServer.createToken(participantContextId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(context.query().toString())
                    .post(DCP_SCOPES_PATH + "/request")
                    .then()
                    .statusCode(403);
        }

        private void seedScope(DcpScopeRegistry registry, String id, String value) {
            registry.create(DcpScope.Builder.newInstance().id(id).value(value).build())
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }

        private Optional<DcpScope> find(DcpScopeRegistry registry, String id) {
            return registry.query(QuerySpec.max()).getContent().stream()
                    .filter(scope -> scope.getId().equals(id))
                    .findFirst();
        }

        private String dcpScopeBody(String id, String type, String value, String profile, String prefixMapping) {
            var builder = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "DcpScope")
                    .add(ID, id)
                    .add("type", type)
                    .add("value", value)
                    .add("profile", profile);

            if (prefixMapping != null) {
                builder.add("prefixMapping", prefixMapping);
            }

            return builder.build().toString();
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
                .modules(Runtimes.ControlPlane.VIRTUAL_DCP_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(DcpScopeApiV5EndToEndTest::dcpConfig)
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
        static final BeforeAllCallback SETUP = context -> POSTGRES_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());

        @Order(2)
        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.VIRTUAL_DCP_MODULES)
                .modules(Runtimes.ControlPlane.VIRTUAL_SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(DcpScopeApiV5EndToEndTest::dcpConfig)
                .configurationProvider(() -> POSTGRES_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();
    }
}
