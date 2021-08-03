/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.transfer.store.cosmos;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.spi.types.domain.metadata.DataEntry;
import org.eclipse.edc.spi.types.domain.transfer.*;

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

    @JsonTypeName("edc:dummycatalogentry")
    public static class DummyCatalogEntry implements org.eclipse.edc.spi.types.domain.metadata.DataCatalogEntry {
        @Override
        public DataAddress getAddress() {
            return DataAddress.Builder.newInstance().type("test-source-type").build();
        }
    }
}
