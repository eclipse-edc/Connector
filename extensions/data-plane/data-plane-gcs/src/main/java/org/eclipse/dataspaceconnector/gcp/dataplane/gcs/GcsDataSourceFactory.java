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

package org.eclipse.dataspaceconnector.gcp.dataplane.gcs;

import com.google.cloud.storage.StorageOptions;
import org.eclipse.dataspaceconnector.dataplane.common.validation.ValidationRule;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.gcp.core.storage.GcsStoreSchema;
import org.eclipse.dataspaceconnector.gcp.dataplane.gcs.validation.GcsSourceDataAddressValidationRule;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

public class GcsDataSourceFactory implements DataSourceFactory {

    private final ValidationRule<DataAddress> validation = new GcsSourceDataAddressValidationRule();
    private final Monitor monitor;

    public GcsDataSourceFactory(Monitor monitor) {
        this.monitor = monitor;
    }


    @Override
    public boolean canHandle(DataFlowRequest request) {
        return GcsStoreSchema.TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var source = request.getSourceDataAddress();
        return validation.apply(source).map(it -> true);
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var validationResult = validate(request);
        if (validationResult.failed()) {
            throw new EdcException(String.join(", ", validationResult.getFailureMessages()));
        }
        var storageClient = StorageOptions.newBuilder()
                .build().getService();

        var source = request.getSourceDataAddress();

        return GcsDataSource.Builder.newInstance()
                .storageClient(storageClient)
                .bucketName(source.getProperty(GcsStoreSchema.BUCKET_NAME))
                .blobName(source.getProperty(GcsStoreSchema.BLOB_NAME))
                .monitor(monitor)
                .build();

    }

}