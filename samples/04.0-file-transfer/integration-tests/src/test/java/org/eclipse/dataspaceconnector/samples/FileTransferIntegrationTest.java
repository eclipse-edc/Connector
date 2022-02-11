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
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.junit.launcher.EdcRuntimeExtension;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.findUnallocatedServerPort;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;


/**
 * System Test for Sample 04.0-file-transfer
 */
public class FileTransferIntegrationTest {

    private static final String PROVIDER_ASSET_NAME = "test-document";

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

    private static final String CONSUMER_ASSET_PATH = propOrEnv("edc.samples.04.consumer.asset.path", tempDirectory());
    private static final int CONSUMER_CONNECTOR_PORT = findUnallocatedServerPort();
    private static final String CONSUMER_CONNECTOR_HOST = "http://localhost:" + CONSUMER_CONNECTOR_PORT;

    private static final int PROVIDER_CONNECTOR_PORT = findUnallocatedServerPort();
    private static final String PROVIDER_CONNECTOR_HOST = "http://localhost:" + PROVIDER_CONNECTOR_PORT;
    private static final String PROVIDER_ASSET_PATH = propOrEnv("edc.samples.04.asset.path", format("%s/%s.txt", tempDirectory(), PROVIDER_ASSET_NAME));

    private static final String API_KEY_CONTROL_AUTH = propOrEnv("edc.api.control.auth.apikey.value", "password");
    private static final String API_KEY_HEADER = "X-Api-Key";

    @RegisterExtension
    static EdcRuntimeExtension consumer = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:consumer",
            "consumer",
            Map.of(
                    "web.http.port", String.valueOf(CONSUMER_CONNECTOR_PORT),
                    "edc.api.control.auth.apikey.value", API_KEY_CONTROL_AUTH,
                    "ids.webhook.address", CONSUMER_CONNECTOR_HOST));

    @RegisterExtension
    static EdcRuntimeExtension provider = new EdcRuntimeExtension(
            ":samples:04.0-file-transfer:provider",
            "provider",
            Map.of(
                    "web.http.port", String.valueOf(PROVIDER_CONNECTOR_PORT),
                    "edc.samples.04.asset.path", PROVIDER_ASSET_PATH,
                    "ids.webhook.address", PROVIDER_CONNECTOR_HOST));

    @Test
    public void transferFile_success() throws Exception {
        // Arrange
        var contractOffer = TestUtils.getFileFromResourceName("contractoffer.json");
        // Create a file with test data on provider file system.
        var fileContent = "Sample04-test-" + UUID.randomUUID();
        Files.write(Path.of(PROVIDER_ASSET_PATH), fileContent.getBytes(StandardCharsets.UTF_8));

        // Act & Assert
        // Initiate a contract negotiation
        var contractNegotiationRequestId =
                givenConsumerRequest()
                        .contentType(ContentType.JSON)
                        .queryParam(CONNECTOR_ADDRESS_PARAM, format("%s/api/ids/multipart", PROVIDER_CONNECTOR_HOST))
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
                .extract().jsonPath().get("contractAgreement.id");

        // Initiate file transfer
        var transferProcessId =
                givenConsumerRequest()
                        .noContentType()
                        .pathParam(FILE_NAME_PARAM, PROVIDER_ASSET_NAME)
                        .queryParam(CONNECTOR_ADDRESS_PARAM, format("%s/api/ids/multipart", PROVIDER_CONNECTOR_HOST))
                        .queryParam(DESTINATION_PARAM, CONSUMER_ASSET_PATH)
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

        var copiedFilePath = Path.of(format(CONSUMER_ASSET_PATH + "/%s.txt", PROVIDER_ASSET_NAME));
        var actualFileContent = Files.readString(copiedFilePath);
        assertThat(actualFileContent)
                .withFailMessage("Transferred file contents are null")
                .isNotNull();
        assertThat(actualFileContent)
                .withFailMessage("Transferred file contents are not same as the source file")
                .isEqualTo(fileContent);
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
                        .header(API_KEY_HEADER, API_KEY_CONTROL_AUTH)
                .when()
                        .get(CONTRACT_AGREEMENT_PATH)
                .then()
                        .assertThat().statusCode(HttpStatus.SC_OK);
    }

    private RequestSpecification givenConsumerRequest() {
        return given()
                .baseUri(CONSUMER_CONNECTOR_HOST);
    }

    /**
     * Helper method to create a temporary directory.
     *
     * @return a newly create temporary directory.
     */
    private static String tempDirectory() {
        try {
            return Files.createTempDirectory(FileTransferIntegrationTest.class.getSimpleName()).toString();
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
