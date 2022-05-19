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
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiationStates;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
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
public abstract class TransferSimulationUtils {

    public static final String CONTRACT_AGREEMENT_ID = "contractAgreementId";
    public static final String CONTRACT_NEGOTIATION_REQUEST_ID = "contractNegotiationRequestId";
    public static final String TRANSFER_PROCESS_ID = "transferProcessId";

    public static final String DESCRIPTION = "[Contract negotiation and file transfer]";

    public static final String PROVIDER_ASSET_ID = "test-document";
    public static final String PROVIDER_ASSET_FILE = "text-document.txt";

    public static final String TRANSFER_SUCCESSFUL = "Transfer successful";

    public static final String TRANSFER_PROCESSES_PATH = "/transferprocess";
    public static final String IDS_PATH = "/api/v1/ids";

    private TransferSimulationUtils() {
    }

    /**
     * Gatling chain for performing contract negotiation and file transfer.
     *
     * @param providerUrl             URL for the Provider API, as accessed from the Consumer runtime.
     * @param simulationConfiguration Configuration for transfers.
     */
    public static ChainBuilder contractNegotiationAndTransfer(String providerUrl, TransferSimulationConfiguration simulationConfiguration) {
        return startContractAgreement(providerUrl)
                .exec(waitForContractAgreement())
                .exec(startTransfer(providerUrl, simulationConfiguration))
                .exec(waitForTransferCompletion(simulationConfiguration));
    }

    /**
     * Gatling chain for initiating a contract negotiation.
     * <p>
     * Saves the Contract Negotiation Request ID into the {@see CONTRACT_NEGOTIATION_REQUEST_ID} session key.
     *
     * @param providerUrl URL for the Provider API, as accessed from the Consumer runtime.
     */
    private static ChainBuilder startContractAgreement(String providerUrl) {
        var connectorAddress = getConnectorAddress(providerUrl);
        return group("Contract negotiation")
                .on(exec(initiateContractNegotiation(connectorAddress)));
    }

    @NotNull
    private static HttpRequestActionBuilder initiateContractNegotiation(String connectorAddress) {
        return http("Initiate contract negotiation")
                .post("/contractnegotiations")
                .body(StringBody(loadContractAgreement(connectorAddress)))
                .header(CONTENT_TYPE, "application/json")
                .check(status().is(200))
                .check(jmesPath("id")
                        .notNull()
                        .saveAs(CONTRACT_NEGOTIATION_REQUEST_ID));
    }

    /**
     * Gatling chain for calling ContractNegotiation status endpoint repeatedly until a CONFIRMED state is
     * attained, or a timeout is reached.
     * <p>
     * Expects the Contract Negotiation Request ID to be provided in the {@see CONTRACT_NEGOTIATION_REQUEST_ID} session key.
     * <p>
     * Saves the Contract Agreement ID into the {@see CONTRACT_AGREEMENT_ID} session key.
     */
    private static ChainBuilder waitForContractAgreement() {
        return exec(session -> session.set("status", -1))
                .group("Wait for agreement")
                .on(doWhileDuring(session -> contractAgreementNotCompleted(session), Duration.ofSeconds(30))
                        .on(exec(getContractStatus()).pace(Duration.ofSeconds(1)))
                )
                .exitHereIf(session -> contractAgreementNotCompleted(session));
    }

    private static boolean contractAgreementNotCompleted(Session session) {
        return session.getString(CONTRACT_AGREEMENT_ID) == null;
    }

    @NotNull
    private static HttpRequestActionBuilder getContractStatus() {
        return http("Get status")
                .get(session -> format("/contractnegotiations/%s", session.getString(CONTRACT_NEGOTIATION_REQUEST_ID)))
                .check(status().is(200))
                .check(
                        jmesPath("id").is(session -> session.getString(CONTRACT_NEGOTIATION_REQUEST_ID)),
                        jmesPath("state").saveAs("status")
                )
                .checkIf(
                        session -> ContractNegotiationStates.CONFIRMED.name().equals(session.getString("status"))
                ).then(
                        jmesPath("contractAgreementId").notNull().saveAs(CONTRACT_AGREEMENT_ID)
                );
    }

