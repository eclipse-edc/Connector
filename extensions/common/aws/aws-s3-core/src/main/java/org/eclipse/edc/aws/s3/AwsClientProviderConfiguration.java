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

package org.eclipse.edc.aws.s3;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.net.URI;
import java.util.Objects;

public class AwsClientProviderConfiguration {

    static final int DEFAULT_AWS_ASYNC_CLIENT_THREAD_POOL_SIZE = 50;

    private AwsCredentialsProvider credentialsProvider;
    private URI endpointOverride;
    private int threadPoolSize = DEFAULT_AWS_ASYNC_CLIENT_THREAD_POOL_SIZE;
    private boolean pathStyleAccessEnabled = false;

    private AwsClientProviderConfiguration() {

    }

    public AwsCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    public URI getEndpointOverride() {
        return endpointOverride;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public boolean getPathStyleAccessEnabled() {
        return pathStyleAccessEnabled;
    }

    public static class Builder {

        private final AwsClientProviderConfiguration configuration = new AwsClientProviderConfiguration();

        private Builder() {

        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder credentialsProvider(AwsCredentialsProvider credentialsProvider) {
            configuration.credentialsProvider = credentialsProvider;
            return this;
        }

        public Builder endpointOverride(URI endpointOverride) {
            configuration.endpointOverride = endpointOverride;
            return this;
        }

        public Builder threadPoolSize(int threadPoolSize) {
            configuration.threadPoolSize = threadPoolSize;
            return this;
        }

        public Builder pathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
            configuration.pathStyleAccessEnabled = pathStyleAccessEnabled;
            return this;
        }

        public AwsClientProviderConfiguration build() {
            Objects.requireNonNull(configuration.credentialsProvider, "AWS Credentials Provider is mandatory");

            return configuration;
        }
    }
}
