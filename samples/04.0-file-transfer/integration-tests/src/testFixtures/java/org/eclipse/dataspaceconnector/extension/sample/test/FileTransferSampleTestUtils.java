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
 *       Microsoft Corporation - initial test implementation for sample
 *
 */

package org.eclipse.dataspaceconnector.extension.sample.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.apache.http.HttpStatus;
import org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class FileTransferSampleTestUtils {

    static final ObjectMapper MAPPER = new ObjectMapper();

    //region constant test settings
    static final String INITIATE_CONTRACT_NEGOTIATION_URI = "http://localhost:9192/api/v1/data/contractnegotiations";
    static final String LOOK_UP_CONTRACT_AGREEMENT_URI = "http://localhost:9192/api/v1/data/contractnegotiations/{id}";
    static final String INITIATE_TRANSFER_PROCESS_URI = "http://localhost:9192/api/v1/data/transferprocess";
    static final String CONTRACT_OFFER_FILE_PATH = "samples/04.0-file-transfer/contractoffer.json";
    static final String TRANSFER_FILE_PATH = "samples/04.0-file-transfer/filetransfer.json";
    static final String DESTINATION_FILE_PATH = "samples/requested.test.txt";
    static final String API_KEY_HEADER_KEY = "X-Api-Key";
    static final String API_KEY_HEADER_VALUE = "password";
    static final File DESTINATION_FILE = getFileFromRelativePath(FileTransferSampleTestUtils.DESTINATION_FILE_PATH);
    //endregion

    //region changeable test settings
    final String sampleAssetFilePath;
    final File sampleAssetFile;
    Duration timeout = Duration.ofSeconds(15);
    Duration pollInterval = Duration.ofMillis(500);
    //endregion

    String contractNegotiationId;
    String contractAgreementId;

    FileTransferSampleTestUtils(@NotNull String sampleAssetFilePath) {
        this.sampleAssetFilePath = sampleAssetFilePath;
        this.sampleAssetFile  = getFileFromRelativePath(sampleAssetFilePath);
    }

    /**
     * Assert that prerequisites are fulfilled before running the test.
     * This assertion checks only whether the file to be copied is not existing already.
     */
    void assertTestPrerequisites() {
        assertThat(DESTINATION_FILE).doesNotExist();
    }

    /**
     * Remove files created while running the tests.
     * The copied file will be deleted.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void cleanTemporaryTestFiles() {
        DESTINATION_FILE.delete();
    }

    /**
     * Resolves a {@link File} instance from a relative path.
     */
    @NotNull
    static File getFileFromRelativePath(String relativePath) {
        return new File(TestUtils.findBuildRoot(), relativePath);
    }

    /**
     * Assert that the file to be copied exists at the expected location.
     * This method waits a duration which is defined in {@link FileTransferSampleTestUtils#timeout}.
     */
    void assertDestinationFileContent() {
        await().atMost(timeout).pollInterval(pollInterval).untilAsserted(()
                -> assertThat(DESTINATION_FILE).hasSameBinaryContentAs(sampleAssetFile));
    }

    /**
     * Assert that the marker file has been created at the expected location with the expected content.
     * This method waits a duration which is defined in {@link FileTransferSampleTestUtils#timeout}.
     */
    void assertFileContent(File markerFile, String markerFileContent) {
        await().atMost(timeout).pollInterval(pollInterval).untilAsserted(()
                -> assertThat(markerFile).hasContent(markerFileContent));
    }

    /**
     * Assert that a POST request to initiate a contract negotiation is successful.
     * This method corresponds to the command in the sample: {@code curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/contractoffer.json "http://localhost:9192/api/v1/data/contractnegotiations"}
     */
    void initiateContractNegotiation() {
        contractNegotiationId = RestAssured
            .given()
                .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                .contentType(ContentType.JSON)
                .body(new File(TestUtils.findBuildRoot(), CONTRACT_OFFER_FILE_PATH))
            .when()
                .post(INITIATE_CONTRACT_NEGOTIATION_URI)
            .then()
                .statusCode(HttpStatus.SC_OK)
                .body("id", not(emptyString()))
                .extract()
                .jsonPath()
                .get("id");
    }

    /**
     * Assert that a GET request to look up a contract agreement is successful.
     * This method corresponds to the command in the sample: {@code curl -X GET -H 'X-Api-Key: password' "http://localhost:9192/api/v1/data/contractnegotiations/{UUID}"}
     */
    void lookUpContractAgreementId() {
        // Wait for transfer to be completed.
        await().atMost(timeout).pollInterval(pollInterval)
                .untilAsserted(() ->
                contractAgreementId = RestAssured
                    .given()
                        .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                    .when()
                        .get(LOOK_UP_CONTRACT_AGREEMENT_URI, contractNegotiationId)
                    .then()
                        .statusCode(HttpStatus.SC_OK)
                        .body("state", equalTo("CONFIRMED"))
                        .body("contractAgreementId", not(emptyString()))
                        .extract().body().jsonPath().getString("contractAgreementId")
        );
    }

    /**
     * Assert that a POST request to initiate transfer process is successful.
     * This method corresponds to the command in the sample: {@code curl -X POST -H "Content-Type: application/json" -H "X-Api-Key: password" -d @samples/04.0-file-transfer/filetransfer.json "http://localhost:9192/api/v1/data/transferprocess"}
     *
     * @throws IOException Thrown if there was an error accessing the transfer request file defined in {@link FileTransferSampleTestUtils#TRANSFER_FILE_PATH}.
     */
    void requestTransferFile() throws IOException {
        var transferJsonFile = getFileFromRelativePath(TRANSFER_FILE_PATH);
        DataRequest sampleDataRequest = readAndUpdateDataRequestFromJsonFile(transferJsonFile, contractAgreementId);

        JsonPath jsonPath = RestAssured
                .given()
                    .headers(API_KEY_HEADER_KEY, API_KEY_HEADER_VALUE)
                    .contentType(ContentType.JSON)
                    .body(sampleDataRequest)
                .when()
                    .post(INITIATE_TRANSFER_PROCESS_URI)
                .then()
                    .statusCode(HttpStatus.SC_OK)
                    .body("id", not(emptyString()))
                    .extract()
                    .jsonPath();

        String transferProcessId = jsonPath.get("id");

        assertThat(transferProcessId).isNotEmpty();
    }

    /**
     * Reads a transfer request file with changed value for contract agreement ID and file destination path.
     *
     * @param transferJsonFile A {@link File} instance pointing to a JSON transfer request file.
     * @param contractAgreementId This string containing a UUID will be used as value for the contract agreement ID.
     * @return An instance of {@link DataRequest} with changed values for contract agreement ID and file destination path.
     * @throws IOException Thrown if there was an error accessing the file given in transferJsonFile.
     */
    static DataRequest readAndUpdateDataRequestFromJsonFile(File transferJsonFile, String contractAgreementId) throws IOException {
        // convert JSON file to map
        DataRequest sampleDataRequest = MAPPER.readValue(transferJsonFile, DataRequest.class);

        var changedAddressProperties = sampleDataRequest.getDataDestination().getProperties();
        changedAddressProperties.put("path", DESTINATION_FILE.getAbsolutePath());

        DataAddress newDataDestination = DataAddress.Builder.newInstance()
                .properties(changedAddressProperties)
                .build();

        return DataRequest.Builder.newInstance()
                // copy unchanged values from JSON file
                .id(sampleDataRequest.getId())
                .processId(sampleDataRequest.getProcessId())
                .connectorAddress(sampleDataRequest.getConnectorAddress())
                .protocol(sampleDataRequest.getProtocol())
                .connectorId(sampleDataRequest.getConnectorId())
                .assetId(sampleDataRequest.getAssetId())
                .destinationType(sampleDataRequest.getDestinationType())
                .transferType(sampleDataRequest.getTransferType())
                .managedResources(sampleDataRequest.isManagedResources())
                .properties(sampleDataRequest.getProperties())
                // set changed values
                .contractId(contractAgreementId)
                .dataDestination(newDataDestination)
                .build();
    }
}
