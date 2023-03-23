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

package org.eclipse.edc.connector.dataplane.aws.s3;

import org.eclipse.edc.aws.s3.AwsClientProvider;
import org.eclipse.edc.aws.s3.AwsSecretToken;
import org.eclipse.edc.aws.s3.AwsTemporarySecretToken;
import org.eclipse.edc.aws.s3.S3BucketSchema;
import org.eclipse.edc.connector.dataplane.aws.s3.validation.S3DataAddressCredentialsValidationRule;
import org.eclipse.edc.connector.dataplane.aws.s3.validation.S3DataAddressValidationRule;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSink;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSinkFactory;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.concurrent.ExecutorService;

import static org.eclipse.edc.aws.s3.S3BucketSchema.ACCESS_KEY_ID;
import static org.eclipse.edc.aws.s3.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.edc.aws.s3.S3BucketSchema.REGION;
import static org.eclipse.edc.aws.s3.S3BucketSchema.SECRET_ACCESS_KEY;

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
        return validateRequest(request).map(it -> true);
    }

    @Override
    public @NotNull Result<Void> validateRequest(DataFlowRequest request) {
        var destination = request.getDestinationDataAddress();

        return validation.apply(destination).map(it -> null);
    }

    @Override
    public DataSink createSink(DataFlowRequest request) {
        var validationResult = validateRequest(request);
        if (validationResult.failed()) {
            throw new EdcException(String.join(", ", validationResult.getFailureMessages()));
        }

        var destination = request.getDestinationDataAddress();

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

}
