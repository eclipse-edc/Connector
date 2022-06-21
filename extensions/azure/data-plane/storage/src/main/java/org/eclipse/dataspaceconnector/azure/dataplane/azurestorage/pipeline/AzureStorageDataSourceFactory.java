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
import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApi;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateBlobName;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateContainerName;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateKeyName;

/**
 * Instantiates {@link AzureStorageDataSource}s for requests whose source data type is {@link AzureBlobStoreSchema#TYPE}.
 */
public class AzureStorageDataSourceFactory implements DataSourceFactory {
    private final BlobStoreApi blobStoreApi;
    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final Vault vault;

    public AzureStorageDataSourceFactory(BlobStoreApi blobStoreApi, RetryPolicy<Object> retryPolicy, Monitor monitor, Vault vault) {
        this.blobStoreApi = blobStoreApi;
        this.retryPolicy = retryPolicy;
        this.monitor = monitor;
        this.vault = vault;
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
            validateKeyName(properties.remove(DataAddress.KEY_NAME));
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
                .sharedKey(vault.resolveSecret(dataAddress.getKeyName()))
                .blobStoreApi(blobStoreApi)
                .blobName(dataAddress.getProperty(AzureBlobStoreSchema.BLOB_NAME))
                .requestId(request.getId())
                .retryPolicy(retryPolicy)
                .monitor(monitor)
                .build();
    }
}
