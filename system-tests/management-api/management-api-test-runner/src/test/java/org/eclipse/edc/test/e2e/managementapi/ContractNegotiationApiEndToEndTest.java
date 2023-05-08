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
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_OFFER;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@EndToEndTest
public class ContractNegotiationApiEndToEndTest extends BaseManagementApiEndToEndTest {

    // these constants must be identical to the ones defined in NegotiationInitiateRequestDto
    public static final String INITIATE_TYPE = EDC_NAMESPACE + "NegotiationInitiateRequestDto";
    public static final String CONNECTOR_ADDRESS = EDC_NAMESPACE + "connectorAddress";
    public static final String PROTOCOL = EDC_NAMESPACE + "protocol";
    public static final String CONNECTOR_ID = EDC_NAMESPACE + "connectorId";
    public static final String PROVIDER_ID = EDC_NAMESPACE + "providerId";
    public static final String CONSUMER_ID = EDC_NAMESPACE + "consumerId";
    public static final String OFFER = EDC_NAMESPACE + "offer";
    public static final String CALLBACK_ADDRESSES = EDC_NAMESPACE + "callbackAddresses";
    public static final String OFFER_ID = EDC_NAMESPACE + "offerId";
    public static final String ASSET_ID = EDC_NAMESPACE + "assetId";
    public static final String POLICY = EDC_NAMESPACE + "policy";


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

        var json = baseRequest()
                .contentType(JSON)
                .get("cn1/state")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath();

        assertThat((String) json.get("state")).isEqualTo("FINALIZED");
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

        var requestJson = Json.createObjectBuilder()
                .add(TYPE, INITIATE_TYPE)
                .add(CONNECTOR_ADDRESS, "test-address")
                .add(PROTOCOL, "test-protocol")
                .add(CONNECTOR_ID, "test-conn-id")
                .add(PROVIDER_ID, "test-provider-id")
                .add(CONSUMER_ID, "test-consumer-id")
                .add(CALLBACK_ADDRESSES, createCallbackAddress())
                .add(OFFER, Json.createObjectBuilder()
                        .add(OFFER_ID, "test-offer-id")
                        .add(ASSET_ID, "test-asset")
                        .add(POLICY, createPolicy())
                        .build())
                .build();

        var jsonPath = baseRequest()
                .contentType(JSON)
                .body(requestJson)
                .post()
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", notNullValue())
                .extract().body().jsonPath();

        var id = jsonPath.getString("id");

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
                .baseUri("http://localhost:" + PORT + "/management/v2/contractnegotiations")
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
        return builder.add(Json.createObjectBuilder()
                .add(IS_TRANSACTIONAL, true)
                .add(URI, "http://test.local/")
                .add(EVENTS, Json.createArrayBuilder().build()));
    }

    private JsonObject createPolicy() {
        var permissionJson = getJsonObject("permission");
        var prohibitionJson = getJsonObject("prohibition");
        var dutyJson = getJsonObject("duty");
        return Json.createObjectBuilder()
                .add(TYPE, ODRL_POLICY_TYPE_OFFER)
                .add(ODRL_PERMISSION_ATTRIBUTE, permissionJson)
                .add(ODRL_PROHIBITION_ATTRIBUTE, prohibitionJson)
                .add(ODRL_OBLIGATION_ATTRIBUTE, dutyJson)
                .build();
    }

    private JsonObject getJsonObject(String type) {
        return Json.createObjectBuilder()
                .add(TYPE, type)
                .build();
    }
}
