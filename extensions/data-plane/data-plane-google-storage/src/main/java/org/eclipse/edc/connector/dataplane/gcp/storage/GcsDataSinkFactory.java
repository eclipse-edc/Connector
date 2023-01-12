/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.connector.dataplane.gcp.storage;


import com.google.cloud.storage.StorageOptions;
import org.eclipse.edc.connector.dataplane.gcp.storage.validation.GcsSinkDataAddressValidationRule;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.gcp.common.GcpCredentials;
import org.eclipse.edc.gcp.storage.GcsStoreSchema;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;

public class GcsDataSinkFactory implements DataSinkFactory {
    private final ValidationRule<DataAddress> validation = new GcsSinkDataAddressValidationRule();
    private final ExecutorService executorService;
    private final Monitor monitor;
    private final GcpCredentials gcpCredential;

    public GcsDataSinkFactory(ExecutorService executorService, Monitor monitor, GcpCredentials gcpCredential) {
        this.executorService = executorService;
        this.monitor = monitor;
        this.gcpCredential = gcpCredential;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return GcsStoreSchema.TYPE.equals(request.getDestinationDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var destination = request.getDestinationDataAddress();
        return validation.apply(destination).map(it -> true);
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        var validationResult = validate(request);
        if (validationResult.failed()) {
            throw new EdcException(String.join(", ", validationResult.getFailureMessages()));
        }
        var destinationDataAddress = request.getDestinationDataAddress();
        var googleCredentials = gcpCredential.resolveGoogleCredentialsFromDataAddress(destinationDataAddress.getKeyName(),
                destinationDataAddress.getProperty(GcsStoreSchema.SERVICE_ACCOUNT_KEY_NAME),
                destinationDataAddress.getProperty(GcsStoreSchema.SERVICE_ACCOUNT_KEY_VALUE));

        var destination = request.getDestinationDataAddress();

        var storageClient = StorageOptions.newBuilder()
                .setCredentials(googleCredentials)
                .build().getService();

        return GcsDataSink.Builder.newInstance()
                .storageClient(storageClient)
                .bucketName(destination.getProperty(GcsStoreSchema.BUCKET_NAME))
                .blobName(destination.getProperty(GcsStoreSchema.BLOB_NAME))
                .requestId(request.getId())
                .executorService(executorService)
                .monitor(monitor)
                .build();
    }
}