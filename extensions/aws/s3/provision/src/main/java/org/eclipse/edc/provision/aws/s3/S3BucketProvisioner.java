/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.provision.aws.s3;

import org.eclipse.edc.provision.aws.provider.ClientProvider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.transfer.provision.ProvisionContext;
import org.eclipse.edc.spi.transfer.provision.Provisioner;
import org.eclipse.edc.spi.transfer.response.ResponseStatus;
import org.eclipse.edc.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.edc.spi.types.domain.transfer.ResourceDefinition;
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
        final S3DeprovisionPipeline pipeline = S3DeprovisionPipeline.Builder.newInstance().clientProvider(clientProvider)
                .retryPolicy(retryPolicy)
                .monitor(monitor)
                .resource().build();
        pipeline.deprovision(provisionedResource, throwable -> context.deprovisioned(provisionedResource, throwable));
        return ResponseStatus.OK;
    }

}


