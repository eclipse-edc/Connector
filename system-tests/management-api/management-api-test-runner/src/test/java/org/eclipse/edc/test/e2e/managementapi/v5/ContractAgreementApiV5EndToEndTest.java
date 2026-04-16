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


import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.PARTICIPANT_CONTEXT_ID;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContextArray;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.participantContext;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ContractAgreementApiV5EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        private String participantTokenJwt;

        @BeforeEach
        void setup(OauthServer authServer, ParticipantContextService participantContextService) {
            createParticipant(participantContextService, PARTICIPANT_CONTEXT_ID);

            participantTokenJwt = authServer.createToken(PARTICIPANT_CONTEXT_ID);
        }

        @AfterEach
        void teardown(ParticipantContextService participantContextService, ContractNegotiationStore store) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            store.queryNegotiations(QuerySpec.max()).forEach(cn -> store.deleteById(cn.getId()));
        }


        @Test
        void getById(ManagementEndToEndV5TestContext context, ContractNegotiationStore store) {
            var agreement = createContractAgreement("agreement-id");
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/agreement-id")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(CONTEXT, contains(jsonLdContextArray()))
                    .body(TYPE, equalTo("ContractAgreement"))
                    .body(ID, is(agreement.getId()))
                    .body("agreementId", is(agreement.getAgreementId()))
                    .body("assetId", notNullValue())
                    .body("policy.assignee", is(agreement.getPolicy().getAssignee()))
                    .body("policy.assigner", is(agreement.getPolicy().getAssigner()));

        }

        @Test
        void getById_tokenBearerDoesNotOwnResource(ManagementEndToEndV5TestContext context,
                                                   OauthServer authServer,
                                                   ContractNegotiationStore store, ParticipantContextService srv) {
            var agreement = createContractAgreement("agreement-id");
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

            var otherParticipantId = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipantId);

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/agreement-id")
                    .then()
                    .statusCode(403);
        }

        @Test
        void getById_tokenLacksRequiredScope(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractNegotiationStore store) {
            var agreement = createContractAgreement("agreement-id");
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:missing"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/agreement-id")
                    .then()
                    .statusCode(403);
        }


        @Test
        void getNegotiationByAgreementId(ManagementEndToEndV5TestContext context, ContractNegotiationStore store) {
            store.save(createContractNegotiationBuilder("negotiation-id")
                    .contractAgreement(createContractAgreement("agreement-id"))
                    .build());

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/agreement-id/negotiation")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(CONTEXT, contains(jsonLdContextArray()))
                    .body(TYPE, equalTo("ContractNegotiation"))
                    .body(ID, is("negotiation-id"));

        }

        @Test
        void getNegotiationByAgreementId_tokenBearerDoesNotOwnResource(ManagementEndToEndV5TestContext context,
                                                                       OauthServer authServer,
                                                                       ContractNegotiationStore store, ParticipantContextService srv) {
            var agreement = createContractAgreement("agreement-id");
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

            var otherParticipantId = UUID.randomUUID().toString();
            createParticipant(srv, otherParticipantId);

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/agreement-id/negotiation")
                    .then()
                    .statusCode(403);
        }

        @Test
        void getNegotiationByAgreementId_tokenLacksRequiredScope(ManagementEndToEndV5TestContext context,
                                                                 OauthServer authServer,
                                                                 ContractNegotiationStore store) {
            var agreement = createContractAgreement("agreement-id");
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:missing"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/agreement-id/negotiation")
                    .then()
                    .statusCode(403);
        }

        @Test
        void query(ManagementEndToEndV5TestContext context, ContractNegotiationStore store) {
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());
            store.save(createContractNegotiationBuilder("cn2").contractAgreement(createContractAgreement("cn2")).build());

            var jsonPath = context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(2))
                    .extract().jsonPath();

            assertThat(jsonPath.getString("[0].assetId")).isNotNull();
            assertThat(jsonPath.getString("[1].assetId")).isNotNull();
            assertThat(jsonPath.getString("[0].@id")).isIn("cn1", "cn2");
            assertThat(jsonPath.getString("[1].@id")).isIn("cn1", "cn2");
            assertThat(jsonPath.getList("[0].@context")).contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2);
            assertThat(jsonPath.getList("[1].@context")).contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2);
        }

        @Test
        void query_tokenBearerIsAdmin_shouldReturnAll(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractNegotiationStore store) {
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());
            store.save(createContractNegotiationBuilder("cn2").contractAgreement(createContractAgreement("cn2")).build());

            var token = authServer.createAdminToken();

            var jsonPath = context.baseRequest(token)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(2))
                    .extract().jsonPath();

            assertThat(jsonPath.getString("[0].assetId")).isNotNull();
            assertThat(jsonPath.getString("[1].assetId")).isNotNull();
            assertThat(jsonPath.getString("[0].@id")).isIn("cn1", "cn2");
            assertThat(jsonPath.getString("[1].@id")).isIn("cn1", "cn2");
            assertThat(jsonPath.getList("[0].@context")).contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2);
            assertThat(jsonPath.getList("[1].@context")).contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2);
        }

        @Test
        void query_shouldLimitToResourceOwner(ManagementEndToEndV5TestContext context, OauthServer authServer, ContractNegotiationStore store) {
            var otherParticipantId = UUID.randomUUID().toString();

            store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());
            store.save(createContractNegotiationBuilder("cn2")
                    .participantContextId(otherParticipantId)
                    .contractAgreement(createContractAgreementBuilder("cn2").participantContextId(otherParticipantId).build()).build());

            var token = authServer.createAdminToken();

            var jsonPath = context.baseRequest(token)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(1))
                    .extract().jsonPath();

            assertThat(jsonPath.getString("[0].assetId")).isNotNull();
            assertThat(jsonPath.getString("[0].@id")).isEqualTo("cn1");
            assertThat(jsonPath.getList("[0].@context")).contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2);
        }

        @Test
        void query_tokenBearerNotEqualResourceOwner(ManagementEndToEndV5TestContext context,
                                                    OauthServer authServer,
                                                    ContractNegotiationStore store, ParticipantContextService srv) {
            var otherParticipantId = UUID.randomUUID().toString();
            srv.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError("ParticipantContext " + otherParticipantId + " not created."));

            store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/contractagreements/request")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource.".formatted(otherParticipantId)));
        }

        private ContractNegotiation.Builder createContractNegotiationBuilder(String negotiationId) {
            return ContractNegotiation.Builder.newInstance()
                    .id(negotiationId)
                    .counterPartyId(UUID.randomUUID().toString())
                    .counterPartyAddress("address")
                    .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                            .uri("local://test")
                            .events(Set.of("test-event1", "test-event2"))
                            .build()))
                    .protocol("dataspace-protocol-http")
                    .contractOffer(contractOfferBuilder().build())
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .state(FINALIZED.code());
        }

        private ContractOffer.Builder contractOfferBuilder() {
            return ContractOffer.Builder.newInstance()
                    .id("test-offer-id")
                    .assetId("test-asset-id")
                    .policy(Policy.Builder.newInstance().build());
        }

        private ContractAgreement createContractAgreement(String id) {
            return createContractAgreementBuilder(id)
                    .build();
        }

        private ContractAgreement.Builder createContractAgreementBuilder(String id) {
            return ContractAgreement.Builder.newInstance()
                    .id(id)
                    .assetId(UUID.randomUUID().toString())
                    .consumerId(UUID.randomUUID() + "-consumer")
                    .providerId(UUID.randomUUID() + "-provider")
                    .policy(Policy.Builder.newInstance().assignee("assignee").assigner("assigner").build())
                    .participantContextId(PARTICIPANT_CONTEXT_ID);
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
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(Runtimes.ControlPlane::config)
                .configurationProvider(() -> POSTGRES_EXTENSION.configFor(Runtimes.ControlPlane.NAME.toLowerCase()))
                .configurationProvider(AUTH_SERVER_EXTENSION::getConfig)
                .paramProvider(ManagementEndToEndV5TestContext.class, ManagementEndToEndV5TestContext::forContext)
                .build();


        @AfterEach
        void cleanup() {
            POSTGRES_EXTENSION.execute(Runtimes.ControlPlane.NAME.toLowerCase(), "DELETE FROM edc_contract_negotiation;");
            POSTGRES_EXTENSION.execute(Runtimes.ControlPlane.NAME.toLowerCase(), "DELETE FROM edc_contract_agreement;");
        }
    }
}
