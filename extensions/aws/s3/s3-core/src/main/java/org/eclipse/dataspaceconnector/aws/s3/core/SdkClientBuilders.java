/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.aws.s3.core;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamAsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsAsyncClientBuilder;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;

/**
 * Creates base SDK clients.
 */
public class SdkClientBuilders {

    public static S3AsyncClient buildS3Client(Consumer<S3AsyncClientBuilder> consumer, ThreadPoolExecutor executor, AwsCredentialsProvider credentialsProvider) {
        S3AsyncClientBuilder builder = S3AsyncClient.builder();
        builder.asyncConfiguration(b -> b.advancedOption(FUTURE_COMPLETION_EXECUTOR, executor));
        builder.credentialsProvider(credentialsProvider);
        consumer.accept(builder);
        return builder.build();
    }

    public static IamAsyncClient buildIamClient(Consumer<IamAsyncClientBuilder> consumer, ThreadPoolExecutor executor, AwsCredentialsProvider credentialsProvider) {
        IamAsyncClientBuilder builder = IamAsyncClient.builder();
        builder.asyncConfiguration(b -> b.advancedOption(FUTURE_COMPLETION_EXECUTOR, executor));
        builder.credentialsProvider(credentialsProvider);
        consumer.accept(builder);
        return builder.build();
    }

    public static StsAsyncClient buildStsClient(Consumer<StsAsyncClientBuilder> consumer, ThreadPoolExecutor executor, AwsCredentialsProvider credentialsProvider) {
        StsAsyncClientBuilder builder = StsAsyncClient.builder();
        builder.asyncConfiguration(b -> b.advancedOption(FUTURE_COMPLETION_EXECUTOR, executor));
        builder.credentialsProvider(credentialsProvider);
        consumer.accept(builder);
        return builder.build();
    }

}
