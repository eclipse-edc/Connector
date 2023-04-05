/*
 *  Copyright (c) 2023 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.azure.datafactory;

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.ms.dataverse.MicrosoftDataverseSchema;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import static java.lang.String.format;

public enum DataFactoryPipelineType {
    /**
     * Pipeline copy binary data from one blob to another
     */
    BLOB_TO_BLOB,
    /**
     * Pipeline copy csv data from a blob to a dataverse
     */
    BLOB_TO_DATAVERSE;

    public static DataFactoryPipelineType fromRequest(DataFlowRequest request) {
        DataAddress sourceAddress = request.getSourceDataAddress();
        DataAddress destinationAddress = request.getDestinationDataAddress();

        if(sourceAddress.getType().equals(AzureBlobStoreSchema.TYPE) && destinationAddress.getType().equals(AzureBlobStoreSchema.TYPE)) {
            return BLOB_TO_BLOB;
        } else if(sourceAddress.getType().equals(AzureBlobStoreSchema.TYPE) && destinationAddress.getType().equals(MicrosoftDataverseSchema.TYPE)) {
            return BLOB_TO_DATAVERSE;
        }
        return null;
    }
}
