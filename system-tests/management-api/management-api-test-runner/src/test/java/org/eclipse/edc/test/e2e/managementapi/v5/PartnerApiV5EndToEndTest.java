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
import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
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
import java.util.Set;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class PartnerApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
        private static final String OTHER_PARTICIPANT_CONTEXT_ID = "other-participant";
        private static final String SCOPES = "management-api:partners:read management-api:partners:write";

        private String token;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService, PartnerGroupStore groupStore) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);
            createParticipant(participantContextService, OTHER_PARTICIPANT_CONTEXT_ID);
            token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", SCOPES));

            groupStore.create(group(PARTICIPANT_CONTEXT_ID, "gold"));
            groupStore.create(group(PARTICIPANT_CONTEXT_ID, "eu"));
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, PartnerStore partnerStore, PartnerGroupStore groupStore) {
            partnerStore.query(QuerySpec.max()).forEach(p -> partnerStore.deleteById(p.getParticipantContextId(), p.getId()));
            groupStore.query(QuerySpec.max()).forEach(g -> groupStore.deleteById(g.getParticipantContextId(), g.getId()));
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            participantContextService.deleteParticipantContext(OTHER_PARTICIPANT_CONTEXT_ID).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }

        @Test
        void create_thenGet(ManagementEndToEndV5TestContext context, PartnerStore partnerStore) {
            var id = context.baseRequest(token)
                    .contentType(JSON)
                    .body(partnerJson("acme", "did:web:acme", Map.of("businessId", "BID-A"), Set.of("gold", "eu")).toString())
                    .post("/v5/participants/%s/partners".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            assertThat(id).isEqualTo("acme");
            var stored = partnerStore.findById(PARTICIPANT_CONTEXT_ID, "acme");
            assertThat(stored).isNotNull();
            assertThat(stored.getIdentity()).isEqualTo("did:web:acme");
            assertThat(stored.getGroupIds()).containsExactlyInAnyOrder("gold", "eu");
            assertThat(stored.getProperties()).containsEntry("businessId", "BID-A");

            context.baseRequest(token)
                    .get("/v5/participants/%s/partners/acme".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(ID, is("acme"))
                    .body("identity", is("did:web:acme"))
                    .body("name", is("ACME"))
                    .body("properties.businessId", is("BID-A"))
                    .body("groupIds", containsInAnyOrder("gold", "eu"));
        }

        @Test
        void create_shouldFail_whenGroupUnknown(ManagementEndToEndV5TestContext context) {
            context.baseRequest(token)
                    .contentType(JSON)
                    .body(partnerJson("acme", "did:web:acme", Map.of(), Set.of("platinum")).toString())
                    .post("/v5/participants/%s/partners".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400)
                    .body(containsString("platinum"));
        }

        @Test
        void create_shouldFail_whenIdentityTaken(ManagementEndToEndV5TestContext context, PartnerStore partnerStore) {
            partnerStore.create(partner(PARTICIPANT_CONTEXT_ID, "existing", "did:web:acme"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(partnerJson("acme", "did:web:acme", Map.of(), Set.of()).toString())
                    .post("/v5/participants/%s/partners".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);
        }

        @Test
        void create_shouldFail_whenIdentityMissing(ManagementEndToEndV5TestContext context) {
            var body = createObjectBuilder().add(CONTEXT, jsonLdContext()).add(TYPE, "Partner").add("name", "no identity").build();

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(body.toString())
                    .post("/v5/participants/%s/partners".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void query_byPropertyAndGroup(ManagementEndToEndV5TestContext context, PartnerStore partnerStore) {
            partnerStore.create(partner(PARTICIPANT_CONTEXT_ID, "p1", "did:web:p1").toBuilder().property("businessId", "BID-1").groupIds(Set.of("gold")).build());
            partnerStore.create(partner(PARTICIPANT_CONTEXT_ID, "p2", "did:web:p2").toBuilder().property("businessId", "BID-2").groupIds(Set.of("eu")).build());
            partnerStore.create(partner(OTHER_PARTICIPANT_CONTEXT_ID, "p3", "did:web:p3").toBuilder().property("businessId", "BID-1").groupIds(Set.of()).build());

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(context.query(criterion("properties.businessId", "=", "BID-1")).toString())
                    .post("/v5/participants/%s/partners/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(1))
                    .body("[0].@id", is("p1"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(context.query(criterion("groupIds", "contains", "eu")).toString())
                    .post("/v5/participants/%s/partners/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(1))
                    .body("[0].@id", is("p2"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(context.query().toString())
                    .post("/v5/participants/%s/partners/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(2));
        }

        @Test
        void update(ManagementEndToEndV5TestContext context, PartnerStore partnerStore) {
            partnerStore.create(partner(PARTICIPANT_CONTEXT_ID, "acme", "did:web:acme"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(partnerJson("acme", "did:web:acme-renamed", Map.of("businessId", "BID-B"), Set.of("gold")).toString())
                    .put("/v5/participants/%s/partners/acme".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            var updated = partnerStore.findById(PARTICIPANT_CONTEXT_ID, "acme");
            assertThat(updated.getIdentity()).isEqualTo("did:web:acme-renamed");
            assertThat(updated.getGroupIds()).containsExactly("gold");
            assertThat(updated.getProperties()).containsEntry("businessId", "BID-B");
        }

        @Test
        void update_shouldFail_whenNotFound(ManagementEndToEndV5TestContext context) {
            context.baseRequest(token)
                    .contentType(JSON)
                    .body(partnerJson("missing", "did:web:missing", Map.of(), Set.of()).toString())
                    .put("/v5/participants/%s/partners/missing".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);
        }

        @Test
        void delete(ManagementEndToEndV5TestContext context, PartnerStore partnerStore) {
            partnerStore.create(partner(PARTICIPANT_CONTEXT_ID, "acme", "did:web:acme"));

            context.baseRequest(token)
                    .delete("/v5/participants/%s/partners/acme".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            assertThat(partnerStore.findById(PARTICIPANT_CONTEXT_ID, "acme")).isNull();

            context.baseRequest(token)
                    .delete("/v5/participants/%s/partners/acme".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .statusCode(404);
        }

        @Test
        void shouldNotAccessOtherParticipantContext(ManagementEndToEndV5TestContext context, OauthServer authServer, PartnerStore partnerStore) {
            partnerStore.create(partner(PARTICIPANT_CONTEXT_ID, "acme", "did:web:acme"));
            var otherToken = authServer.createToken(OTHER_PARTICIPANT_CONTEXT_ID, Map.of("scope", SCOPES));

            context.baseRequest(otherToken)
                    .get("/v5/participants/%s/partners/acme".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

            context.baseRequest(otherToken)
                    .contentType(JSON)
                    .body(context.query().toString())
                    .post("/v5/participants/%s/partners/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        @Test
        void shouldRequireWriteScope(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var readOnly = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:partners:read"));

            context.baseRequest(readOnly)
                    .contentType(JSON)
                    .body(partnerJson("acme", "did:web:acme", Map.of(), Set.of()).toString())
                    .post("/v5/participants/%s/partners".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

            context.baseRequest(readOnly)
                    .contentType(JSON)
                    .body(context.query().toString())
                    .post("/v5/participants/%s/partners/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);
        }

        private jakarta.json.JsonObject partnerJson(String id, String identity, Map<String, String> properties, Set<String> groupIds) {
            var propertiesBuilder = createObjectBuilder();
            properties.forEach(propertiesBuilder::add);
            return createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(ID, id)
                    .add(TYPE, "Partner")
                    .add("identity", identity)
                    .add("name", "ACME")
                    .add("properties", propertiesBuilder)
                    .add("groupIds", createArrayBuilder(groupIds))
                    .build();
        }

        private Partner partner(String participantContextId, String id, String identity) {
            return Partner.Builder.newInstance().id(id).participantContextId(participantContextId).identity(identity).name("Partner " + id).build();
        }

        private PartnerGroup group(String participantContextId, String id) {
            return PartnerGroup.Builder.newInstance().id(id).participantContextId(participantContextId).name("Group " + id).build();
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
        static final BeforeAllCallback SETUP = context -> POSTGRES_EXTENSION.createDatabase(Runtimes.ControlPlane.NAME.toLowerCase());

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
