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
import org.eclipse.edc.test.system.utils.TransferSimulationUtils;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.test.system.utils.GatlingUtils.runGatling;


/**
 * Runs {@see FileTransferAsClientSimulation}.
 */
@MinikubeIntegrationTest
public class FileTransferAsClientIntegrationTest {

    @Test
    public void performFileTransfer() {
        runGatling(FileTransferAsClientSimulation.class, TransferSimulationUtils.DESCRIPTION);
    }
}
