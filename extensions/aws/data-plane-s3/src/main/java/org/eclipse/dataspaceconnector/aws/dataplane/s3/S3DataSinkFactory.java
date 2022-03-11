/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */
package org.eclipse.dataspaceconnector.aws.dataplane.s3;

import org.eclipse.dataspaceconnector.aws.dataplane.s3.validation.EmptyValueValidationRule;
import org.eclipse.dataspaceconnector.aws.dataplane.s3.validation.S3DataAddressValidationRule;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsSecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.s3.core.S3ClientProvider;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSink;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.AbstractResult;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.ACCESS_KEY_ID;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.REGION;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.SECRET_ACCESS_KEY;

public class S3DataSinkFactory implements DataSinkFactory {

    private final S3DataAddressValidationRule validation = new S3DataAddressValidationRule();
    private final S3ClientProvider s3ClientProvider;
    private final ExecutorService executorService;
    private final Monitor monitor;

    public S3DataSinkFactory(S3ClientProvider s3ClientProvider, ExecutorService executorService, Monitor monitor) {
        this.s3ClientProvider = s3ClientProvider;
        this.executorService = executorService;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return S3BucketSchema.TYPE.equals(request.getDestinationDataAddress().getType());
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

        var destination = request.getDestinationDataAddress();
        var secretToken = new AwsSecretToken(destination.getProperty(ACCESS_KEY_ID), destination.getProperty(SECRET_ACCESS_KEY));
        var client = s3ClientProvider.provide(destination.getProperty(REGION), secretToken);
        return S3DataSink.Builder.newInstance()
                .bucketName(destination.getProperty(BUCKET_NAME))
                .keyName(destination.getKeyName())
                .requestId(request.getId())
                .executorService(executorService)
                .monitor(monitor)
                .client(client)
                .build();
    }

}
