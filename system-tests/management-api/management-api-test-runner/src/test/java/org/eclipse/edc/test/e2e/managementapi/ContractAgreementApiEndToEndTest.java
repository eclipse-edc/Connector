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

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates.FINALIZED;
import static org.hamcrest.Matchers.is;

@EndToEndTest
public class ContractAgreementApiEndToEndTest extends BaseManagementApiEndToEndTest {

    @Test
    void getAll() {
        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);
        store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());
        store.save(createContractNegotiationBuilder("cn2").contractAgreement(createContractAgreement("cn2")).build());

        var jsonPath = baseRequest()
                .contentType(JSON)
                .post("/v2/contractagreements/request")
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
    }

    @Test
    void getById() {
        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);
        store.save(createContractNegotiationBuilder("cn1").contractAgreement(createContractAgreement("cn1")).build());

        var json = baseRequest()
                .contentType(JSON)
                .get("/v2/contractagreements/cn1")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath();

        assertThat(json.getString("@id")).isEqualTo("cn1");
        assertThat(json.getString("assetId")).isNotNull();
    }

    @Test
    void getNegotiationByAgreementId() {
        var store = controlPlane.getContext().getService(ContractNegotiationStore.class);
        store.save(createContractNegotiationBuilder("negotiation-id")
                .contractAgreement(createContractAgreement("agreement-id"))
                .build());

        var json = baseRequest()
                .contentType(JSON)
                .get("/v2/contractagreements/agreement-id/negotiation")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract().jsonPath();

        assertThat(json.getString("@id")).isEqualTo("negotiation-id");
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
                .state(FINALIZED.code());
    }

    private ContractOffer.Builder contractOfferBuilder() {
        return ContractOffer.Builder.newInstance()
                .id("test-offer-id")
                .policy(Policy.Builder.newInstance().build());
    }

    private ContractAgreement createContractAgreement(String negotiationId) {
        return ContractAgreement.Builder.newInstance()
                .id(negotiationId)
                .assetId(UUID.randomUUID().toString())
                .consumerId(UUID.randomUUID() + "-consumer")
                .providerId(UUID.randomUUID() + "-provider")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

}
