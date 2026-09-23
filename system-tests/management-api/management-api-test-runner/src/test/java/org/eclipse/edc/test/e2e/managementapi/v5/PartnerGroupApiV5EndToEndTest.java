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
import static jakarta.json.Json.createObjectBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.hamcrest.Matchers.is;

public class PartnerGroupApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private static final String PARTICIPANT_CONTEXT_ID = "test-participant";
        private static final String OTHER_PARTICIPANT_CONTEXT_ID = "other-participant";
        private static final String SCOPES = "management-api:partners:read management-api:partners:write";

        private String token;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);
            createParticipant(participantContextService, OTHER_PARTICIPANT_CONTEXT_ID);
            token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", SCOPES));
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, PartnerStore partnerStore, PartnerGroupStore groupStore) {
            partnerStore.query(QuerySpec.max()).forEach(p -> partnerStore.deleteById(p.getParticipantContextId(), p.getId()));
            groupStore.query(QuerySpec.max()).forEach(g -> groupStore.deleteById(g.getParticipantContextId(), g.getId()));
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
            participantContextService.deleteParticipantContext(OTHER_PARTICIPANT_CONTEXT_ID).orElseThrow(f -> new AssertionError(f.getFailureDetail()));
        }

        @Test
        void create_thenGet(ManagementEndToEndV5TestContext context, PartnerGroupStore groupStore) {
            var id = context.baseRequest(token)
                    .contentType(JSON)
                    .body(groupJson("gold", "Gold tier").toString())
                    .post("/v5/participants/%s/partnergroups".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            assertThat(id).isEqualTo("gold");
            assertThat(groupStore.findById(PARTICIPANT_CONTEXT_ID, "gold")).isNotNull();

            context.baseRequest(token)
                    .get("/v5/participants/%s/partnergroups/gold".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body(ID, is("gold"))
                    .body("name", is("Gold tier"))
                    .body("description", is("desc"))
                    .body("properties.region", is("eu"));
        }

        @Test
        void create_shouldFail_whenNameMissing(ManagementEndToEndV5TestContext context) {
            var body = createObjectBuilder().add(CONTEXT, jsonLdContext()).add(TYPE, "PartnerGroup").build();

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(body.toString())
                    .post("/v5/participants/%s/partnergroups".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);
        }

        @Test
        void create_shouldFail_whenExists(ManagementEndToEndV5TestContext context, PartnerGroupStore groupStore) {
            groupStore.create(group(PARTICIPANT_CONTEXT_ID, "gold"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(groupJson("gold", "Gold tier").toString())
                    .post("/v5/participants/%s/partnergroups".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);
        }

        @Test
        void query(ManagementEndToEndV5TestContext context, PartnerGroupStore groupStore) {
            groupStore.create(group(PARTICIPANT_CONTEXT_ID, "gold"));
            groupStore.create(group(PARTICIPANT_CONTEXT_ID, "silver"));
            groupStore.create(group(OTHER_PARTICIPANT_CONTEXT_ID, "gold"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(context.query().toString())
                    .post("/v5/participants/%s/partnergroups/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(2));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(context.query(criterion("name", "=", "Group silver")).toString())
                    .post("/v5/participants/%s/partnergroups/request".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", is(1))
                    .body("[0].@id", is("silver"));
        }

        @Test
        void update(ManagementEndToEndV5TestContext context, PartnerGroupStore groupStore) {
            groupStore.create(group(PARTICIPANT_CONTEXT_ID, "gold"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(groupJson("gold", "Renamed").toString())
                    .put("/v5/participants/%s/partnergroups/gold".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            assertThat(groupStore.findById(PARTICIPANT_CONTEXT_ID, "gold").getName()).isEqualTo("Renamed");
        }

        @Test
        void delete_shouldFail_whenReferencedByPartner(ManagementEndToEndV5TestContext context, PartnerGroupStore groupStore, PartnerStore partnerStore) {
            groupStore.create(group(PARTICIPANT_CONTEXT_ID, "gold"));
            partnerStore.create(Partner.Builder.newInstance().id("acme").participantContextId(PARTICIPANT_CONTEXT_ID)
                    .identity("did:web:acme").groupIds(Set.of("gold")).build());

            context.baseRequest(token)
                    .delete("/v5/participants/%s/partnergroups/gold".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);

            partnerStore.deleteById(PARTICIPANT_CONTEXT_ID, "acme");

            context.baseRequest(token)
                    .delete("/v5/participants/%s/partnergroups/gold".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            assertThat(groupStore.findById(PARTICIPANT_CONTEXT_ID, "gold")).isNull();
        }

        @Test
        void shouldNotAccessOtherParticipantContext(ManagementEndToEndV5TestContext context, OauthServer authServer, PartnerGroupStore groupStore) {
            groupStore.create(group(PARTICIPANT_CONTEXT_ID, "gold"));
            var otherToken = authServer.createToken(OTHER_PARTICIPANT_CONTEXT_ID, Map.of("scope", SCOPES));

            context.baseRequest(otherToken)
                    .get("/v5/participants/%s/partnergroups/gold".formatted(PARTICIPANT_CONTEXT_ID))
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);
        }

        private jakarta.json.JsonObject groupJson(String id, String name) {
            return createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(ID, id)
                    .add(TYPE, "PartnerGroup")
                    .add("name", name)
                    .add("description", "desc")
                    .add("properties", createObjectBuilder().add("region", "eu"))
                    .build();
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
