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

import org.eclipse.dataspaceconnector.aws.dataplane.s3.validation.S3DataAddressCredentialsValidationRule;
import org.eclipse.dataspaceconnector.aws.dataplane.s3.validation.S3DataAddressValidationRule;
import org.eclipse.dataspaceconnector.aws.dataplane.s3.validation.ValidationRule;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsClientProvider;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsSecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
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
import software.amazon.awssdk.services.s3.S3Client;

import java.util.concurrent.ExecutorService;

import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.ACCESS_KEY_ID;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.ENDPOINT_OVERRIDE;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.REGION;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.SECRET_ACCESS_KEY;

public class S3DataSinkFactory implements DataSinkFactory {

    private static final int CHUNK_SIZE_IN_BYTES = 1024 * 1024 * 500; // 500MB chunk size

    private final ValidationRule<DataAddress> validation = new S3DataAddressValidationRule();
    private final ValidationRule<DataAddress> credentialsValidation = new S3DataAddressCredentialsValidationRule();
    private final AwsClientProvider clientProvider;
    private final ExecutorService executorService;
    private final Monitor monitor;
    private Vault vault;
    private TypeManager typeManager;

    public S3DataSinkFactory(AwsClientProvider clientProvider, ExecutorService executorService, Monitor monitor, Vault vault, TypeManager typeManager) {
        this.clientProvider = clientProvider;
        this.executorService = executorService;
        this.monitor = monitor;
        this.vault = vault;
        this.typeManager = typeManager;
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

        S3Client client = createS3Client(destination);
        return S3DataSink.Builder.newInstance()
            .bucketName(destination.getProperty(BUCKET_NAME))
            .keyName(destination.getKeyName())
            .requestId(request.getId())
            .executorService(executorService)
            .monitor(monitor)
            .client(client)
            .chunkSizeBytes(CHUNK_SIZE_IN_BYTES)
            .build();
    }

    private S3Client createS3Client(DataAddress destination) {

        clientProvider.configureEndpointOverride(destination.getProperty(ENDPOINT_OVERRIDE));

        S3Client client;
        var secret = vault.resolveSecret(destination.getKeyName());
        if (secret != null) {
            var secretToken = typeManager.readValue(secret, AwsTemporarySecretToken.class);
            client = clientProvider.s3Client(destination.getProperty(REGION), secretToken);
        } else if (credentialsValidation.apply(destination).succeeded()) {
            var secretToken = new AwsSecretToken(destination.getProperty(ACCESS_KEY_ID), destination.getProperty(SECRET_ACCESS_KEY));
            client = clientProvider.s3Client(destination.getProperty(REGION), secretToken);
        } else {
            client = clientProvider.s3Client(destination.getProperty(REGION));
        }
        return client;
    }

}
