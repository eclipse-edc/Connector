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

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static jakarta.json.Json.createObjectBuilder;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;
import static org.hamcrest.Matchers.is;

@EndToEndTest
public class ContractNegotiationApiEndToEndTest extends BaseManagementApiEndToEndTest {

    @Test
    void getAll() {
        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);
        store.save(createContractNegotiation("cn1"));
        store.save(createContractNegotiation("cn2"));

        var jsonPath = baseRequest()
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(2))
                .extract().jsonPath();

        // must use bracket notation when using keys with a colon
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Property_accessors
        assertThat(jsonPath.getString("[0]['edc:counterPartyAddress']")).isEqualTo("address");
        assertThat(jsonPath.getString("[0].@id")).isIn("cn1", "cn2");
        assertThat(jsonPath.getString("[1].@id")).isIn("cn1", "cn2");
        assertThat(jsonPath.getString("[0]['edc:protocol']")).isEqualTo("dataspace-protocol-http");
        assertThat(jsonPath.getString("[1]['edc:protocol']")).isEqualTo("dataspace-protocol-http");
    }

    @Test
    void getById() {
        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);
        store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());

        var json = baseRequest()
                .contentType(JSON)
                .get("cn1")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath();

        assertThat((String) json.get("@id")).isEqualTo("cn1");
        assertThat(json.getString("'edc:protocol'")).isEqualTo("dataspace-protocol-http");
    }

    @Test
    void getState() {
        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);
        var state = ContractNegotiationStates.FINALIZED.code(); // all other states could be modified by the state machine
        store.save(createContractNegotiationBuilder("cn1").state(state).build());

        baseRequest()
                .contentType(JSON)
                .get("cn1/state")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("'edc:state'", is("FINALIZED"));
    }

    @Test
    void getAgreementForNegotiation() {
        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);
        var agreement = createContractAgreement("cn1");
        store.save(createContractNegotiationBuilder("cn1").contractAgreement(agreement).build());

        var json = baseRequest()
                .contentType(JSON)
                .get("cn1/agreement")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath();

        assertThat(json.getString("@id")).isEqualTo("cn1");
        assertThat((Object) json.get("'edc:policy'")).isNotNull().isInstanceOf(Map.class);
        assertThat(json.getString("'edc:assetId'")).isEqualTo(agreement.getAssetId());
    }

    @Test
    void initiateNegotiation() {

        var requestJson = createObjectBuilder()
                .add(CONTEXT, createObjectBuilder().add(EDC_PREFIX, EDC_NAMESPACE))
                .add(TYPE, "NegotiationInitiateRequestDto")
                .add("connectorAddress", "test-address")
                .add("protocol", "test-protocol")
                .add("connectorId", "test-conn-id")
                .add("providerId", "test-provider-id")
                .add("consumerId", "test-consumer-id")
                .add("callbackAddresses", createCallbackAddress())
                .add("offer", createObjectBuilder()
                        .add("offerId", "test-offer-id")
                        .add("assetId", "test-asset")
                        .add("policy", createPolicy())
                        .build())
                .build();

        var id = baseRequest()
                .contentType(JSON)
                .body(requestJson)
                .post()
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath().getString(ID);

        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);

        assertThat(store.findById(id)).isNotNull();
    }

    @Test
    void cancel() {
        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);
        store.save(createContractNegotiationBuilder("cn1").build());

        baseRequest()
                .contentType(JSON)
                .post("cn1/cancel")
                .then()
                .statusCode(204);
    }

    @Test
    void decline() {
        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);
        store.save(createContractNegotiationBuilder("cn1").build());

        baseRequest()
                .contentType(JSON)
                .post("cn1/decline")
                .then()
                .statusCode(204);
    }

    private RequestSpecification baseRequest() {
        return given()
                .port(PORT)
                .basePath("/management/v2/contractnegotiations")
                .when();
    }

    private ContractNegotiation createContractNegotiation(String negotiationId) {
        return createContractNegotiationBuilder(negotiationId)
                .build();
    }

    private ContractNegotiation.Builder createContractNegotiationBuilder(String negotiationId) {
        return ContractNegotiation.Builder.newInstance()
                .id(negotiationId)
                .counterPartyId(randomUUID().toString())
                .counterPartyAddress("address")
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance()
                        .uri("local://test")
                        .events(Set.of("test-event1", "test-event2"))
                        .build()))
                .protocol("dataspace-protocol-http")
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
                .add("permission", permissionJson)
                .add("prohibition", prohibitionJson)
                .add("obligation", dutyJson)
                .build();
    }

}
