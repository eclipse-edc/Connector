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
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3BaseClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;

public class AwsClientProviderImpl implements AwsClientProvider {

    private final AwsCredentialsProvider credentialsProvider;
    private final AwsClientProviderConfiguration configuration;
    private final Executor executor;
    private final Map<String, S3Client> s3Clients = new ConcurrentHashMap<>();
    private final Map<String, S3AsyncClient> s3AsyncClients = new ConcurrentHashMap<>();
    private final Map<String, StsAsyncClient> stsAsyncClients = new ConcurrentHashMap<>();
    private final IamAsyncClient iamAsyncClient;

    public AwsClientProviderImpl(AwsClientProviderConfiguration configuration) {
        this.credentialsProvider = configuration.getCredentialsProvider();
        this.configuration = configuration;
        this.executor = Executors.newFixedThreadPool(configuration.getThreadPoolSize(), new ThreadFactoryBuilder().threadNamePrefix("aws-client").build());
        this.iamAsyncClient = createIamAsyncClient();
    }

    @Override
    public S3Client s3Client(String region, SecretToken token) {
        if (token instanceof AwsTemporarySecretToken) {
            var temporary = (AwsTemporarySecretToken) token;
            var credentials = AwsSessionCredentials.create(temporary.getAccessKeyId(), temporary.getSecretAccessKey(), temporary.getSessionToken());
            return createS3Client(credentials, region);
        } else if (token instanceof AwsSecretToken) {
            var secretToken = (AwsSecretToken) token;
            var credentials = AwsBasicCredentials.create(secretToken.getAccessKeyId(), secretToken.getSecretAccessKey());
            return createS3Client(credentials, region);
        } else {
            throw new EdcException(String.format("SecretToken %s is not supported", token.getClass()));
        }
    }

    @Override
    public S3Client s3Client(String region) {
        return s3Clients.computeIfAbsent(region, this::createS3Client);
    }

    @Override
    public S3AsyncClient s3AsyncClient(String region) {
        return s3AsyncClients.computeIfAbsent(region, this::createS3AsyncClient);
    }

    @Override
    public IamAsyncClient iamAsyncClient() {
        return iamAsyncClient;
    }

    @Override
    public StsAsyncClient stsAsyncClient(String region) {
        return stsAsyncClients.computeIfAbsent(region, this::createStsClient);
    }

    @Override
    public void shutdown() {
        iamAsyncClient.close();
        s3Clients.values().forEach(SdkAutoCloseable::close);
        s3AsyncClients.values().forEach(SdkAutoCloseable::close);
        stsAsyncClients.values().forEach(SdkAutoCloseable::close);
    }

    @Override
    public void configureEndpointOverride(String endpointOverride) {
        if(endpointOverride == null || endpointOverride.equals(""))
            return;
        configuration.setEndpointOverride(endpointOverride);
    }

    private S3Client createS3Client(AwsCredentials credentials, String region) {
        var credentialsProvider = StaticCredentialsProvider.create(credentials);
        var builder = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region));

        handleBaseEndpointOverride(builder);

        return builder.build();
    }

    private S3Client createS3Client(String region) {
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region));

        handleBaseEndpointOverride(builder);

        return builder.build();
    }

    private S3AsyncClient createS3AsyncClient(String region) {
        var builder = S3AsyncClient.builder()
                .asyncConfiguration(b -> b.advancedOption(FUTURE_COMPLETION_EXECUTOR, executor))
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region));

        handleBaseEndpointOverride(builder);

        return builder.build();
    }

    private StsAsyncClient createStsClient(String region) {
        var builder = StsAsyncClient.builder()
                .asyncConfiguration(b -> b.advancedOption(FUTURE_COMPLETION_EXECUTOR, executor))
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region));

        handleEndpointOverride(builder);

        return builder.build();
    }

    private IamAsyncClient createIamAsyncClient() {
        var builder = IamAsyncClient.builder()
                .asyncConfiguration(b -> b.advancedOption(FUTURE_COMPLETION_EXECUTOR, executor))
                .credentialsProvider(credentialsProvider)
                .region(Region.AWS_GLOBAL);

        handleEndpointOverride(builder);

        return builder.build();
    }

    private void handleBaseEndpointOverride(S3BaseClientBuilder<?, ?> builder) {
        var endpointOverride = configuration.getEndpointOverride();
        if (endpointOverride != null) {
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .endpointOverride(endpointOverride);
        }
    }

    private void handleEndpointOverride(SdkClientBuilder<?, ?> builder) {
        var endpointOverride = configuration.getEndpointOverride();
        if (endpointOverride != null) {
            builder.endpointOverride(endpointOverride);
        }
    }
}
