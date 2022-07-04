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

package org.eclipse.dataspaceconnector.aws.s3.provision;

import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.ClientProvider;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.util.concurrent.CompletableFuture;

/**
 * Asynchronously provisions S3 buckets.
 */
public class S3BucketProvisioner implements Provisioner<S3BucketResourceDefinition, S3BucketProvisionedResource> {

    private final ClientProvider clientProvider;
    private final Monitor monitor;
    private final RetryPolicy<Object> retryPolicy;
    private final S3BucketProvisionerConfiguration configuration;

    public S3BucketProvisioner(ClientProvider clientProvider, Monitor monitor, RetryPolicy<Object> retryPolicy, S3BucketProvisionerConfiguration configuration) {
        this.clientProvider = clientProvider;
        this.monitor = monitor;
        this.configuration = configuration;
        this.retryPolicy = RetryPolicy.builder(retryPolicy.getConfig())
                .withMaxRetries(configuration.getMaxRetries())
                .handle(AwsServiceException.class)
                .build();
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
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(S3BucketResourceDefinition resourceDefinition, Policy policy) {
        return S3ProvisionPipeline.Builder.newInstance(retryPolicy)
                .clientProvider(clientProvider)
                .roleMaxSessionDuration(configuration.getRoleMaxSessionDuration())
                .monitor(monitor)
                .build()
                .provision(resourceDefinition)
                .thenApply(result -> provisionSuccedeed(resourceDefinition, result.getRole(), result.getCredentials()));
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(S3BucketProvisionedResource resource, Policy policy) {
        return S3DeprovisionPipeline.Builder.newInstance(retryPolicy)
                .clientProvider(clientProvider)
                .monitor(monitor)
                .build()
                .deprovision(resource)
                .thenApply(ignore -> StatusResult.success(DeprovisionedResource.Builder.newInstance().provisionedResourceId(resource.getId()).build()));
    }

    private StatusResult<ProvisionResponse> provisionSuccedeed(S3BucketResourceDefinition resourceDefinition, Role role, Credentials credentials) {
        var resource = S3BucketProvisionedResource.Builder.newInstance()
                .id(resourceDefinition.getBucketName())
                .resourceDefinitionId(resourceDefinition.getId())
                .hasToken(true)
                .region(resourceDefinition.getRegionId())
                .bucketName(resourceDefinition.getBucketName())
                .role(role.roleName())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .resourceName(resourceDefinition.getBucketName())
                .build();

        var secretToken = new AwsTemporarySecretToken(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken(), credentials.expiration().toEpochMilli());

        monitor.debug("S3BucketProvisioner: Bucket request submitted: " + resourceDefinition.getBucketName());
        var response = ProvisionResponse.Builder.newInstance().resource(resource).secretToken(secretToken).build();
        return StatusResult.success(response);
    }

}


