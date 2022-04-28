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

package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateBlobName;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateContainerName;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateSharedKey;

/**
 * Validator for {@link AzureDataFactoryTransferService}.
 */
class AzureDataFactoryTransferRequestValidator {

    /**
     * Returns true if this service can transfer the request.
     */
    boolean canHandle(DataFlowRequest request) {
        return AzureBlobStoreSchema.TYPE.equals(request.getSourceDataAddress().getType()) &&
                AzureBlobStoreSchema.TYPE.equals(request.getDestinationDataAddress().getType());
    }

    /**
     * Returns true if the request is valid.
     */
    @NotNull Result<Boolean> validate(DataFlowRequest request) {
        try {
            validateSource(request.getSourceDataAddress());
            validateDestination(request.getDestinationDataAddress());
        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }
        return Result.success(true);
    }

    private void validateSource(DataAddress source) {
        var properties = new HashMap<>(source.getProperties());
        validateBlobName(properties.remove(AzureBlobStoreSchema.BLOB_NAME));
        validateProperties(properties);
    }

    private void validateDestination(DataAddress destination) {
        var properties = new HashMap<>(destination.getProperties());
        validateProperties(properties);
    }

    private void validateProperties(HashMap<String, String> properties) {
        validateAccountName(properties.remove(AzureBlobStoreSchema.ACCOUNT_NAME));
        validateContainerName(properties.remove(AzureBlobStoreSchema.CONTAINER_NAME));
        validateSharedKey(properties.remove(AzureBlobStoreSchema.SHARED_KEY));
        properties.keySet().stream().filter(k -> !DataAddress.TYPE.equals(k)).findFirst().ifPresent(k -> {
            throw new IllegalArgumentException(format("Unexpected property %s", k));
        });
    }
}
