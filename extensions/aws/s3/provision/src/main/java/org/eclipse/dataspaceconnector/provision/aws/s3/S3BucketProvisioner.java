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

package org.eclipse.dataspaceconnector.provision.aws.s3;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.provision.aws.provider.ClientProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionContext;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.util.concurrent.CompletableFuture;

/**
 * Asynchronously provisions S3 buckets.
 */
public class S3BucketProvisioner implements Provisioner<S3BucketResourceDefinition, S3BucketProvisionedResource> {
    private final ClientProvider clientProvider;
    private final int sessionDuration;
    private final Monitor monitor;
    private final RetryPolicy<Object> retryPolicy;
    private ProvisionContext context;

    /**
     * Ctor.
     *
     * @param clientProvider  the provider for SDK clients
     * @param sessionDuration role duration in seconds
     * @param monitor         the monitor
     * @param retryPolicy     the retry policy
     */
    public S3BucketProvisioner(ClientProvider clientProvider, int sessionDuration, Monitor monitor, RetryPolicy<Object> retryPolicy) {
        this.clientProvider = clientProvider;
        this.sessionDuration = sessionDuration;
        this.monitor = monitor;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public void initialize(ProvisionContext context) {
        this.context = context;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof S3BucketResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof S3BucketProvisionedResource;
    }

    @Override
    public CompletableFuture<ProvisionResponse> provision(S3BucketResourceDefinition resourceDefinition) {
        S3ProvisionPipeline pipeline = S3ProvisionPipeline.Builder.newInstance(retryPolicy)
                .resourceDefinition(resourceDefinition)
                .clientProvider(clientProvider)
                .sessionDuration(sessionDuration)
                .monitor(monitor)
                .build();

        return pipeline.provision();
    }

    @Override
    public CompletableFuture<ResponseStatus> deprovision(S3BucketProvisionedResource provisionedResource) {
        S3DeprovisionPipeline pipeline = S3DeprovisionPipeline.Builder.newInstance().clientProvider(clientProvider)
                .retryPolicy(retryPolicy)
                .monitor(monitor)
                .resource().build();

        return pipeline.deprovision(provisionedResource, throwable -> context.deprovisioned(provisionedResource, throwable))
                .thenApply(empty -> ResponseStatus.OK);
    }
}


