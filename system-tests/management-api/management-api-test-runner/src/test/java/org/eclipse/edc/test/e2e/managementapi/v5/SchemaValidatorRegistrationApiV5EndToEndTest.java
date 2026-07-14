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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.CachedDocumentService;
import org.eclipse.edc.document.cache.spi.CachedDocumentType;
import org.eclipse.edc.document.cache.spi.PullStrategy;
import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
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
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.PARTICIPANT_CONTEXT_ID;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;

public class SchemaValidatorRegistrationApiV5EndToEndTest {

    private static final String SCHEMA_ID = "https://example.com/schema/custom-type.json";

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        protected String adminToken;

        protected JsonObject registrationBody(String version, String validatedType, String schema) {
            var builder = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "SchemaValidatorRegistration");
            if (version != null) {
                builder.add("version", version);
            }
            if (validatedType != null) {
                builder.add("validatedType", validatedType);
            }
            if (schema != null) {
                builder.add("schema", schema);
            }
            return builder.build();
        }

        protected void cacheSchema(CachedDocumentService cacheService) {
            var content = createObjectBuilder()
                    .add("$schema", "https://json-schema.org/draft/2019-09/schema")
                    .add("type", "object")
                    .add("required", Json.createArrayBuilder().add("mustHave"))
                    .build();
            var document = CachedDocument.Builder.newInstance()
                    .url(SCHEMA_ID)
                    .content(content.toString())
                    .type(CachedDocumentType.JSON_SCHEMA)
                    .pullStrategy(PullStrategy.NEVER)
                    .build();
            cacheService.create(document).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);
            adminToken = authServer.createAdminToken();
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, SchemaValidatorRegistrationStore store, CachedDocumentStore cachedDocumentStore) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            store.findAll(QuerySpec.max()).forEach(r -> store.delete(r.getId()));
            cachedDocumentStore.findAll(QuerySpec.max()).forEach(d -> cachedDocumentStore.delete(d.getId()));
        }

        @Test
        void create_whenSchemaCached(ManagementEndToEndV5TestContext context, CachedDocumentService cacheService) {
            cacheSchema(cacheService);

            context.baseRequest(adminToken)
                    .body(registrationBody("v5", "CustomType", SCHEMA_ID).toString())
                    .contentType(JSON)
                    .post("/v5beta/schemavalidators")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body(ID, matchesRegex("\\S+"));
        }

        @Test
        void create_whenSchemaNotCached_returnsBadRequest(ManagementEndToEndV5TestContext context) {
            context.baseRequest(adminToken)
                    .body(registrationBody("v5", "CustomType", "https://example.com/schema/not-cached.json").toString())
                    .contentType(JSON)
                    .post("/v5beta/schemavalidators")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void create_validationFails_whenSchemaMissing(ManagementEndToEndV5TestContext context) {
            context.baseRequest(adminToken)
                    .body(registrationBody("v5", "CustomType", null).toString())
                    .contentType(JSON)
                    .post("/v5beta/schemavalidators")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void getAll(ManagementEndToEndV5TestContext context, CachedDocumentService cacheService) {
            cacheSchema(cacheService);
            var id = createRegistration(context, cacheService);

            context.baseRequest(adminToken)
                    .contentType(JSON)
                    .get("/v5beta/schemavalidators")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1))
                    .body("[0].validatedType", is("CustomType"));

            context.baseRequest(adminToken)
                    .contentType(JSON)
                    .get("/v5beta/schemavalidators/" + id)
                    .then()
                    .statusCode(200)
                    .body("schema", is(SCHEMA_ID));
        }

        @Test
        void delete(ManagementEndToEndV5TestContext context, CachedDocumentService cacheService) {
            cacheSchema(cacheService);
            var id = createRegistration(context, cacheService);

            context.baseRequest(adminToken)
                    .delete("/v5beta/schemavalidators/" + id)
                    .then()
                    .statusCode(204);

            context.baseRequest(adminToken)
                    .delete("/v5beta/schemavalidators/" + id)
                    .then()
                    .statusCode(404);
        }

        @Test
        void create_notAdmin(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            context.baseRequest(nonAdminToken(authServer))
                    .body(registrationBody("v5", "CustomType", SCHEMA_ID).toString())
                    .contentType(JSON)
                    .post("/v5beta/schemavalidators")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(matchesRegex("(?s).*Required scope.*management-api:admin.*missing.*"));
        }

        protected String createRegistration(ManagementEndToEndV5TestContext context, CachedDocumentService cacheService) {
            return context.baseRequest(adminToken)
                    .body(registrationBody("v5", "CustomType", SCHEMA_ID).toString())
                    .contentType(JSON)
                    .post("/v5beta/schemavalidators")
                    .then()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);
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

        // The whole point of the vertical: a runtime registration must cause request bodies of the bound type to be
        // validated against the cached schema, and deleting it must restore prior behaviour.
        @Test
        void registration_appliesValidationThroughRegistry(ManagementEndToEndV5TestContext context,
                                                           CachedDocumentService cacheService, JsonObjectValidatorRegistry validatorRegistry) {
            cacheSchema(cacheService);
            var id = createRegistration(context, cacheService);

            var invalid = createObjectBuilder().add("@type", "CustomType").build();
            var valid = createObjectBuilder().add("@type", "CustomType").add("mustHave", "value").build();

            assertThat(validatorRegistry.validate("v5:CustomType", invalid).failed()).isTrue();
            assertThat(validatorRegistry.validate("v5:CustomType", valid).succeeded()).isTrue();

            context.baseRequest(adminToken)
                    .delete("/v5beta/schemavalidators/" + id)
                    .then()
                    .statusCode(204);

            // once the registration is removed the dynamic validator passes everything again
            assertThat(validatorRegistry.validate("v5:CustomType", invalid).succeeded()).isTrue();
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
