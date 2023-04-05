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

package org.eclipse.edc.connector.dataplane.azure.datafactory;

import com.azure.resourcemanager.datafactory.models.AzureBlobStorageLocation;
import com.azure.resourcemanager.datafactory.models.AzureKeyVaultSecretReference;
import com.azure.resourcemanager.datafactory.models.AzureStorageLinkedService;
import com.azure.resourcemanager.datafactory.models.BinaryDataset;
import com.azure.resourcemanager.datafactory.models.BlobSink;
import com.azure.resourcemanager.datafactory.models.*;
import com.azure.resourcemanager.datafactory.models.BlobSource;
import com.azure.resourcemanager.datafactory.models.CopyActivity;
import com.azure.resourcemanager.datafactory.models.DatasetReference;
import com.azure.resourcemanager.datafactory.models.DatasetResource;
import com.azure.resourcemanager.datafactory.models.LinkedServiceReference;
import com.azure.resourcemanager.datafactory.models.LinkedServiceResource;
import com.azure.resourcemanager.datafactory.models.PipelineResource;
import org.eclipse.edc.azure.blob.AzureBlobStoreSchema;
import org.eclipse.edc.ms.dataverse.MicrosoftDataverseSchema;
import org.eclipse.edc.azure.blob.AzureSasToken;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;

import java.util.*;

import static java.lang.String.format;
import static org.eclipse.edc.spi.types.domain.DataAddress.KEY_NAME;

/**
 * Factory class for Azure Data Factory object definitions, such as pipelines and datasets.
 */
class DataFactoryPipelineFactory {
    private static final String ADF_RESOURCE_NAME_PREFIX = "EDC-DPF-";
    private static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net/";

    private final String keyVaultLinkedService;
    private final KeyVaultClient keyVaultClient;
    private final DataFactoryClient client;
    private final TypeManager typeManager;

    DataFactoryPipelineFactory(String keyVaultLinkedService, KeyVaultClient keyVaultClient, DataFactoryClient client, TypeManager typeManager) {
        this.keyVaultLinkedService = keyVaultLinkedService;
        this.keyVaultClient = keyVaultClient;
        this.client = client;
        this.typeManager = typeManager;
    }

    /**
     * Create a Data Factory pipeline for a transfer request.
     *
     * @param request the transfer request.
     * @return the created pipeline resource.
     */
    PipelineResource createPipeline(DataFlowRequest request) {
        var baseName = ADF_RESOURCE_NAME_PREFIX + UUID.randomUUID();
        DataFactoryPipelineType pipelineType = DataFactoryPipelineType.fromRequest(request);
        var sourceDataAddress = request.getSourceDataAddress();
        var destinationDataAddress = request.getDestinationDataAddress();

        switch (pipelineType) {
            case BLOB_TO_BLOB:
                return createBlobToBlobCopyPipeline(baseName, sourceDataAddress, destinationDataAddress);
            case BLOB_TO_DATAVERSE:
                return createBlobToDataverseCopyPipeline(baseName, sourceDataAddress, destinationDataAddress);
            default:
                throw new UnsupportedOperationException(format("Unsupported source or destination types: %s and %s", sourceDataAddress.getType(), destinationDataAddress.getType()));
        }
    }

