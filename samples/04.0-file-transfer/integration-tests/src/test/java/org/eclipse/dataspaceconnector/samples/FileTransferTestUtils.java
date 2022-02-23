/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.samples;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;


/**
 * Client utility for performing file transfer, including preliminary contract negotiation.
 */
public class FileTransferTestUtils {

    public static final String PROVIDER_ASSET_NAME = "test-document";

    private static final String CONTRACT_NEGOTIATION_PATH = "/api/negotiation";
    private static final String CONTRACT_AGREEMENT_PATH = "/api/control/negotiation/{contractNegotiationRequestId}";
    private static final String TRANSFER_PATH = "/api/transfer/{transferId}";
    private static final String FILE_TRANSFER_PATH = "/api/file/{filename}";

    private static final String CONNECTOR_ADDRESS_PARAM = "connectorAddress";
    private static final String CONTRACT_NEGOTIATION_REQUEST_ID_PARAM = "contractNegotiationRequestId";
    private static final String TRANSFER_ID_PARAM = "transferId";
    private static final String DESTINATION_PARAM = "destination";
    private static final String CONTRACT_ID_PARAM = "contractId";
    private static final String FILE_NAME_PARAM = "filename";

    private static final String API_KEY_HEADER = "X-Api-Key";

    private String consumerUrl;
    private String providerUrl;
    private String destinationPath;
    private String apiKey;

    public void setConsumerUrl(String consumerUrl) {
        this.consumerUrl = consumerUrl;
    }

    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void performFileTransfer(String contractAgreementId) {
        // Initiate file transfer
        var transferProcessId =
                givenConsumerRequest()
                        .noContentType()
                        .pathParam(FILE_NAME_PARAM, PROVIDER_ASSET_NAME)
                        .queryParam(CONNECTOR_ADDRESS_PARAM, format("%s/api/ids/multipart", providerUrl))
                        .queryParam(DESTINATION_PARAM, destinationPath)
                        .queryParam(CONTRACT_ID_PARAM, contractAgreementId)
                .when()
                        .post(FILE_TRANSFER_PATH)
                .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract().asString();

        // Verify TransferProcessId
        assertThat(transferProcessId).isNotNull();

        // Verify file transfer is completed and file contents
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchTransfer(transferProcessId)
                        .body("id", equalTo(transferProcessId))
                        .body("state", equalTo(TransferProcessStates.COMPLETED.code())
                        ));
    }

    public String negotiateContractAgreement() {
        // Arrange
        var contractOffer = getClass().getClassLoader().getResourceAsStream("contractoffer.json");
        assertThat(contractOffer)
                .withFailMessage("Resource not found")
                .isNotNull();

        // Act & Assert
        // Initiate a contract negotiation
        var contractNegotiationRequestId =
                givenConsumerRequest()
                        .contentType(ContentType.JSON)
                        .queryParam(CONNECTOR_ADDRESS_PARAM, format("%s/api/ids/multipart", providerUrl))
                        .body(contractOffer)
                .when()
                        .post(CONTRACT_NEGOTIATION_PATH)
                .then()
                        .assertThat().statusCode(HttpStatus.SC_OK)
                        .extract().asString();

        // UUID is returned to get the contract agreement negotiated between provider and consumer.
        assertThat(contractNegotiationRequestId)
                .withFailMessage("Contract negotiation requestId is null").isNotBlank();

        // Verify ContractNegotiation is CONFIRMED
        await().atMost(30, SECONDS).untilAsserted(() ->
                fetchNegotiatedAgreement(contractNegotiationRequestId)
                        .body("id", equalTo(contractNegotiationRequestId))
                        .body("state", equalTo(ContractNegotiationStates.CONFIRMED.code()))
                        .body("contractAgreement.id", notNullValue()));

        // Obtain contract agreement ID
        var contractAgreementId = fetchNegotiatedAgreement(contractNegotiationRequestId)
                .extract().jsonPath().getString("contractAgreement.id");
        return contractAgreementId;
    }

    private ValidatableResponse fetchTransfer(String transferProcessId) {
        return
                givenConsumerRequest()
                        .pathParam(TRANSFER_ID_PARAM, transferProcessId)
                .when()
                        .get(TRANSFER_PATH)
                .then()
                        .assertThat().statusCode(HttpStatus.SC_OK);
    }

    /**
     * Fetch negotiated contract agreement.
     *
     * @param contractNegotiationRequestId ID of the ongoing contract negotiation between consumer and provider.
     * @return Negotiation as {@link ValidatableResponse}.
     */
    private ValidatableResponse fetchNegotiatedAgreement(String contractNegotiationRequestId) {
        return
                givenConsumerRequest()
                        .pathParam(CONTRACT_NEGOTIATION_REQUEST_ID_PARAM, contractNegotiationRequestId)
                        .header(API_KEY_HEADER, apiKey)
                .when()
                        .get(CONTRACT_AGREEMENT_PATH)
                .then()
                        .assertThat().statusCode(HttpStatus.SC_OK);
    }

    private RequestSpecification givenConsumerRequest() {
        return given()
                        .baseUri(consumerUrl);
    }
}
