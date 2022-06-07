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

package org.eclipse.dataspaceconnector.system.tests;

import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferType;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferInitiationData;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationConfiguration;

import java.util.Map;

import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_ID;

/**
 * Configuration for File transfer used in
 * {@link org.eclipse.dataspaceconnector.system.tests.local.FileTransferLocalSimulation}.
 * <p>
 * The result file content is not verified, as it would not work when running in minikube.
 * File content is verified in local integration test.
 */
public class FileTransferSimulationConfiguration implements TransferSimulationConfiguration {

    private final String destinationPath;

    public FileTransferSimulationConfiguration(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    @Override
    public String createTransferRequest(TransferInitiationData transferInitiationData) {
        var request = Map.of(
                "contractId", transferInitiationData.contractAgreementId,
                "assetId", PROVIDER_ASSET_ID,
                "connectorId", "consumer",
                "connectorAddress", transferInitiationData.connectorAddress,
                "protocol", "ids-multipart",
                "dataDestination", DataAddress.Builder.newInstance()
                        .type("File")
                        .property("path", destinationPath)
                        .build(),
                "managedResources", false,
                "transferType", TransferType.Builder.transferType()
                        .contentType("application/octet-stream")
                        .isFinite(true)
                        .build()
        );

        return new TypeManager().writeValueAsString(request);
    }
}
