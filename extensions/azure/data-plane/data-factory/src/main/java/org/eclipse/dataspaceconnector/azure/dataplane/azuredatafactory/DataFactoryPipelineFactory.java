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

import com.azure.resourcemanager.datafactory.models.AzureBlobStorageLocation;
import com.azure.resourcemanager.datafactory.models.AzureKeyVaultSecretReference;
import com.azure.resourcemanager.datafactory.models.AzureStorageLinkedService;
import com.azure.resourcemanager.datafactory.models.BinaryDataset;
import com.azure.resourcemanager.datafactory.models.BlobSink;
import com.azure.resourcemanager.datafactory.models.BlobSource;
import com.azure.resourcemanager.datafactory.models.CopyActivity;
import com.azure.resourcemanager.datafactory.models.DatasetReference;
import com.azure.resourcemanager.datafactory.models.DatasetResource;
import com.azure.resourcemanager.datafactory.models.LinkedServiceReference;
import com.azure.resourcemanager.datafactory.models.LinkedServiceResource;
import com.azure.resourcemanager.datafactory.models.PipelineResource;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;

import java.util.List;
import java.util.UUID;

/**
 * Factory class for Azure Data Factory object definitions, such as pipelines and datasets.
 */
class DataFactoryPipelineFactory {
    private static final String ADF_RESOURCE_NAME_PREFIX = "EDC-DPF-";

    private final String keyVaultLinkedService;
    private final KeyVaultClient keyVaultClient;
    private final DataFactoryClient client;

    DataFactoryPipelineFactory(String keyVaultLinkedService, KeyVaultClient keyVaultClient, DataFactoryClient client) {
        this.keyVaultLinkedService = keyVaultLinkedService;
        this.keyVaultClient = keyVaultClient;
        this.client = client;
    }

    /**
     * Create a Data Factory pipeline for a transfer request.
     *
     * @param request the transfer request.
     * @return the created pipeline resource.
     */
    PipelineResource createPipeline(DataFlowRequest request) {
        var baseName = ADF_RESOURCE_NAME_PREFIX + UUID.randomUUID();

        var sourceDataset = createDataset(baseName + "-src", request.getSourceDataAddress());
        var destinationDataset = createDataset(baseName + "-dst", request.getDestinationDataAddress());

        return createCopyPipeline(baseName, sourceDataset, destinationDataset);
    }

    private PipelineResource createCopyPipeline(String baseName, DatasetResource sourceDataset, DatasetResource destinationDataset) {
        return client.definePipeline(baseName)
                .withActivities(List.of(new CopyActivity()
                        .withName("CopyActivity")
                        .withInputs(List.of(new DatasetReference().withReferenceName(sourceDataset.name())))
                        .withOutputs(List.of(new DatasetReference().withReferenceName(destinationDataset.name())))
                        .withSource(new BlobSource())
                        .withSink(new BlobSink())
                        .withValidateDataConsistency(true)))
                .create();
    }

    private DatasetResource createDataset(String name, DataAddress sourceDataAddress) {
        var linkedService = createLinkedService(name, sourceDataAddress);
        return createDatasetResource(name, linkedService, sourceDataAddress);
    }

    private DatasetResource createDatasetResource(String name, LinkedServiceResource linkedService, DataAddress dataAddress) {
        return client.defineDataset(name)
                .withProperties(
                        new BinaryDataset()
                                .withLinkedServiceName(new LinkedServiceReference().withReferenceName(linkedService.name()))
                                .withLocation(new AzureBlobStorageLocation()
                                        .withFileName(dataAddress.getProperty(AzureBlobStoreSchema.BLOB_NAME))
                                        .withContainer(dataAddress.getProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                                )
                )
                .create();
    }

    private LinkedServiceResource createLinkedService(String name, DataAddress dataAddress) {
        var accountName = dataAddress.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
        var accountKey = dataAddress.getProperty(AzureBlobStoreSchema.SHARED_KEY);

        var secret = keyVaultClient.setSecret(name, accountKey);

        return client.defineLinkedService(name)
                .withProperties(
                        new AzureStorageLinkedService()
                                .withConnectionString(String.format("DefaultEndpointsProtocol=https;AccountName=%s;", accountName))
                                .withAccountKey(
                                        new AzureKeyVaultSecretReference()
                                                .withSecretName(secret.getName())
                                                .withStore(new LinkedServiceReference()
                                                        .withReferenceName(keyVaultLinkedService)
                                                )))
                .create();
    }
}