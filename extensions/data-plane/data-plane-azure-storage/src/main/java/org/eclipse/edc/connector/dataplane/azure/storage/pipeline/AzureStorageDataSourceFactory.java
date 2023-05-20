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

package org.eclipse.edc.connector.dataplane.azure.storage.pipeline;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.BLOB_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateBlobName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateContainerName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateKeyName;

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
        return validateRequest(request).mapTo();
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
        var dataAddress = request.getSourceDataAddress();
        try {
            validateAccountName(dataAddress.getProperty(ACCOUNT_NAME));
            validateContainerName(dataAddress.getProperty(CONTAINER_NAME));
            validateBlobName(dataAddress.getProperty(BLOB_NAME));
            validateKeyName(dataAddress.getProperty(DataAddress.KEY_NAME));
        } catch (IllegalArgumentException e) {
            return Result.failure("AzureStorage source address is invalid: " + e.getMessage());
        }
        return VALID.mapTo();
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        validateRequest(request).orElseThrow(f -> new EdcException(f.getFailureDetail()));

        var dataAddress = request.getSourceDataAddress();

        return AzureStorageDataSource.Builder.newInstance()
                .accountName(dataAddress.getProperty(ACCOUNT_NAME))
                .containerName(dataAddress.getProperty(CONTAINER_NAME))
                .sharedKey(vault.resolveSecret(dataAddress.getKeyName()))
                .blobStoreApi(blobStoreApi)
                .blobName(dataAddress.getProperty(BLOB_NAME))
                .requestId(request.getId())
                .retryPolicy(retryPolicy)
                .monitor(monitor)
                .build();
    }
}
