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

package org.eclipse.edc.connector.api.management.contractagreement;

import io.restassured.specification.RequestSpecification;
import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.spi.query.SortOrder.ASC;
import static org.hamcrest.CoreMatchers.is;

@ApiTest
@ExtendWith(EdcExtension.class)
public class ContractAgreementApiControllerIntegrationTest {

    private final int port = getFreePort();
    private final String authKey = "123456";

    @BeforeEach
    void setUp(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.management.port", String.valueOf(port),
                "web.http.management.path", "/api/v1/management",
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
    void getAll_invalidQuery() {
        baseRequest()
                .get("/contractagreements?limit=1&offset=-1&filter=&sortField=")
                .then()
                .statusCode(400);
    }

    @Test
    void queryAllContractAgreements(ContractNegotiationStore store) {
        store.save(createContractNegotiation(UUID.randomUUID().toString(), createContractAgreement("agreementId")));

        baseRequest()
                .contentType(JSON)
                .post("/contractagreements/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1));
    }

    @Test
    void queryAllContractAgreements_withPaging(ContractNegotiationStore store) {
        store.save(createContractNegotiation(UUID.randomUUID().toString(), createContractAgreement("agreementId")));

        baseRequest()
                .contentType(JSON)
                .body(QuerySpecDto.Builder.newInstance().offset(0).limit(15).sortOrder(ASC).build())
                .post("/contractagreements/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", Matchers.is(1));
    }

    @Test
    void queryAllContractAgreements_withFilter(ContractNegotiationStore store) {
        store.save(createContractNegotiation(UUID.randomUUID().toString(), createContractAgreement("agreementId")));

        baseRequest()
                .contentType(JSON)
                .body(QuerySpecDto.Builder.newInstance().filterExpression(List.of(CriterionDto.from("id", "=", "agreementId"))).build())
                .post("/contractagreements/request")
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
                .basePath("/api/v1/management")
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
