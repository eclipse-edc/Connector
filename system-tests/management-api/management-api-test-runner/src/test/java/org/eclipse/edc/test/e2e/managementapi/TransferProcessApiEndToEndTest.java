/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.test.e2e.managementapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.Matchers.is;

public class TransferProcessApiEndToEndTest {

    abstract static class Tests {

        @Test
        void getAll(ManagementEndToEndTestContext context, TransferProcessStore store) {
            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            store.save(createTransferProcess(id1));
            store.save(createTransferProcess(id2));

            context.baseRequest()
                    .contentType(JSON)
                    .body(context.query(criterion("id", "in", List.of(id1, id2))))
                    .post("/v3/transferprocesses/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(2))
                    .body("[0].@id", anyOf(is(id1), is(id2)))
                    .body("[1].@id", anyOf(is(id1), is(id2)));
        }

        @Test
        void getById(ManagementEndToEndTestContext context, TransferProcessStore store) {
            store.save(createTransferProcess("tp1"));
            store.save(createTransferProcess("tp2"));

            context.baseRequest()
                    .get("/v3/transferprocesses/tp2")
                    .then()
                    .statusCode(200)
                    .body("@id", is("tp2"))
                    .body(TYPE, is("TransferProcess"));
        }

        @Test
        void getState(ManagementEndToEndTestContext context, TransferProcessStore store) {
            store.save(createTransferProcessBuilder("tp2").state(COMPLETED.code()).build());

            context.baseRequest()
                    .get("/v3/transferprocesses/tp2/state")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("TransferState"))
                    .body("'state'", is("COMPLETED"));
        }

        @Test
        void create(ManagementEndToEndTestContext context, TransferProcessStore transferProcessStore, ContractNegotiationStore contractNegotiationStore) {
            var assetId = UUID.randomUUID().toString();
            var contractId = UUID.randomUUID().toString();
            var contractNegotiation = ContractNegotiation.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .counterPartyId("counterPartyId")
                    .counterPartyAddress("http://counterparty")
                    .protocol("dataspace-protocol-http")
                    .contractAgreement(createContractAgreement(contractId, assetId).build())
                    .build();
            contractNegotiationStore.save(contractNegotiation);

            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
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

            var id = context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v3/transferprocesses/")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            assertThat(transferProcessStore.findById(id)).isNotNull();
        }

        @Test
        void deprovision(ManagementEndToEndTestContext context, TransferProcessStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(COMPLETED.code()).build());

            context.baseRequest()
                    .contentType(JSON)
                    .post("/v3/transferprocesses/" + id + "/deprovision")
                    .then()
                    .statusCode(204);
        }

        @Test
        void terminate(ManagementEndToEndTestContext context, TransferProcessStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(REQUESTED.code()).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                    .add("reason", "any")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v3/transferprocesses/" + id + "/terminate")
                    .then()
                    .log().ifError()
                    .statusCode(204);
        }

        @ParameterizedTest
        @ValueSource(strings = { "600", "STARTED" })
        void request_byState(String state, ManagementEndToEndTestContext context, TransferProcessStore store) {
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
                    .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                    .add(TYPE, "QuerySpec")
                    .add("filterExpression", createObjectBuilder()
                            .add("operandLeft", "state")
                            .add("operator", "=")
                            .add("operandRight", stateValue)
                    )
                    .add("limit", 100)
                    .add("offset", 0)
                    .build();

            var result = context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v3/transferprocesses/request")
                    .then()
                    .statusCode(200)
                    .extract().body().as(JsonArray.class);

            assertThat(result)
                    .isNotEmpty()
                    .anySatisfy(it -> assertThat(it.asJsonObject().getString("state")).isEqualTo(actualState.name()));
        }

        @Test
        void request_sortByStateTimestamp(ManagementEndToEndTestContext context, TransferProcessStore store) throws JsonProcessingException {
            var tp1 = createTransferProcessBuilder("test-tp1").build();
            var tp2 = createTransferProcessBuilder("test-tp2")
                    .clock(Clock.fixed(Instant.now().plus(1, ChronoUnit.HOURS), ZoneId.systemDefault()))
                    .build();
            store.save(tp1);
            store.save(tp2);


            var content = """
                    {
                        "@context": {
                            "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
                        },
                        "@type": "QuerySpec",
                        "sortField": "stateTimestamp",
                        "sortOrder": "ASC",
                        "limit": 100,
                        "offset": 0
                    }
                    """;
            var query = JacksonJsonLd.createObjectMapper()
                    .readValue(content, JsonObject.class);

            var result = context.baseRequest()
                    .contentType(JSON)
                    .body(query)
                    .post("/v3/transferprocesses/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().body().as(JsonArray.class);

            assertThat(result).isNotEmpty().hasSizeGreaterThanOrEqualTo(2);
            assertThat(result).isSortedAccordingTo((o1, o2) -> {
                var l1 = o1.asJsonObject().getJsonNumber("stateTimestamp").longValue();
                var l2 = o2.asJsonObject().getJsonNumber("stateTimestamp").longValue();
                return Long.compare(l1, l2);
            });
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
                    .counterPartyAddress("http://connector/address");
        }

        private JsonArrayBuilder createCallbackAddress() {
            var builder = Json.createArrayBuilder();
            return builder.add(createObjectBuilder()
                    .add(IS_TRANSACTIONAL, false)
                    .add(URI, "http://test.local/")
                    .add(EVENTS, Json.createArrayBuilder().build()));
        }

        private ContractAgreement.Builder createContractAgreement(String contractId, String assetId) {
            return ContractAgreement.Builder.newInstance()
                    .id(contractId)
                    .providerId("providerId")
                    .consumerId("consumerId")
                    .policy(Policy.Builder.newInstance().target(assetId).build())
                    .assetId(assetId);
        }
    }

    @Nested
    @EndToEndTest
    @ExtendWith(ManagementEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        static PostgresqlEndToEndExtension postgres = new PostgresqlEndToEndExtension();

        @RegisterExtension
        static ManagementEndToEndExtension runtime = new ManagementEndToEndExtension.Postgres(postgres);

    }

}
