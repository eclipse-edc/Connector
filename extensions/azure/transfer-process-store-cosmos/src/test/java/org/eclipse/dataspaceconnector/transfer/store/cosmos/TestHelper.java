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

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceManifest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import static org.eclipse.dataspaceconnector.spi.types.domain.asset.AssetProperties.POLICY_ID;

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
                .asset(Asset.Builder.newInstance().id("asset-id").property(POLICY_ID, "test-policyId").build())
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

    @JsonTypeName("dataspaceconnector:dummycatalogentry")
    public static class DummyCatalogEntry implements org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataCatalogEntry {
        @Override
        public DataAddress getAddress() {
            return DataAddress.Builder.newInstance().type("test-source-type").build();
        }
    }
}
