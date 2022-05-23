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

import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureSasToken;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApi;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.dataspaceconnector.azure.blob.core.validator.AzureStorageValidator.validateContainerName;

/**
 * Instantiates {@link AzureStorageDataSink}s for requests whose source data type is {@link AzureBlobStoreSchema#TYPE}.
 */
public class AzureStorageDataSinkFactory implements DataSinkFactory {
    private final BlobStoreApi blobStoreApi;
    private final ExecutorService executorService;
    private final int partitionSize;
    private final Monitor monitor;
    private final Vault vault;
    private final TypeManager typeManager;

    public AzureStorageDataSinkFactory(BlobStoreApi blobStoreApi, ExecutorService executorService, int partitionSize, Monitor monitor, Vault vault, TypeManager typeManager) {
        this.blobStoreApi = blobStoreApi;
        this.executorService = executorService;
        this.partitionSize = partitionSize;
        this.monitor = monitor;
        this.vault = vault;
        this.typeManager = typeManager;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return AzureBlobStoreSchema.TYPE.equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var dataAddress = request.getDestinationDataAddress();
        var properties = new HashMap<>(dataAddress.getProperties());
        try {
            validateAccountName(properties.remove(AzureBlobStoreSchema.ACCOUNT_NAME));
            validateContainerName(properties.remove(AzureBlobStoreSchema.CONTAINER_NAME));
            properties.remove(DataAddress.KEY_NAME);
            properties.keySet().stream().filter(k -> !DataAddress.TYPE.equals(k)).findFirst().ifPresent(k -> {
                throw new IllegalArgumentException(format("Unexpected property %s", k));
            });
        } catch (IllegalArgumentException e) {
            return Result.failure(e.getMessage());
        }
        return VALID;
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        Result<Boolean> validate = validate(request);
        if (validate.failed()) {
            throw new EdcException(validate.getFailure().getMessages().toString());
        }

        var dataAddress = request.getDestinationDataAddress();
        var requestId = request.getId();

        var secret = vault.resolveSecret(dataAddress.getKeyName());
        var token = typeManager.readValue(secret, AzureSasToken.class);

        return AzureStorageDataSink.Builder.newInstance()
                .accountName(dataAddress.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME))
                .containerName(dataAddress.getProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                .sharedAccessSignature(token.getSas())
                .requestId(requestId)
                .partitionSize(partitionSize)
                .blobStoreApi(blobStoreApi)
                .executorService(executorService)
                .monitor(monitor)
                .build();
    }
}