    private PipelineResource createBlobToDataverseCopyPipeline(String baseName, DataAddress srcDataAddress, DataAddress dstDataAddress) {
        var srcName = baseName + "-src";
        var dstName = baseName + "-dst";
        LinkedServiceResource srcLinkedService = createSourceLinkedService(srcName, srcDataAddress);
        DelimitedTextDataset srcDataset = new DelimitedTextDataset()
                .withLinkedServiceName(new LinkedServiceReference().withReferenceName(srcLinkedService.name()))
                .withLocation(new AzureBlobStorageLocation()
                        .withFileName(srcDataAddress.getProperty(AzureBlobStoreSchema.BLOB_NAME))
                        .withContainer(srcDataAddress.getProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                )
                .withFirstRowAsHeader(true);
        DatasetResource srcDatasetResource = client.defineDataset(srcName).withProperties(srcDataset).create();
        DelimitedTextSource source = new DelimitedTextSource();

        LinkedServiceResource dstLinkedService = createDestinationLinkedService(dstName, dstDataAddress);
        DynamicsCrmEntityDataset dstDataset = new DynamicsCrmEntityDataset()
                .withLinkedServiceName(new LinkedServiceReference().withReferenceName(dstLinkedService.name()))
                .withEntityName(dstDataAddress.getProperty(MicrosoftDataverseSchema.ENTITY_NAME));
        DatasetResource dstDatasetResource = client.defineDataset(dstName).withProperties(dstDataset).create();
        DynamicsCrmSink sink = new DynamicsCrmSink()
                .withWriteBehavior(DynamicsSinkWriteBehavior.UPSERT)
                .withWriteBatchSize(1000);

        return client.definePipeline(baseName)
                .withActivities(List.of(new CopyActivity()
                        .withName("CopyActivity")
                        .withInputs(List.of(new DatasetReference().withReferenceName(srcDatasetResource.name())))
                        .withOutputs(List.of(new DatasetReference().withReferenceName(dstDatasetResource.name())))
                        .withSource(source)
                        .withSink(sink)
                        .withTranslator(new TabularTranslator()
                                .withTypeConversion(true)
                                .withTypeConversionSettings(new TypeConversionSettings()
                                        .withAllowDataTruncation(true)
                                        .withTreatBooleanAsNumber(false)
                                )
                        )
                ))
                .create();
    }

    private PipelineResource createBlobToBlobCopyPipeline(String baseName, DataAddress srcDataAddress, DataAddress dstDataAddress) {
        var srcName = baseName + "-src";
        var dstName = baseName + "-dst";
        LinkedServiceResource srcLinkedService = createSourceLinkedService(srcName, srcDataAddress);
        BinaryDataset srcDataset = new BinaryDataset()
                .withLinkedServiceName(new LinkedServiceReference().withReferenceName(srcLinkedService.name()))
                .withLocation(new AzureBlobStorageLocation()
                        .withFileName(srcDataAddress.getProperty(AzureBlobStoreSchema.BLOB_NAME))
                        .withContainer(srcDataAddress.getProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                );
        DatasetResource srcDatasetResource = client.defineDataset(srcName).withProperties(srcDataset).create();

        BlobSource source = new BlobSource();

        LinkedServiceResource dstLinkedService = createDestinationLinkedService(dstName, dstDataAddress);
        BinaryDataset dstDataset = new BinaryDataset()
                .withLinkedServiceName(new LinkedServiceReference().withReferenceName(dstLinkedService.name()))
                .withLocation(new AzureBlobStorageLocation()
                        .withFileName(dstDataAddress.getProperty(AzureBlobStoreSchema.BLOB_NAME))
                        .withContainer(dstDataAddress.getProperty(AzureBlobStoreSchema.CONTAINER_NAME))
                );
        DatasetResource dstDatasetResource = client.defineDataset(dstName).withProperties(dstDataset).create();
        BlobSink sink = new BlobSink();

        return client.definePipeline(baseName)
                .withActivities(List.of(new CopyActivity()
                        .withName("CopyActivity")
                        .withInputs(List.of(new DatasetReference().withReferenceName(srcDatasetResource.name())))
                        .withOutputs(List.of(new DatasetReference().withReferenceName(dstDatasetResource.name())))
                        .withSource(source)
                        .withSink(sink)
                        .withValidateDataConsistency(false)
                ))
                .create();
    }

    private LinkedServiceResource createSourceLinkedService(String name, DataAddress dataAddress) {
        String type = dataAddress.getType();
        if (type.equals(AzureBlobStoreSchema.TYPE)) {
            var accountName = dataAddress.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);

            return client.defineLinkedService(name)
                    .withProperties(new AzureStorageLinkedService()
                            .withConnectionString(format("DefaultEndpointsProtocol=https;AccountName=%s;", accountName))
                            .withAccountKey(
                                    new AzureKeyVaultSecretReference()
                                            .withSecretName(dataAddress.getProperty(KEY_NAME))
                                            .withStore(new LinkedServiceReference()
                                                    .withReferenceName(keyVaultLinkedService)
                                            ))
                    )
                    .create();
        }
        throw new UnsupportedOperationException(format("Unsupported data source type: %s", type));
    }

    private LinkedServiceResource createDestinationLinkedService(String name, DataAddress dataAddress) {
        String type = dataAddress.getType();
        switch (type) {
            case AzureBlobStoreSchema.TYPE:
                var accountName = dataAddress.getProperty(AzureBlobStoreSchema.ACCOUNT_NAME);
                var secret = keyVaultClient.getSecret(dataAddress.getProperty(KEY_NAME));
                var token = typeManager.readValue(secret.getValue(), AzureSasToken.class);
                var sasTokenSecret = keyVaultClient.setSecret(name, token.getSas());

                return client.defineLinkedService(name)
                        .withProperties(
                                new AzureStorageLinkedService()
                                        .withSasUri(format(BLOB_STORE_ENDPOINT_TEMPLATE, accountName))
                                        .withSasToken(
                                                new AzureKeyVaultSecretReference()
                                                        .withSecretName(sasTokenSecret.getName())
                                                        .withStore(new LinkedServiceReference()
                                                                .withReferenceName(keyVaultLinkedService)
                                                        ))
                        )
                        .create();
            case MicrosoftDataverseSchema.TYPE:
                return  client.defineLinkedService(name)
                        .withProperties(
                                new DynamicsCrmLinkedService()
                                        .withServiceUri(dataAddress.getProperty(MicrosoftDataverseSchema.SERVICE_URI))
                                        .withDeploymentType("Online")
                                        .withAuthenticationType("AADServicePrincipal")
                                        .withServicePrincipalId(dataAddress.getProperty(MicrosoftDataverseSchema.SERVICE_PRINCIPAL_ID))
                                        .withServicePrincipalCredentialType("ServicePrincipalKey")
                                        .withServicePrincipalCredential(
                                                new AzureKeyVaultSecretReference()
                                                        .withSecretName(dataAddress.getProperty(KEY_NAME))
                                                        .withStore(new LinkedServiceReference()
                                                                .withReferenceName(keyVaultLinkedService)
                                                        )
                                        )
                        )
                        .create();
            default:
                throw new UnsupportedOperationException(format("Unsupported data destination type: %s", type));
        }
    }
}