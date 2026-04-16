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

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.api.authentication.OauthServer;
import org.eclipse.edc.api.authentication.OauthServerEndToEndExtension;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.createParticipant;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContext;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.jsonLdContextArray;
import static org.eclipse.edc.test.e2e.managementapi.v5.TestFunction.participantContext;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class TransferProcessApiV5EndToEndTest {

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
        void teardown(ParticipantContextService participantContextService, TransferProcessStore store) {
            participantContextService.deleteParticipantContext(PARTICIPANT_CONTEXT_ID)
                    .orElseThrow(f -> new AssertionError(f.getFailureDetail()));

            store.findAll(QuerySpec.max()).forEach(tp -> store.delete(tp.getId()));

        }

        @Test
        void initiate(ManagementEndToEndV5TestContext context, TransferProcessStore transferProcessStore, ContractNegotiationStore contractNegotiationStore) {
            var assetId = UUID.randomUUID().toString();
            var contractId = UUID.randomUUID().toString();
            var contractNegotiation = ContractNegotiation.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .counterPartyId("counterPartyId")
                    .counterPartyAddress("http://counterparty")
                    .protocol("dataspace-protocol-http")
                    .participantContextId("participantContextId")
                    .contractAgreement(createContractAgreement(contractId, assetId).build())
                    .build();
            contractNegotiationStore.save(contractNegotiation);

            var requestBody = createTransferRequestJson(contractId, assetId);

            var id = context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            assertThat(transferProcessStore.findById(id)).isNotNull();
        }

        private JsonObject createTransferRequestJson(String contractId, String assetId) {
            return createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "TransferRequest")
                    .add("dataDestination", createObjectBuilder()
                            .add(TYPE, "DataAddress")
                            .add("type", "HttpData")
                            .add("properties", createObjectBuilder()
                                    .add("baseUrl", "http://any")
                                    .build())
                            .build()
                    )
                    .add("transferType", "HttpData-PUSH")
                    .add("callbackAddresses", createCallbackAddress())
                    .add("protocol", "dataspace-protocol-http")
                    .add("counterPartyAddress", "http://connector-address")
                    .add("contractId", contractId)
                    .add("assetId", assetId)
                    .build();
        }

        @Test
        void initiate_whenValidationFails(ManagementEndToEndV5TestContext context) {

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "TransferRequest")
                    .build();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses")
                    .then()
                    .log().ifError()
                    .statusCode(400);
        }

        @Test
        void initiate_tokenBearerWrong(ManagementEndToEndV5TestContext context, OauthServer authServer, ParticipantContextService service) {
            var assetId = UUID.randomUUID().toString();
            var contractId = UUID.randomUUID().toString();
            var requestBody = createTransferRequestJson(contractId, assetId);

            var otherParticipantId = UUID.randomUUID().toString();

            service.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError("ParticipantContext " + otherParticipantId + " not created."));

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses")
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void initiate_tokenLacksWriteScope(ManagementEndToEndV5TestContext context, OauthServer authServer) {
            var assetId = UUID.randomUUID().toString();
            var contractId = UUID.randomUUID().toString();
            var requestBody = createTransferRequestJson(contractId, assetId);

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses")
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void getById(ManagementEndToEndV5TestContext context, TransferProcessStore store) {
            store.save(createTransferProcess("tp1"));
            store.save(createTransferProcess("tp2"));

            context.baseRequest(participantTokenJwt)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/tp2")
                    .then()
                    .statusCode(200)
                    .body(TYPE, is("TransferProcess"))
                    .body(ID, is("tp2"))
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2));
        }

        @Test
        void getById_tokenBearerWrong(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                      TransferProcessStore store, ParticipantContextService service) {
            store.save(createTransferProcess("tp1"));

            var otherParticipantId = UUID.randomUUID().toString();

            service.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError("ParticipantContext " + otherParticipantId + " not created."));

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/tp1")
                    .then()
                    .statusCode(403);
        }

        @Test
        void getById_tokenLacksReadScope(ManagementEndToEndV5TestContext context, OauthServer authServer, TransferProcessStore store) {
            store.save(createTransferProcess("tp1"));

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:wrong"));

            context.baseRequest(token)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/tp1")
                    .then()
                    .statusCode(403);
        }

        @Test
        void getState(ManagementEndToEndV5TestContext context, TransferProcessStore store) {
            store.save(createTransferProcessBuilder("tp2").state(COMPLETED.code()).build());

            context.baseRequest(participantTokenJwt)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/tp2/state")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("TransferState"))
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body("state", is("COMPLETED"));
        }

        @Test
        void getState_tokenBearerWrong(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                       TransferProcessStore store, ParticipantContextService service) {
            store.save(createTransferProcess("tp1"));

            var otherParticipantId = UUID.randomUUID().toString();

            service.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError("ParticipantContext " + otherParticipantId + " not created."));

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/tp1/state")
                    .then()
                    .statusCode(403);
        }

        @Test
        void getState_tokenLacksReadScope(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                          TransferProcessStore store) {
            store.save(createTransferProcess("tp1"));

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:wrong"));

            context.baseRequest(token)
                    .get("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/tp1/state")
                    .then()
                    .statusCode(403);
        }

        @Test
        void terminate(ManagementEndToEndV5TestContext context, TransferProcessStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(REQUESTED.code()).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "TerminateTransfer")
                    .add("reason", "any")
                    .build();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/" + id + "/terminate")
                    .then()
                    .log().ifError()
                    .statusCode(204);
        }

        @Test
        void terminate_tokenBearerWrong(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                        TransferProcessStore store, ParticipantContextService service) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(REQUESTED.code()).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "TerminateTransfer")
                    .add("reason", "any")
                    .build();

            var otherParticipantId = UUID.randomUUID().toString();

            service.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError("ParticipantContext " + otherParticipantId + " not created."));

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/" + id + "/terminate")
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void terminate_tokenLacksWriteScope(ManagementEndToEndV5TestContext context, OauthServer authServer, TransferProcessStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(REQUESTED.code()).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "TerminateTransfer")
                    .add("reason", "any")
                    .build();

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/" + id + "/terminate")
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void suspend(ManagementEndToEndV5TestContext context, TransferProcessStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(STARTED.code()).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "SuspendTransfer")
                    .add("reason", "any")
                    .build();

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/" + id + "/suspend")
                    .then()
                    .log().ifError()
                    .statusCode(204);
        }

        @Test
        void suspend_tokenBearerWrong(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                      TransferProcessStore store, ParticipantContextService service) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(STARTED.code()).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "SuspendTransfer")
                    .add("reason", "any")
                    .build();

            var otherParticipantId = UUID.randomUUID().toString();

            service.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError("ParticipantContext " + otherParticipantId + " not created."));

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/" + id + "/suspend")
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void suspend_tokenLacksWriteScope(ManagementEndToEndV5TestContext context, OauthServer authServer, TransferProcessStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(STARTED.code()).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, jsonLdContext())
                    .add(TYPE, "SuspendTransfer")
                    .add("reason", "any")
                    .build();

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/" + id + "/suspend")
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void resume(ManagementEndToEndV5TestContext context, TransferProcessStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(SUSPENDED.code()).build());

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/" + id + "/resume")
                    .then()
                    .log().ifError()
                    .statusCode(204);
        }

        @Test
        void resume_tokenBearerWrong(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                     TransferProcessStore store, ParticipantContextService service) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(SUSPENDED.code()).build());

            var otherParticipantId = UUID.randomUUID().toString();

            service.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError("ParticipantContext " + otherParticipantId + " not created."));

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/" + id + "/resume")
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void resume_tokenLacksWriteScope(ManagementEndToEndV5TestContext context, OauthServer authServer, TransferProcessStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(SUSPENDED.code()).build());

            var token = authServer.createToken(PARTICIPANT_CONTEXT_ID, Map.of("scope", "management-api:read"));

            context.baseRequest(token)
                    .contentType(JSON)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/" + id + "/resume")
                    .then()
                    .log().ifError()
                    .statusCode(403);
        }

        @Test
        void query(ManagementEndToEndV5TestContext context, TransferProcessStore store) {
            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            store.save(createTransferProcess(id1));
            store.save(createTransferProcess(id2));

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(context.query(criterion("id", "in", List.of(id1, id2))).toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(2))
                    .body("[0].@id", anyOf(is(id1), is(id2)))
                    .body("[0].@context", contains(jsonLdContextArray()))
                    .body("[1].@id", anyOf(is(id1), is(id2)))
                    .body("[1].@context", contains(jsonLdContextArray()));

        }

        @SuppressWarnings("unchecked")
        @ParameterizedTest
        @ValueSource(strings = {"600", "STARTED"})
        void query_byState(String state, ManagementEndToEndV5TestContext context, TransferProcessStore store) {
            var actualState = STARTED;
            var tp = createTransferProcessBuilder("test-tp")
                    .state(actualState.code())
                    .build();
            store.save(tp);

            JsonValue stateValue;
            try {
                stateValue = Json.createValue(Integer.valueOf(state));
            } catch (NumberFormatException e) {
                stateValue = Json.createValue(state);
            }

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "QuerySpec")
                    .add("filterExpression", createArrayBuilder().add(createObjectBuilder()
                            .add(TYPE, "Criterion")
                            .add("operandLeft", "state")
                            .add("operator", "=")
                            .add("operandRight", stateValue))
                    )
                    .add("limit", 100)
                    .add("offset", 0)
                    .build();

            List<Map<String, Object>> result = context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(requestBody.toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/request")
                    .then()
                    .statusCode(200)
                    .extract().body().as(List.class);

            assertThat(result)
                    .isNotEmpty()
                    .anySatisfy(it -> assertThat(it.get("state")).isEqualTo(actualState.name()));
        }

        @SuppressWarnings("unchecked")
        @Test
        void query_sortByStateTimestamp(ManagementEndToEndV5TestContext context, TransferProcessStore store) throws JsonProcessingException {
            var tp1 = createTransferProcessBuilder("test-tp1").build();
            var tp2 = createTransferProcessBuilder("test-tp2")
                    .clock(Clock.fixed(Instant.now().plus(1, ChronoUnit.HOURS), ZoneId.systemDefault()))
                    .build();
            store.save(tp1);
            store.save(tp2);


            var content = """
                    {
                        "@context": ["%s"],
                        "@type": "QuerySpec",
                        "sortField": "stateTimestamp",
                        "sortOrder": "ASC",
                        "limit": 100,
                        "offset": 0
                    }
                    """.formatted(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2);

            List<Map<String, Object>> result = context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(content)
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().body().as(List.class);

            assertThat(result).isNotEmpty().hasSizeGreaterThanOrEqualTo(2);
            assertThat(result).isSortedAccordingTo((o1, o2) -> {
                var l1 = (Long) o1.get("stateTimestamp");
                var l2 = (Long) o2.get("stateTimestamp");
                return Long.compare(l1, l2);
            });
        }

        @Test
        void query_tokenBearerIsAdmin_shouldReturnAll(ManagementEndToEndV5TestContext context, OauthServer authServer, TransferProcessStore store) {
            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            store.save(createTransferProcess(id1));
            store.save(createTransferProcess(id2));

            var token = authServer.createAdminToken();


            context.baseRequest(token)
                    .contentType(JSON)
                    .body(context.query(criterion("id", "in", List.of(id1, id2))).toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/request")
                    .then()
                    .log().ifError()
                    .statusCode(200);

        }

        @Test
        void query_shouldLimitToResourceOwner(ManagementEndToEndV5TestContext context, TransferProcessStore store) {
            var otherParticipantId = UUID.randomUUID().toString();

            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            store.save(createTransferProcess(id1));
            store.save(createTransferProcessBuilder(id2).participantContextId(otherParticipantId).build());

            context.baseRequest(participantTokenJwt)
                    .contentType(JSON)
                    .body(context.query(criterion("id", "in", List.of(id1, id2))).toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(1))
                    .body("[0].@id", is(id1));

        }

        @Test
        void query_tokenBearerNotEqualResourceOwner(ManagementEndToEndV5TestContext context, OauthServer authServer,
                                                    TransferProcessStore store, ParticipantContextService srv) {
            var otherParticipantId = UUID.randomUUID().toString();
            srv.createParticipantContext(participantContext(otherParticipantId))
                    .orElseThrow(f -> new AssertionError("ParticipantContext " + otherParticipantId + " not created."));

            var id1 = UUID.randomUUID().toString();
            store.save(createTransferProcess(id1));

            var token = authServer.createToken(otherParticipantId);

            context.baseRequest(token)
                    .contentType(JSON)
                    .body(context.query(criterion("id", "in", List.of(id1))).toString())
                    .post("/v5beta/participants/" + PARTICIPANT_CONTEXT_ID + "/transferprocesses/request")
                    .then()
                    .log().ifError()
                    .statusCode(403)
                    .body(containsString("User '%s' is not authorized to access this resource.".formatted(otherParticipantId)));

        }

        private TransferProcess createTransferProcess(String id) {
            return createTransferProcessBuilder(id).build();
        }

        private TransferProcess.Builder createTransferProcessBuilder(String id) {
            return TransferProcess.Builder.newInstance()
                    .id(id)
                    .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri("http://any").events(emptySet()).build()))
                    .correlationId(UUID.randomUUID().toString())
                    .dataDestination(DataAddress.Builder.newInstance()
                            .type("type")
                            .build())
                    .protocol("dataspace-protocol-http")
                    .assetId("asset-id")
                    .contractId("contractId")
                    .participantContextId(PARTICIPANT_CONTEXT_ID)
                    .counterPartyAddress("http://connector/address");
        }

        private JsonArrayBuilder createCallbackAddress() {
            var builder = Json.createArrayBuilder();
            return builder.add(createObjectBuilder()
                    .add(TYPE, "CallbackAddress")
                    .add("transactional", false)
                    .add("uri", "http://test.local/")
                    .add("events", Json.createArrayBuilder().build()));
        }

        private ContractAgreement.Builder createContractAgreement(String contractId, String assetId) {
            return ContractAgreement.Builder.newInstance()
                    .id(contractId)
                    .providerId("providerId")
                    .consumerId("consumerId")
                    .policy(Policy.Builder.newInstance().target(assetId).build())
                    .participantContextId("participantContextId")
                    .assetId(assetId);
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

    }

}
