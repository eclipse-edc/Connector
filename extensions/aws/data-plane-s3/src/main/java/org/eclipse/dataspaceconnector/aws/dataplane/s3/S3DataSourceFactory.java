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
import org.eclipse.dataspaceconnector.aws.s3.core.AwsSecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.s3.core.S3ClientProvider;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSource;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.DataSourceFactory;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.ACCESS_KEY_ID;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.BUCKET_NAME;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.REGION;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.SECRET_ACCESS_KEY;

public class S3DataSourceFactory implements DataSourceFactory {

    private final ValidationRule<DataAddress> validation = new S3DataAddressValidationRule();
    private final ValidationRule<DataAddress> credentialsValidation = new S3DataAddressCredentialsValidationRule();
    private final S3ClientProvider s3ClientProvider;
    private final AwsCredentialsProvider credentialsProvider;

    public S3DataSourceFactory(S3ClientProvider s3ClientProvider, AwsCredentialsProvider credentialsProvider) {
        this.s3ClientProvider = s3ClientProvider;
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public boolean canHandle(DataFlowRequest request) {
        return S3BucketSchema.TYPE.equals(request.getSourceDataAddress().getType());
    }

    @Override
    public @NotNull Result<Boolean> validate(DataFlowRequest request) {
        var source = request.getSourceDataAddress();
        var validator = resolveCredentials() == null
                ? validation.and(credentialsValidation)
                : validation;

        return validator.apply(source).map(it -> true);
    }

    @Override
    public DataSource createSource(DataFlowRequest request) {
        var validationResult = validate(request);
        if (validationResult.failed()) {
            throw new EdcException(String.join(", ", validationResult.getFailureMessages()));
        }

        var source = request.getSourceDataAddress();
        var awsCredentials = resolveCredentials();

        var secretToken = new AwsSecretToken(source.getProperty(ACCESS_KEY_ID), source.getProperty(SECRET_ACCESS_KEY));

        var client = awsCredentials == null
                ? s3ClientProvider.provide(source.getProperty(REGION), secretToken)
                : s3ClientProvider.provide(source.getProperty(REGION), awsCredentials);

        return S3DataSource.Builder.newInstance()
                .bucketName(source.getProperty(BUCKET_NAME))
                .keyName(source.getKeyName())
                .client(client)
                .build();
    }

    private AwsCredentials resolveCredentials() {
        try {
            return credentialsProvider.resolveCredentials();
        } catch (Exception e) {
            return null;
        }
    }

}
