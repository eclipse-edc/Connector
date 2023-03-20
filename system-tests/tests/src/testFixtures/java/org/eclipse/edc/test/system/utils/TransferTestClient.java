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

package org.eclipse.edc.test.system.utils;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferRequestDto;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Simple client for testing transfer scenario
 */
public class TransferTestClient {

    public static final String TRANSFERPROCESS_ID_PATH = "/transferprocess/{id}";
    public static final String TRANSFERPROCESS_PATH = "/transferprocess";
    public static final String CATALOG_PATH = "/catalog";
    private static final String CONTRACT_AGREEMENT_PATH = "/contractnegotiations/{contractNegotiationRequestId}";
    private static final String CONTRACT_NEGOTIATION_PATH = "/contractnegotiations";
    private final String consumerUrl;

    public TransferTestClient(String consumerUrl) {
        this.consumerUrl = consumerUrl;
    }


    public Catalog getCatalog(String providerUrl) {
        return givenConsumerRequest()
                .queryParam("providerUrl", providerUrl)
                .get(CATALOG_PATH)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK)
                .extract().as(Catalog.class);

    }

    public String negotiateContractAgreement(NegotiationInitiateRequestDto dto) {
        var contractNegotiationRequestId =
                givenConsumerRequest()
                        .contentType(ContentType.JSON)
                        .body(dto)
                        .when()
                        .post(CONTRACT_NEGOTIATION_PATH)
                        .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract().body().jsonPath().getString("id");


        assertThat(contractNegotiationRequestId)
                .withFailMessage("Contract negotiation requestId is null").isNotBlank();

        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchNegotiatedAgreement(contractNegotiationRequestId)
                        .body("id", equalTo(contractNegotiationRequestId))
                        .body("state", equalTo(ContractNegotiationStates.PROVIDER_FINALIZED.name()))
                        .body("contractAgreementId", notNullValue()));

        return fetchNegotiatedAgreement(contractNegotiationRequestId)
                .extract().jsonPath().getString("contractAgreementId");

    }

    public Map<String, String> initiateTransfer(TransferRequestDto dto) {
        var transferProcessId =
                givenConsumerRequest()
                        .contentType(ContentType.JSON)
                        .body(dto)
                        .when()
                        .post(TRANSFERPROCESS_PATH)
                        .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract().body().jsonPath().getString("id");

        assertThat(transferProcessId)
                .withFailMessage("TransferProcess Id is null").isNotBlank();

        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransferProcess(transferProcessId)
                        .body("id", equalTo(transferProcessId))
                        .body("state", equalTo(TransferProcessStates.COMPLETED.name())));

        return fetchTransferProcess(transferProcessId).extract().jsonPath().get("dataDestination.properties");
    }

    private ValidatableResponse fetchNegotiatedAgreement(String contractNegotiationRequestId) {
        return givenConsumerRequest()
                .get(CONTRACT_AGREEMENT_PATH, contractNegotiationRequestId)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
    }

    private ValidatableResponse fetchTransferProcess(String transferProcessId) {
        return givenConsumerRequest()
                .get(TRANSFERPROCESS_ID_PATH, transferProcessId)
                .then()
                .assertThat().statusCode(HttpStatus.SC_OK);
    }

    private RequestSpecification givenConsumerRequest() {
        return given()
                .baseUri(consumerUrl);
    }
}
