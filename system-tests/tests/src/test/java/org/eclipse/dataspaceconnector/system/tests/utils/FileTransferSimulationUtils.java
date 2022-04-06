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

package org.eclipse.dataspaceconnector.system.tests.utils;

import io.gatling.javaapi.core.ChainBuilder;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Objects;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.doWhileDuring;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static java.lang.String.format;

/**
 * Utility methods for building a Gatling simulation for performing contract negotiation and file transfer.
 */
public abstract class FileTransferSimulationUtils {

    public static final String CONTRACT_AGREEMENT_ID = "contractAgreementId";
    public static final String CONTRACT_NEGOTIATION_REQUEST_ID = "contractNegotiationRequestId";
    public static final String TRANSFER_PROCESS_ID = "transferProcessId";

    public static final String DESCRIPTION = "[Contract negotiation and file transfer]";
    public static final String OFFER_FILE = "contractoffer.json";

    public static final String PROVIDER_ASSET_NAME = "test-document";

    private static final String CONNECTOR_ADDRESS_PARAM = "connectorAddress";
    private static final String DESTINATION_PARAM = "destination";
    private static final String CONTRACT_ID_PARAM = "contractId";
    private static final String API_KEY_HEADER = "X-Api-Key";

    private FileTransferSimulationUtils() {
    }

    /**
     * Gatling chain for performing contract negotiation and file transfer.
     *
     * @param providerUrl     URL for the Provider API, as accessed from the Consumer runtime.
     * @param destinationPath File copy destination path. If it includes the character sequence {@code %s}, that sequence is replaced with a random string in each iteration.
     * @param apiKey          Consumer runtime API Key.
     */
    public static ChainBuilder contractNegotiationAndFileTransfer(String providerUrl, String destinationPath, String apiKey) {
        return startContractAgreement(providerUrl, loadContractAgreement())
                .exec(waitForContractAgreement(apiKey))
                .exec(startFileTransfer(providerUrl, destinationPath))
                .exec(waitForTransferCompletion());
    }

    /**
     * Gatling chain for initiating a contract negotiation.
     * <p>
     * Saves the Contract Negotiation Request ID into the {@see CONTRACT_NEGOTIATION_REQUEST_ID} session key.
     *
     * @param providerUrl       URL for the Provider API, as accessed from the Consumer runtime.
     * @param contractAgreement Contract agreement definition in JSON format.
     */
    private static ChainBuilder startContractAgreement(String providerUrl, String contractAgreement) {
        String connectorAddress = format("%s/api/v1/ids/data", providerUrl);
        return group("Contract negotiation")
                .on(exec(
                        // Initiate a contract negotiation
                        http("Contract negotiation")
                                .post("/api/negotiation")
                                .body(StringBody(contractAgreement))
                                .header(CONTENT_TYPE, "application/json")
                                .queryParam(CONNECTOR_ADDRESS_PARAM, connectorAddress)
                                .check(status().is(200))
                                .check(bodyString()
                                        .notNull()
                                        .saveAs(CONTRACT_NEGOTIATION_REQUEST_ID))
                ));
    }

    /**
     * Gatling chain for calling ContractNegotiation status endpoint repeatedly until a CONFIRMED state is
     * attained, or a timeout is reached.
     * <p>
     * Expects the Contract Negotiation Request ID to be provided in the {@see CONTRACT_NEGOTIATION_REQUEST_ID} session key.
     * <p>
     * Saves the Contract Agreement ID into the {@see CONTRACT_AGREEMENT_ID} session key.
     *
     * @param apiKey Consumer runtime API Key.
     */
    private static ChainBuilder waitForContractAgreement(String apiKey) {
        return exec(session -> session.set("status", -1))
                .group("Wait for agreement").on(
                        doWhileDuring(session -> session.getString(CONTRACT_AGREEMENT_ID) == null,
                                Duration.ofSeconds(30))
                                .on(exec(http("Get status")
                                                .get(session -> format("/api/control/negotiation/%s", session.getString(CONTRACT_NEGOTIATION_REQUEST_ID)))
                                                .header(API_KEY_HEADER, apiKey)
                                                .check(status().is(200))
                                                .check(
                                                        jmesPath("id").is(session -> session.getString(CONTRACT_NEGOTIATION_REQUEST_ID)),
                                                        jmesPath("state").saveAs("status")
                                                )
                                                .checkIf(
                                                        session -> Integer.toString(ContractNegotiationStates.CONFIRMED.code()).equals(session.getString("status"))
                                                )
                                                .then(
                                                        // Obtain contract agreement ID
                                                        jmesPath("contractAgreement.id").notNull().saveAs(CONTRACT_AGREEMENT_ID)
                                                )
                                        ).pace(Duration.ofSeconds(1))
                                )

                );
    }

    /**
     * Gatling chain for initiating a file transfer request.
     * <p>
     * Expects the Contract Agreement ID to be provided in the {@see CONTRACT_AGREEMENT_ID} session key.
     * <p>
     * Saves the Transfer Process ID into the {@see TRANSFER_PROCESS_ID} session key.
     *
     * @param providerUrl     URL for the Provider API, as accessed from the Consumer runtime.
     * @param destinationPath File copy destination path.
     */
    private static ChainBuilder startFileTransfer(String providerUrl, String destinationPath) {
        String connectorAddress = format("%s/api/v1/ids/data", providerUrl);
        return group("Initiate transfer")
                .on(exec(
                        // Initiate a file transfer
                        http("Initiate transfer")
                                .post(format("/api/file/%s", PROVIDER_ASSET_NAME))
                                .queryParam(CONNECTOR_ADDRESS_PARAM, connectorAddress)
                                .queryParam(DESTINATION_PARAM, destinationPath)
                                .queryParam(CONTRACT_ID_PARAM, s -> s.getString(CONTRACT_AGREEMENT_ID))
                                .check(status().is(200))
                                .check(bodyString()
                                        .notNull()
                                        .saveAs(TRANSFER_PROCESS_ID))
                ));
    }

    /**
     * Gatling chain for calling the transfer status endpoint repeatedly until a COMPLETED state is
     * attained, or a timeout is reached.
     * <p>
     * Expects the Transfer Process ID to be provided in the {@see TRANSFER_PROCESS_ID} session key.
     */
    private static ChainBuilder waitForTransferCompletion() {
        return group("Wait for transfer").on(
                exec(session -> session.set("status", -1))
                        .doWhileDuring(session -> session.getInt("status") != TransferProcessStates.COMPLETED.code(),
                                Duration.ofSeconds(30))
                        .on(exec(http("Get transfer status")
                                        .get(session -> format("/api/transfer/%s", session.getString(TRANSFER_PROCESS_ID)))
                                        .check(status().is(200))
                                        .check(
                                                jmesPath("id").is(session -> session.getString(TRANSFER_PROCESS_ID)),
                                                jmesPath("state").saveAs("status")
                                        )
                                ).pace(Duration.ofSeconds(1))
                        )
        );
    }

    private static String loadContractAgreement() {
        try {
            InputStream offerFileData = Thread.currentThread().getContextClassLoader().getResourceAsStream(OFFER_FILE);
            return new String(Objects.requireNonNull(offerFileData, OFFER_FILE + " not found").readAllBytes());
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
