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

package org.eclipse.dataspaceconnector.system.tests.local;

import io.gatling.javaapi.core.Simulation;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationConfiguration;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;
import static org.eclipse.dataspaceconnector.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.DESCRIPTION;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.TRANSFER_SUCCESSFUL;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.contractNegotiationAndTransfer;

/**
 * Runs a single iteration of contract negotiation and file transfer.
 */
public abstract class TransferLocalSimulation extends Simulation {
    public static final int CONSUMER_CONNECTOR_PORT = getFreePort();
    public static final int CONSUMER_MANAGEMENT_PORT = getFreePort();
    public static final String CONSUMER_CONNECTOR_PATH = "/api";
    public static final String CONSUMER_MANAGEMENT_PATH = "/api/v1/data";
    public static final String CONSUMER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + CONSUMER_MANAGEMENT_PORT;
    public static final int CONSUMER_IDS_API_PORT = getFreePort();
    public static final String CONSUMER_IDS_API = "http://localhost:" + CONSUMER_IDS_API_PORT;

    public static final int PROVIDER_CONNECTOR_PORT = getFreePort();
    public static final int PROVIDER_MANAGEMENT_PORT = getFreePort();
    public static final String PROVIDER_CONNECTOR_PATH = "/api";
    public static final String PROVIDER_MANAGEMENT_PATH = "/api/v1/data";
    public static final String PROVIDER_CONNECTOR_MANAGEMENT_URL = "http://localhost:" + PROVIDER_MANAGEMENT_PORT;
    public static final int PROVIDER_IDS_API_PORT = getFreePort();
    public static final String PROVIDER_IDS_API = "http://localhost:" + PROVIDER_IDS_API_PORT;

    public static final String IDS_PATH = "/api/v1/ids";
    private static final int REPEAT = Integer.parseInt(propOrEnv("repeat", "1"));
    private static final int AT_ONCE_USERS = Integer.parseInt(propOrEnv("at.once.users", "1"));
    private static final int MAX_RESPONSE_TIME = Integer.parseInt(propOrEnv("max.response.time", "10000"));
    private static final double SUCCESS_PERCENTAGE = Double.parseDouble(propOrEnv("success.percentage", "100.0"));

    public TransferLocalSimulation(TransferSimulationConfiguration simulationConfiguration) {
        setUp(scenario(DESCRIPTION)
                .repeat(REPEAT)
                .on(contractNegotiationAndTransfer(PROVIDER_IDS_API, simulationConfiguration))
                .injectOpen(atOnceUsers(AT_ONCE_USERS)))
                .protocols(http.baseUrl(CONSUMER_CONNECTOR_MANAGEMENT_URL + CONSUMER_MANAGEMENT_PATH))
                .assertions(
                        details(TRANSFER_SUCCESSFUL).successfulRequests().count().is((long) AT_ONCE_USERS * REPEAT),
                        global().responseTime().max().lt(MAX_RESPONSE_TIME),
                        global().successfulRequests().percent().is(SUCCESS_PERCENTAGE)
                );
    }
}
