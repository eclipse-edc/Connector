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
import org.eclipse.edc.junit.extensions.ComponentRuntimeExtension;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.test.e2e.managementapi.ManagementEndToEndTestContext;
import org.eclipse.edc.test.e2e.managementapi.Runtimes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.AGREED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.TERMINATED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ContractNegotiationApiV4EndToEndTest {

    @SuppressWarnings("JUnitMalformedDeclaration")
    abstract static class Tests {

        @Test
        void getAll(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            var id1 = UUID.randomUUID().toString();
            var id2 = UUID.randomUUID().toString();
            store.save(createContractNegotiationBuilder(id1).counterPartyAddress(context.providerProtocolUrl()).build());
            store.save(createContractNegotiationBuilder(id2).counterPartyAddress(context.providerProtocolUrl()).build());

            var jsonPath = context.baseRequest()
                    .contentType(JSON)
                    .body(createObjectBuilder()
                            .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                            .add(TYPE, "QuerySpec")
                            .add("filterExpression", createArrayBuilder()
                                    .add(createObjectBuilder()
                                            .add(TYPE, "Criterion")
                                            .add("operandLeft", "id")
                                            .add("operator", "in")
                                            .add("operandRight", createArrayBuilder().add(id1).add(id2))
                                    )
                                    .add(createObjectBuilder()
                                            .add(TYPE, "Criterion")
                                            .add("operandLeft", "pending")
                                            .add("operator", "=")
                                            .add("operandRight", false)
                                    )
                            )
                            .build()
                    )
                    .post("/v4beta/contractnegotiations/request")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("size()", is(2))
                    .extract().jsonPath();

            assertThat(jsonPath.getString("[0].counterPartyAddress")).isEqualTo(context.providerProtocolUrl());
            assertThat(jsonPath.getString("[0].@id")).isIn(id1, id2);
            assertThat(jsonPath.getString("[1].@id")).isIn(id1, id2);
            assertThat(jsonPath.getString("[0].protocol")).isEqualTo("dataspace-protocol-http");
            assertThat(jsonPath.getString("[1].protocol")).isEqualTo("dataspace-protocol-http");
            assertThat(jsonPath.getString("[0].@type")).isEqualTo("ContractNegotiation");
            assertThat(jsonPath.getString("[1].@type")).isEqualTo("ContractNegotiation");
            assertThat(jsonPath.getList("[0].@context")).contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2);
            assertThat(jsonPath.getList("[1].@context")).contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2);

        }

        @Test
        void getById(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());

            context.baseRequest()
                    .contentType(JSON)
                    .get("/v4beta/contractnegotiations/cn1")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body(TYPE, equalTo("ContractNegotiation"))
                    .body(ID, is("cn1"))
                    .body("protocol", equalTo("dataspace-protocol-http"));
        }

        @Test
        void getState(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            var state = ContractNegotiationStates.FINALIZED.code(); // all other states could be modified by the state machine
            store.save(createContractNegotiationBuilder("cn1").state(state).build());

            context.baseRequest()
                    .contentType(JSON)
                    .get("/v4beta/contractnegotiations/cn1/state")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body("state", is("FINALIZED"));
        }

        @Test
        void getAgreementForNegotiation(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            var agreement = createContractAgreement("cn1");
            store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

            context.baseRequest()
                    .contentType(JSON)
                    .get("/v4beta/contractnegotiations/cn1/agreement")
                    .then()
                    .statusCode(200)
                    .contentType(JSON)
                    .body(CONTEXT, contains(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .body(TYPE, equalTo("ContractAgreement"))
                    .body(ID, is("cn1"))
                    .body("assetId", equalTo(agreement.getAssetId()))
                    .body("policy.'@type'", equalTo("Agreement"));

        }

        @Test
        void initiateNegotiation(ManagementEndToEndTestContext context, ContractNegotiationStore store) {

            var requestJson = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(TYPE, "ContractRequest")
                    .add("counterPartyAddress", "test-address")
                    .add("protocol", "test-protocol")
                    .add("providerId", "test-provider-id")
                    .add("callbackAddresses", createCallbackAddress())
                    .add("policy", createPolicy())
                    .build();

            var id = context.baseRequest()
                    .contentType(JSON)
                    .body(requestJson)
                    .post("/v4beta/contractnegotiations")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .contentType(JSON)
                    .extract().jsonPath().getString(ID);

            assertThat(store.findById(id)).isNotNull();
        }

        @Test
        void terminate(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            store.save(createContractNegotiationBuilder("cn1").build());
            var requestBody = createObjectBuilder()
                    .add(CONTEXT, createArrayBuilder().add(EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2))
                    .add(ID, "cn1")
                    .add(TYPE, "TerminateNegotiation")
                    .add("reason", "any good reason")
                    .build();

            context.baseRequest()
                    .body(requestBody)
                    .contentType(JSON)
                    .post("/v4beta/contractnegotiations/cn1/terminate")
                    .then()
                    .log().ifError()
                    .statusCode(204);
        }

        @Test
        void deleteNegotiation_shouldSucceed(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            store.save(createContractNegotiationBuilder("cn1")
                    .state(TERMINATED.code()).build());

            context.baseRequest()
                    .contentType(JSON)
                    .delete("/v4beta/contractnegotiations/cn1")
                    .then()
                    .statusCode(204);

            assertThat(store.findById("cn1")).isNull();
        }

        @Test
        void deleteNegotiation_shouldFailDueToWrongState(ManagementEndToEndTestContext context, ContractNegotiationStore store) {
            store.save(createContractNegotiationBuilder("cn1")
                    .state(AGREED.code()).build());

            context.baseRequest()
                    .contentType(JSON)
                    .delete("/v4beta/contractnegotiations/cn1")
                    .then()
                    .statusCode(409);

            assertThat(store.findById("cn1")).isNotNull();
        }

        private ContractNegotiation.Builder createContractNegotiationBuilder(String negotiationId) {
            return ContractNegotiation.Builder.newInstance()
                    .id(negotiationId)
                    .correlationId(negotiationId)
                    .counterPartyId(randomUUID().toString())
                    .counterPartyAddress("http://counter-party/address")
                    .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                            .uri("local://test")
                            .events(Set.of("test-event1", "test-event2"))
                            .build()))
                    .protocol("dataspace-protocol-http")
                    .state(REQUESTED.code())
                    .participantContextId("participantContextId")
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
                    .policy(Policy.Builder.newInstance().type(PolicyType.CONTRACT).build())
                    .participantContextId("participantContextId")
                    .build();
        }

        private JsonArrayBuilder createCallbackAddress() {
            var builder = Json.createArrayBuilder();
            return builder.add(createObjectBuilder()
                    .add(TYPE, "CallbackAddress")
                    .add("transactional", false)
                    .add("uri", "http://test.local/")
                    .add("events", Json.createArrayBuilder().build()));
        }

        private JsonObject createPolicy() {
            var permissionJson = createObjectBuilder().add(TYPE, "permission")
                    .add("action", "use")
                    .build();
            var prohibitionJson = createObjectBuilder().add(TYPE, "prohibition")
                    .add("action", "use")
                    .build();
            var dutyJson = createObjectBuilder().add(TYPE, "duty")
                    .add("action", "use")
                    .build();

            return createObjectBuilder()
                    .add(CONTEXT, "http://www.w3.org/ns/odrl.jsonld")
                    .add(TYPE, "Offer")
                    .add(ID, "offer-id")
                    .add("permission", createArrayBuilder().add(permissionJson))
                    .add("prohibition", createArrayBuilder().add(prohibitionJson))
                    .add("obligation", createArrayBuilder().add(dutyJson))
                    .add("assigner", "provider-id")
                    .add("target", "asset-id")
                    .build();
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
