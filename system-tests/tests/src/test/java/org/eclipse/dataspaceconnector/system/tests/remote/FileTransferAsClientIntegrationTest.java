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

import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.system.tests.utils.FileTransferSimulationUtils;
import org.junit.jupiter.api.Test;

import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;

/**
 * Runs {@see FileTransferAsClientSimulation}.
 */
@IntegrationTest
public class FileTransferAsClientIntegrationTest {

    @Test
    public void performFileTransfer() {
        runGatling(FileTransferAsClientSimulation.class, FileTransferSimulationUtils.DESCRIPTION);
    }
}
