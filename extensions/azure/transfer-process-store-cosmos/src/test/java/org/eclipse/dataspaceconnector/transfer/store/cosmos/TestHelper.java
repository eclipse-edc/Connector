/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.store.cosmos;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.*;

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
                .dataEntry(DataEntry.Builder.newInstance()
                        .policyId("test-policyId")
                        .catalogEntry(new DummyCatalogEntry())
                        .build())
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
                .type(TransferProcess.Type.CLIENT)
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
