/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - add functionalities
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractagreement;

import io.restassured.specification.RequestSpecification;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;
import static org.hamcrest.CoreMatchers.is;

@ExtendWith(EdcExtension.class)
public class ContractAgreementApiControllerIntegrationTest {

    private final int port = getFreePort();
    private final String authKey = "123456";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.data.port", String.valueOf(port),
                "web.http.data.path", "/api/v1/data",
                "edc.api.auth.key", authKey
        ));
    }

    @Test
    void getAllContractAgreements(ContractNegotiationStore store) {
        store.save(createContractNegotiation(UUID.randomUUID().toString(), createContractAgreement("agreementId")));

        baseRequest()
                .get("/contractagreements")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void getAllContractAgreements_withPaging(ContractNegotiationStore store) {
        store.save(createContractNegotiation(UUID.randomUUID().toString(), createContractAgreement("agreementId")));

        baseRequest()
                .get("/contractagreements?offset=0&limit=15&sort=ASC")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", Matchers.is(1));
    }

    @Test
    void getSingleContractAgreement(ContractNegotiationStore store) {
        store.save(createContractNegotiation(UUID.randomUUID().toString(), createContractAgreement("agreementId")));

        baseRequest()
                .get("/contractagreements/agreementId")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", Matchers.is("agreementId"));
    }

    @Test
    void getSingleContractAgreement_notFound() {
        baseRequest()
                .get("/contractagreements/nonExistingId")
                .then()
                .statusCode(404);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/api/v1/data")
                .header("x-api-key", authKey)
                .when();
    }


    private ContractNegotiation createContractNegotiation(String negotiationId, ContractAgreement contractAgreement) {
        return ContractNegotiation.Builder.newInstance()
                .id(negotiationId)
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol("protocol")
                .contractAgreement(contractAgreement)
                .build();
    }

    private ContractAgreement createContractAgreement(String agreementId) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerAgentId(UUID.randomUUID().toString())
                .consumerAgentId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }


}
