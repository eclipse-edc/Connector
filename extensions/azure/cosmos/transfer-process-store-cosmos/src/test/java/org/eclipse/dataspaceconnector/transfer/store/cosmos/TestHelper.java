/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.store.cosmos;

import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

public class TestHelper {

    public static ResourceManifest createManifest() {
        return ResourceManifest.Builder.newInstance()
                .build();
    }

    public static DataRequest createDataRequest() {
        return DataRequest.Builder.newInstance()
                .id("request-id")
                .dataDestination(DataAddress.Builder.newInstance()
                        .type("Test Address Type")
                        .keyName("Test Key Name")
                        .build())
                .assetId(Asset.Builder.newInstance().id("asset-id").build().getId())
                .processId("test-process-id")
                .build();
    }

    public static TransferProcess createTransferProcess() {
        return createTransferProcess("test-process");
    }

    public static TransferProcess createTransferProcess(String processId, TransferProcessStates state) {
        return TransferProcess.Builder.newInstance()
                .id(processId)
                .state(state.code())
                .type(TransferProcess.Type.CONSUMER)
                .dataRequest(createDataRequest())
                .resourceManifest(createManifest())
                .build();
    }

    public static TransferProcess createTransferProcess(String processId) {
        return createTransferProcess(processId, TransferProcessStates.UNSAVED);
    }

}
