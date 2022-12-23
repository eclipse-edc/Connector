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
import org.eclipse.edc.aws.s3.S3BucketSchema;
import org.eclipse.edc.connector.dataplane.aws.s3.validation.S3DataAddressCredentialsValidationRule;
import org.eclipse.edc.connector.dataplane.aws.s3.validation.S3DataAddressValidationRule;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.edc.connector.dataplane.util.validation.ValidationRule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.S3Client;

import static org.eclipse.edc.aws.s3.S3BucketSchema.ACCESS_KEY_ID;
import static org.eclipse.edc.aws.s3.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.edc.aws.s3.S3BucketSchema.REGION;
import static org.eclipse.edc.aws.s3.S3BucketSchema.SECRET_ACCESS_KEY;

public class S3DataSourceFactory implements DataSourceFactory {

    private final ValidationRule<DataAddress> validation = new S3DataAddressValidationRule();
    private final ValidationRule<DataAddress> credentialsValidation = new S3DataAddressCredentialsValidationRule();
    private final AwsClientProvider clientProvider;
    private final Vault vault;

    private final TypeManager typeManager;

    public S3DataSourceFactory(AwsClientProvider clientProvider, Vault vault, TypeManager typeManager) {
        this.clientProvider = clientProvider;
        this.vault = vault;
        this.typeManager = typeManager;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return S3BucketSchema.TYPE.equals(request.getSourceDataAddress().getType());
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

        var source = request.getSourceDataAddress();

        S3Client client;
        var secret = vault.resolveSecret(source.getKeyName());
        if (secret != null) {
            var secretToken = typeManager.readValue(secret, AwsSecretToken.class);
            client = clientProvider.s3Client(source.getProperty(REGION), secretToken);
        } else if (credentialsValidation.apply(source).succeeded()) {
            var secretToken = new AwsSecretToken(source.getProperty(ACCESS_KEY_ID), source.getProperty(SECRET_ACCESS_KEY));
            client = clientProvider.s3Client(source.getProperty(REGION), secretToken);
        } else {
            client = clientProvider.s3Client(source.getProperty(REGION));
        }

        return S3DataSource.Builder.newInstance()
                .bucketName(source.getProperty(BUCKET_NAME))
                .keyName(source.getKeyName())
                .client(client)
                .build();
    }

}