    /**
     * Gatling chain for initiating a file transfer request.
     * <p>
     * Expects the Contract Agreement ID to be provided in the {@see CONTRACT_AGREEMENT_ID} session key.
     * <p>
     * Saves the Transfer Process ID into the {@see TRANSFER_PROCESS_ID} session key.
     *
     * @param providerUrl             URL for the Provider API, as accessed from the Consumer runtime.
     * @param simulationConfiguration Configuration for transfers.
     */
    private static ChainBuilder startTransfer(String providerUrl, TransferSimulationConfiguration simulationConfiguration) {
        String connectorAddress = getConnectorAddress(providerUrl);
        return group("Initiate transfer")
                .on(exec(initiateTransfer(simulationConfiguration, connectorAddress)));
    }

    @NotNull
    private static HttpRequestActionBuilder initiateTransfer(TransferSimulationConfiguration simulationConfiguration, String connectorAddress) {
        return http("Initiate file transfer")
                .post(TRANSFER_PROCESSES_PATH)
                .body(StringBody(session -> simulationConfiguration.createTransferRequest(new TransferInitiationData(session.getString(CONTRACT_AGREEMENT_ID), connectorAddress))))
                .header(CONTENT_TYPE, "application/json")
                .check(status().is(200))
                .check(jmesPath("id")
                        .notNull()
                        .saveAs(TRANSFER_PROCESS_ID));
    }

    /**
     * Gatling chain for calling the transfer status endpoint repeatedly until a COMPLETED state is
     * attained, or a timeout is reached.
     * <p>
     * Expects the Transfer Process ID to be provided in the {@see TRANSFER_PROCESS_ID} session key.
     *
     * @param simulationConfiguration See {@link TransferSimulationConfiguration}
     */
    private static ChainBuilder waitForTransferCompletion(TransferSimulationConfiguration simulationConfiguration) {
        return group("Wait for transfer")
                .on(exec(session -> session.set("status", "INITIAL"))
                        .doWhileDuring(session -> transferNotCompleted(session),
                                simulationConfiguration.copyMaxDuration())
                        .on(exec(getTransferStatus()).pace(Duration.ofSeconds(1))))

                .exitHereIf(session -> transferNotCompleted(session))

                // Perform one additional request if the transfer successful.
                // This allows running Gatling assertions to validate that the transfer actually succeeded
                // (and timeout was not reached).
                .group(TRANSFER_SUCCESSFUL)
                .on(exec(getTransferStatus()));
    }

    @NotNull
    private static Boolean transferNotCompleted(Session session) {
        return !session.getString("status").equals(TransferProcessStates.COMPLETED.name());
    }

    @NotNull
    private static HttpRequestActionBuilder getTransferStatus() {
        return http("Get transfer status")
                .get(session -> format("/transferprocess/%s", session.getString(TRANSFER_PROCESS_ID)))
                .check(status().is(200))
                .check(
                        jmesPath("id").is(session -> session.getString(TRANSFER_PROCESS_ID)),
                        jmesPath("state").saveAs("status")
                );
    }

    private static String loadContractAgreement(String providerUrl) {
        var policy = Policy.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .permission(Permission.Builder.newInstance()
                        .target("test-document")
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .type(PolicyType.SET)
                .build();
        var request = Map.of(
                "connectorId", "provider",
                "connectorAddress", providerUrl,
                "protocol", "ids-multipart",
                "offer", Map.of(
                        "offerId", "1:1",
                        "assetId", PROVIDER_ASSET_ID,
                        "policy", policy
                )
        );

        return new TypeManager().writeValueAsString(request);
    }

    private static String getConnectorAddress(String baseUrl) {
        return format("%s%s/data", baseUrl, IDS_PATH);
    }
}
