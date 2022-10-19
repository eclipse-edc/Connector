/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.dataspaceconnector.gcp.dataplane.storage;

import org.eclipse.dataspaceconnector.gcp.core.storage.GcsStoreSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.UUID;

public class TestFunctions {

    public static DataFlowRequest createRequest(DataAddress sourceDataAddress) {
        return DataFlowRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .processId(UUID.randomUUID().toString())
                .sourceDataAddress(sourceDataAddress)
                .destinationDataAddress(createDataAddress(GcsStoreSchema.TYPE).build())
                .build();
    }

    public static DataAddress.Builder createDataAddress(String type) {
        return DataAddress.Builder.newInstance().type(type);
    }

}
