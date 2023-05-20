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

import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.azure.blob.api.BlobStoreApi;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.edc.azure.blob.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateAccountName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateContainerName;
import static org.eclipse.edc.azure.blob.validator.AzureStorageValidator.validateKeyName;
import static org.eclipse.edc.spi.types.domain.DataAddress.KEY_NAME;

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
        return validateRequest(request).mapTo();
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
        var dataAddress = request.getDestinationDataAddress();
        try {
            validateAccountName(dataAddress.getProperty(ACCOUNT_NAME));
            validateContainerName(dataAddress.getProperty(CONTAINER_NAME));
            validateKeyName(dataAddress.getProperty(KEY_NAME));
        } catch (IllegalArgumentException e) {
            return Result.failure("AzureStorage destination address is invalid: " + e.getMessage());
        }
        return VALID.mapTo();
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        Result<Void> validate = validateRequest(request);
        if (validate.failed()) {
            throw new EdcException(validate.getFailure().getMessages().toString());
        }

        var dataAddress = request.getDestinationDataAddress();
        var requestId = request.getId();

        var secret = vault.resolveSecret(dataAddress.getKeyName());
        var token = typeManager.readValue(secret, AzureSasToken.class);

        return AzureStorageDataSink.Builder.newInstance()
                .accountName(dataAddress.getProperty(ACCOUNT_NAME))
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
