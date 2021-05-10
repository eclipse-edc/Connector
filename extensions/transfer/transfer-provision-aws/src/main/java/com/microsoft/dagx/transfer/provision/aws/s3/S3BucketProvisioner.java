/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.aws.s3;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.transfer.provision.Provisioner;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.transfer.provision.aws.provider.ClientProvider;
import net.jodah.failsafe.RetryPolicy;

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
    public ResponseStatus provision(S3BucketResourceDefinition resourceDefinition) {
        S3ProvisionPipeline.Builder builder = S3ProvisionPipeline.Builder.newInstance(retryPolicy);
        S3ProvisionPipeline pipeline = builder.resourceDefinition(resourceDefinition).clientProvider(clientProvider).sessionDuration(sessionDuration).context(context).monitor(monitor).build();

        pipeline.provision();

        monitor.debug("Bucket request submitted: " + resourceDefinition.getBucketName());
        return ResponseStatus.OK;
    }

    @Override
    public ResponseStatus deprovision(S3BucketProvisionedResource provisionedResource) {
        return ResponseStatus.OK;
    }

}


