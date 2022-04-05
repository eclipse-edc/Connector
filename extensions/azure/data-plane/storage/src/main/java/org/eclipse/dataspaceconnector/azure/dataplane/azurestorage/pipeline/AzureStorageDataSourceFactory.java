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

package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter.BlobAdapterFactory;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateBlobName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateContainerName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.validator.AzureStorageValidator.validateSharedKey;

/**
 * Instantiates {@link AzureStorageDataSource}s for requests whose source data type is {@link AzureBlobStoreSchema#TYPE}.
 */
public class AzureStorageDataSourceFactory implements DataSourceFactory {
    private final BlobAdapterFactory blobAdapterFactory;
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;

    public AzureStorageDataSourceFactory(BlobAdapterFactory blobAdapterFactory, RetryPolicy<Object> retryPolicy, Monitor monitor) {
        this.blobAdapterFactory = blobAdapterFactory;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return AzureBlobStoreSchema.TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var dataAddress = request.getSourceDataAddress();
        var properties = new HashMap<>(dataAddress.getProperties());
        try {
            validateAccountName(properties.remove(AzureBlobStoreSchema.ACCOUNT_NAME));
            validateContainerName(properties.remove(AzureBlobStoreSchema.CONTAINER_NAME));
            validateBlobName(properties.remove(AzureBlobStoreSchema.BLOB_NAME));
            validateSharedKey(properties.remove(AzureBlobStoreSchema.SHARED_KEY));
            properties.keySet().stream().filter(k -> !DataAddress.TYPE.equals(k)).findFirst().ifPresent(k -> {
                throw new IllegalArgumentException(format("Unexpected property %s", k));
            });
        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }
        return VALID;
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        Result<Boolean> validate = validate(request);
        if (validate.failed()) {
            throw new EdcException(validate.getFailure().getMessages().toString());
        }
        var dataAddress = request.getSourceDataAddress();

        return AzureStorageDataSource.Builder.newInstance()
                .accountName(dataAddress.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME))
                .containerName(dataAddress.getProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                .sharedKey(dataAddress.getProperty(AzureBlobStoreSchema.SHARED_KEY))
                .blobAdapterFactory(blobAdapterFactory)
                .blobName(dataAddress.getProperty(AzureBlobStoreSchema.BLOB_NAME))
                .requestId(request.getId())
                .retryPolicy(retryPolicy)
                .monitor(monitor)
                .build();
    }
}
