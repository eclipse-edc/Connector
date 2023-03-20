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

package org.eclipse.edc.test.system.remote;

import org.eclipse.edc.test.system.remote.annotations.MinikubeIntegrationTest;
import org.eclipse.edc.test.system.utils.FileTransferConfiguration;
import org.eclipse.edc.test.system.utils.TransferTestRunner;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.CONSUMER_MANAGEMENT_PATH;
import static org.eclipse.edc.test.system.local.TransferRuntimeConfiguration.IDS_PATH;


/**
 * Runs {@see FileTransferAsClientSimulation}.
 */
@MinikubeIntegrationTest
public class FileTransferAsClientIntegrationTest {

    private static String getFromEnv(String env) {
        return Objects.requireNonNull(System.getenv(env), env + " must be set.");
    }

    @Test
    public void performFileTransfer() {

        var providerUrl = getFromEnv("PROVIDER_URL") + IDS_PATH + "/data";
        var destinationPath = getFromEnv("DESTINATION_PATH");
        var consumerUrl = getFromEnv("CONSUMER_URL") + CONSUMER_MANAGEMENT_PATH;

        var runner = new TransferTestRunner(new FileTransferConfiguration(consumerUrl, providerUrl, destinationPath));

        runner.executeTransfer();
    }
}
