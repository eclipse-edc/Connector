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

package org.eclipse.dataspaceconnector.azure.blob.core;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.time.Instant;
import java.util.UUID;

public class AzureStorageTestFixtures {


    private AzureStorageTestFixtures() {
    }

    public static DataFlowRequest.Builder createRequest(String type) {
        return DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(createDataAddress(type).build())
                .destinationDataAddress(createDataAddress(type).build());
    }

    public static DataAddress.Builder createDataAddress(String type) {
        return DataAddress.Builder.newInstance().type(type);
    }

    public static String createAccountName() {
        return ("acct" + Instant.now().getEpochSecond()); // UUID would be too long
    }

    public static String createContainerName() {
        return ("cont-" + UUID.randomUUID()).toLowerCase();
    }

    public static String createBlobName() {
        return "blob-" + UUID.randomUUID();
    }

    public static String createSharedKey() {
        return "SK-" + UUID.randomUUID();
    }

    public static String createSharedAccessSignature() {
        return "SAS-" + UUID.randomUUID();
    }

}
