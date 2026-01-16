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

package org.eclipse.edc.test.e2e.managementapi.v4;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.ManagementEndToEndTestContext;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
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
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class TransferProcessApiV4EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        @Test
        void getAll(ManagementEndToEndTestContext context, TransferProcessStore store) {
            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            store.save(createTransferProcess(id1));
            store.save(createTransferProcess(id2));

            context.baseRequest()
                    .contentType(JSON)
                    .body(context.queryV2(criterion("id", "in", List.of(id1, id2))))
                    .post("/v4beta/transferprocesses/request")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .body("size()", is(2))
                    .body("[0].@id", anyOf(is(id1), is(id2)))
                    .body("[0].@context", contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body("[1].@id", anyOf(is(id1), is(id2)))
                    .body("[1].@context", contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2));
        }

        @Test
        void getById(ManagementEndToEndTestContext context, TransferProcessStore store) {
            store.save(createTransferProcess("tp1"));
            store.save(createTransferProcess("tp2"));

            context.baseRequest()
                    .get("/v4beta/transferprocesses/tp2")
                    .then()
                    .statusCode(200)
                    .log().ifValidationFails()
                    .body(TYPE, is("TransferProcess"))
                    .body(ID, is("tp2"))
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body("dataplaneMetadata", notNullValue());
        }

        @Test
        void getState(ManagementEndToEndTestContext context, TransferProcessStore store) {
            store.save(createTransferProcessBuilder("tp2").state(COMPLETED.code()).build());

            context.baseRequest()
                    .get("/v4beta/transferprocesses/tp2/state")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(TYPE, is("TransferState"))
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body("state", is("COMPLETED"));
        }

        @Test
        void create(ManagementEndToEndTestContext context, TransferProcessStore transferProcessStore, ContractNegotiationStore contractNegotiationStore) {
            var assetId = UUID.randomUUID().toString();
            var contractId = UUID.randomUUID().toString();
            var contractNegotiation = ContractNegotiation.Builder.newInstance()
                    .id(UUID.randomUUID().toString())
                    .counterPartyId("counterPartyId")
                    .counterPartyAddress("http://counterparty")
                    .protocol(DATASPACE_PROTOCOL_HTTP_V_2025_1)
                    .participantContextId("participantContextId")
                    .contractAgreement(createContractAgreement(contractId, assetId).build())
                    .build();
            contractNegotiationStore.save(contractNegotiation);

            var requestBody = createObjectBuilder()
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
                    .add("protocol", DATASPACE_PROTOCOL_HTTP_V_2025_1)
                    .add("counterPartyAddress", "http://connector-address")
                    .add("contractId", contractId)
                    .add("assetId", assetId)
                    .add("dataplaneMetadata", createObjectBuilder()
                            .add("labels", createArrayBuilder().add("label"))
                            .add("properties", createObjectBuilder().add("key", "value"))
                    )
                    .build();

            var id = context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4beta/transferprocesses/")
                    .then()
                    .log().ifError()
                    .statusCode(200)
                    .extract().jsonPath().getString(ID);

            assertThat(transferProcessStore.findById(id)).isNotNull().satisfies(transferProcess -> {
                assertThat(transferProcess.getDataplaneMetadata()).isNotNull();
            });
        }

        @Test
        void terminate(ManagementEndToEndTestContext context, TransferProcessStore store) {
            var id = UUID.randomUUID().toString();
            store.save(createTransferProcessBuilder(id).state(REQUESTED.code()).build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "TerminateTransfer")
                    .add("reason", "any")
                    .build();

            context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4beta/transferprocesses/" + id + "/terminate")
                    .then()
                    .log().ifError()
                    .statusCode(204);
        }

        @ParameterizedTest
        @ValueSource(strings = {"600", "STARTED"})
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

            var result = context.baseRequest()
                    .contentType(JSON)
                    .body(requestBody)
                    .post("/v4beta/transferprocesses/request")
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
                        "@context": ["%s"],
                        "@type": "QuerySpec",
                        "sortField": "stateTimestamp",
                        "sortOrder": "ASC",
                        "limit": 100,
                        "offset": 0
                    }
                    """.formatted(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2);
            var query = JacksonJsonLd.createObjectMapper()
                    .readValue(content, JsonObject.class);

            var result = context.baseRequest()
                    .contentType(JSON)
                    .body(query)
                    .post("/v4beta/transferprocesses/request")
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
                    .protocol(DATASPACE_PROTOCOL_HTTP_V_2025_1)
                    .assetId("asset-id")
                    .contractId("contractId")
                    .participantContextId("participantContextId")
                    .counterPartyAddress("http://connector/address")
                    .dataplaneMetadata(DataplaneMetadata.Builder.newInstance().label("label").property("key", "value").build());
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

        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .paramProvider(ManagementEndToEndTestContext.class, ManagementEndToEndTestContext::forContext)
                .build();
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        @Order(0)
        static PostgresqlEndToEndExtension postgres = new PostgresqlEndToEndExtension();

        @RegisterExtension
        static RuntimeExtension runtime = ComponentRuntimeExtension.Builder.newInstance()
                .name(Runtimes.ControlPlane.NAME)
                .modules(Runtimes.ControlPlane.MODULES)
                .modules(Runtimes.ControlPlane.SQL_MODULES)
                .endpoints(Runtimes.ControlPlane.ENDPOINTS.build())
                .configurationProvider(postgres::config)
                .paramProvider(ManagementEndToEndTestContext.class, ManagementEndToEndTestContext::forContext)
                .build();

    }

}
