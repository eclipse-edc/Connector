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

package org.eclipse.dataspaceconnector.system.tests.remote;

import io.gatling.javaapi.core.Simulation;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.dataspaceconnector.system.tests.FileTransferRequestFactory;

import java.util.Objects;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferLocalSimulation.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.DESCRIPTION;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.contractNegotiationAndTransfer;

/**
 * Runs a single iteration of contract negotiation and file transfer, getting settings from environment variables.
 */
public class FileTransferAsClientSimulation extends Simulation {

    public FileTransferAsClientSimulation() {
        setUp(scenario(DESCRIPTION)
                .repeat(1)
                .on(
                        contractNegotiationAndTransfer(
                                getFromEnv("PROVIDER_URL"),
                                new FileTransferRequestFactory(getFromEnv("DESTINATION_PATH")))
                )
                .injectOpen(atOnceUsers(1)))
                .protocols(http
                        .baseUrl(getFromEnv("CONSUMER_URL") + "/" + CONSUMER_MANAGEMENT_PATH))
                .assertions(
                        global().responseTime().max().lt(2000),
                        global().successfulRequests().percent().is(100.0)
                );
    }

    private static String getFromEnv(String env) {
        return Objects.requireNonNull(StringUtils.trimToNull(System.getenv(env)), env);
    }
}
