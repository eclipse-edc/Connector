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

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.API_KEY_CONTROL_AUTH;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.CONSUMER_ASSET_PATH;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.CONSUMER_CONNECTOR_HOST;
import static org.eclipse.dataspaceconnector.system.tests.local.FileTransferIntegrationTest.PROVIDER_CONNECTOR_HOST;
import static org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils.DESCRIPTION;
import static org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils.contractNegotiationAndFileTransfer;

/**
 * Runs a single iteration of contract negotiation and file transfer, getting settings from
 * {@see FileTransferIntegrationTest}.
 */
public class FileTransferLocalSimulation extends Simulation {

    public FileTransferLocalSimulation() {

        setUp(scenario(DESCRIPTION)
                .repeat(1)
                .on(
                        contractNegotiationAndFileTransfer(PROVIDER_CONNECTOR_HOST, CONSUMER_ASSET_PATH, API_KEY_CONTROL_AUTH)
                )
                .injectOpen(atOnceUsers(1)))
                .protocols(http
                        .baseUrl(CONSUMER_CONNECTOR_HOST))
                .assertions(
                        global().responseTime().max().lt(2000),
                        global().successfulRequests().percent().is(100.0)
                );
    }
}
