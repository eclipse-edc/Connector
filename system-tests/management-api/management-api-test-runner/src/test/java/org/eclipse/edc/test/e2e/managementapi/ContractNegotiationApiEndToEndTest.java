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

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.extensions.EdcRuntimeExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;
import static org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndInstance.createDatabase;
import static org.eclipse.edc.test.e2e.managementapi.Runtimes.inMemoryRuntime;
import static org.eclipse.edc.test.e2e.managementapi.Runtimes.postgresRuntime;
import static org.hamcrest.Matchers.is;

public class ContractNegotiationApiEndToEndTest {

    @Nested
    @EndToEndTest
    class InMemory extends Tests {

        @RegisterExtension
        public static final EdcRuntimeExtension RUNTIME = inMemoryRuntime();

        InMemory() {
            super(RUNTIME);
        }

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @RegisterExtension
        static final BeforeAllCallback CREATE_DATABASE = context -> createDatabase("runtime");

        @RegisterExtension
        public static final EdcRuntimeExtension RUNTIME = postgresRuntime();

        Postgres() {
            super(RUNTIME);
        }
    }

    abstract static class Tests extends ManagementApiEndToEndTestBase {

        private final String protocolUrl = "http://localhost:" + PROTOCOL_PORT + "/protocol";

        Tests(EdcRuntimeExtension runtime) {
            super(runtime);
        }

        @Test
        void getAll() {
            var store = getContractNegotiationStore();
            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            store.save(createContractNegotiation(id1));
            store.save(createContractNegotiation(id2));

            var jsonPath = baseRequest()
                    .contentType(JSON)
                    .body(createObjectBuilder()
                            .add(CONTEXT, createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                            .add("filterExpression", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add("operandLeft", "id")
                                            .add("operator", "in")
                                            .add("operandRight", createArrayBuilder().add(id1).add(id2))
                                    )
                            )
                            .build()
                    )
                    .post("/v2/contractnegotiations/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(2))
                    .extract().jsonPath();

            assertThat(jsonPath.getString("[0].counterPartyAddress")).isEqualTo(protocolUrl);
            assertThat(jsonPath.getString("[0].@id")).isIn(id1, id2);
            assertThat(jsonPath.getString("[1].@id")).isIn(id1, id2);
            assertThat(jsonPath.getString("[0].protocol")).isEqualTo("dataspace-protocol-http");
            assertThat(jsonPath.getString("[1].protocol")).isEqualTo("dataspace-protocol-http");
        }

        @Test
        void getById() {
            var store = getContractNegotiationStore();
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());

            var json = baseRequest()
                    .contentType(JSON)
                    .get("/v2/contractnegotiations/cn1")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().jsonPath();

            assertThat((String) json.get("@id")).isEqualTo("cn1");
            assertThat(json.getString("protocol")).isEqualTo("dataspace-protocol-http");
        }

        @Test
        void getState() {
            var store = getContractNegotiationStore();
            var state = ContractNegotiationStates.FINALIZED.code(); // all other states could be modified by the state machine
            store.save(createContractNegotiationBuilder("cn1").state(state).build());

            baseRequest()
                    .contentType(JSON)
                    .get("/v2/contractnegotiations/cn1/state")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("state", is("FINALIZED"));
        }

        @Test
        void getAgreementForNegotiation() {
            var store = getContractNegotiationStore();
            var agreement = createContractAgreement("cn1");
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

            var json = baseRequest()
                    .contentType(JSON)
                    .get("/v2/contractnegotiations/cn1/agreement")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().jsonPath();

            assertThat(json.getString("@id")).isEqualTo("cn1");
            assertThat((Object) json.get("policy")).isNotNull().isInstanceOf(Map.class);
            assertThat(json.getString("assetId")).isEqualTo(agreement.getAssetId());
        }

        @Test
        void initiateNegotiation() {

            var requestJson = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                    .add(TYPE, "ContractRequest")
                    .add("counterPartyAddress", "test-address")
                    .add("protocol", "test-protocol")
                    .add("providerId", "test-provider-id")
                    .add("callbackAddresses", createCallbackAddress())
                    .add("policy", createPolicy())
                    .build();

            var id = baseRequest()
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v2/contractnegotiations")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            var store = getContractNegotiationStore();

            assertThat(store.findById(id)).isNotNull();
        }

        @Test
        void terminate() {
            var store = getContractNegotiationStore();
            store.save(createContractNegotiationBuilder("cn1").build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE).build())
                    .add(ID, "cn1")
                    .add("reason", "any good reason")
                    .build();

            baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v2/contractnegotiations/cn1/terminate")
                    .then()
                    .log().ifError()
                    .statusCode(204);
        }

        private ContractNegotiation createContractNegotiation(String negotiationId) {
            return createContractNegotiationBuilder(negotiationId)
                    .build();
        }

        private ContractNegotiation.Builder createContractNegotiationBuilder(String negotiationId) {
            return ContractNegotiation.Builder.newInstance()
                    .id(negotiationId)
                    .correlationId(negotiationId)
                    .counterPartyId(randomUUID().toString())
                    .counterPartyAddress(protocolUrl)
                    .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                            .uri("local://test")
                            .events(Set.of("test-event1", "test-event2"))
                            .build()))
                    .protocol("dataspace-protocol-http")
                    .state(REQUESTED.code())
                    .contractOffer(contractOfferBuilder().build());
        }

        private ContractOffer.Builder contractOfferBuilder() {
            return ContractOffer.Builder.newInstance()
                    .id("test-offer-id")
                    .assetId(randomUUID().toString())
                    .policy(Policy.Builder.newInstance().build());
        }

        private ContractAgreement createContractAgreement(String negotiationId) {
            return ContractAgreement.Builder.newInstance()
                    .id(negotiationId)
                    .assetId(randomUUID().toString())
                    .consumerId(randomUUID() + "-consumer")
                    .providerId(randomUUID() + "-provider")
                    .policy(Policy.Builder.newInstance().build())
                    .build();
        }

        private JsonArrayBuilder createCallbackAddress() {
            var builder = Json.createArrayBuilder();
            return builder.add(createObjectBuilder()
                    .add(IS_TRANSACTIONAL, false)
                    .add(URI, "http://test.local/")
                    .add(EVENTS, Json.createArrayBuilder().build()));
        }

        private JsonObject createPolicy() {
            var permissionJson = createObjectBuilder().add(TYPE, "permission").build();
            var prohibitionJson = createObjectBuilder().add(TYPE, "prohibition").build();
            var dutyJson = createObjectBuilder().add(TYPE, "duty").build();
            return createObjectBuilder()
                    .add(CONTEXT, "http://www.w3.org/ns/odrl.jsonld")
                    .add(TYPE, "Offer")
                    .add(ID, "offer-id")
                    .add("permission", permissionJson)
                    .add("prohibition", prohibitionJson)
                    .add("obligation", dutyJson)
                    .add("assigner", "provider-id")
                    .add("target", "asset-id")
                    .build();
        }

        private ContractNegotiationStore getContractNegotiationStore() {
            return runtime.getContext().getService(ContractNegotiationStore.class);
        }
    }

}
