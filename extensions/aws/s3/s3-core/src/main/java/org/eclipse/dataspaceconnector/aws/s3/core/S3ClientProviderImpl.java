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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.s3.core;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3ClientProviderImpl implements S3ClientProvider {

    @Override
    public S3Client provide(String region, SecretToken token) {
        if (token instanceof AwsTemporarySecretToken) {
            var temporary = (AwsTemporarySecretToken) token;
            var credentials = AwsSessionCredentials.create(temporary.getAccessKeyId(), temporary.getSecretAccessKey(), temporary.getSessionToken());
            return clientWithCredentials(credentials, region);
        } else if (token instanceof AwsSecretToken) {
            var secretToken = (AwsSecretToken) token;
            var credentials = AwsBasicCredentials.create(secretToken.getAccessKeyId(), secretToken.getSecretAccessKey());
            return clientWithCredentials(credentials, region);

        } else {
            throw new EdcException(String.format("SecretToken %s is not supported", token.getClass()));
        }
    }

    @Override
    public S3Client provide(String region, AwsCredentials credentials) {
        return clientWithCredentials(credentials, region);
    }

    private S3Client clientWithCredentials(AwsCredentials credentials, String region) {
        var credentialsProvider = StaticCredentialsProvider.create(credentials);
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                .build();
    }
}
