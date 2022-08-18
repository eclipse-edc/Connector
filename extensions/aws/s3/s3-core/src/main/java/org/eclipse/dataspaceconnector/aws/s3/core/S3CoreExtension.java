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

package org.eclipse.dataspaceconnector.aws.s3.core;

import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import java.net.URI;
import java.util.Optional;

import static java.lang.String.format;
import static org.eclipse.dataspaceconnector.aws.s3.core.AwsClientProviderConfiguration.DEFAULT_AWS_ASYNC_CLIENT_THREAD_POOL_SIZE;

public class S3CoreExtension implements ServiceExtension {

    @EdcSetting(value = "The key of the secret where the AWS Access Key Id is stored")
    private static final String AWS_ACCESS_KEY = "edc.aws.access.key";

    @EdcSetting(value = "The key of the secret where the AWS Secret Access Key is stored")
    private static final String AWS_SECRET_KEY = "edc.aws.secret.access.key";

    @EdcSetting(value = "If valued, the AWS clients will point to the specified endpoint")
    private static final String AWS_ENDPOINT_OVERRIDE = "edc.aws.endpoint.override";

    @EdcSetting(value = "The size of the thread pool used for the async clients")
    private static final String AWS_ASYNC_CLIENT_THREAD_POOL_SIZE = "edc.aws.client.async.thread-pool-size";

    @Inject
    private Vault vault;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return "S3";
    }

    @Provider
    public AwsClientProvider awsClientProvider(ServiceExtensionContext context) {
        var endpointOverride = Optional.of(AWS_ENDPOINT_OVERRIDE)
                .map(key -> context.getSetting(key, null))
                .map(URI::create)
                .orElse(null);

        var threadPoolSize = context.getSetting(AWS_ASYNC_CLIENT_THREAD_POOL_SIZE, DEFAULT_AWS_ASYNC_CLIENT_THREAD_POOL_SIZE);

        var configuration = AwsClientProviderConfiguration.Builder.newInstance()
                .credentialsProvider(createCredentialsProvider(context))
                .endpointOverride(endpointOverride)
                .threadPoolSize(threadPoolSize)
                .build();

        return new AwsClientProviderImpl(configuration);
    }

    @NotNull
    private AwsCredentialsProvider createCredentialsProvider(ServiceExtensionContext context) {
        var accessKey = vault.resolveSecret(AWS_ACCESS_KEY);
        var secretKey = vault.resolveSecret(AWS_SECRET_KEY);

        if (accessKey == null || secretKey == null) {
            monitor.info(format("S3: %s and %s were not found in the vault, DefaultCredentialsProvider will be used", AWS_ACCESS_KEY, AWS_SECRET_KEY));
            return DefaultCredentialsProvider.create();
        }

        // TODO: what is this for?
        if (vault.resolveSecret("aws-credentials") == null) {
            vault.storeSecret("aws-credentials", context.getTypeManager().writeValueAsString(new AwsSecretToken(accessKey, secretKey)));
        }

        return () -> AwsBasicCredentials.create(accessKey, secretKey);
    }

}
