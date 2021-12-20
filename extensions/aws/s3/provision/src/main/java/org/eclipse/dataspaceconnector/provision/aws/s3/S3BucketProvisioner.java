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
import org.eclipse.dataspaceconnector.provision.aws.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.provision.aws.provider.ClientProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.util.concurrent.CompletableFuture;

/**
 * Asynchronously provisions S3 buckets.
 */
public class S3BucketProvisioner implements Provisioner<S3BucketResourceDefinition, S3BucketProvisionedResource> {

    private final ClientProvider clientProvider;
    private final int sessionDuration;
    private final Monitor monitor;
    private final RetryPolicy<Object> retryPolicy;

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
        this.retryPolicy = retryPolicy.copy()
                .withMaxRetries(10)
                .handle(AwsServiceException.class);;
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
        var s3AsyncClient = clientProvider.clientFor(S3AsyncClient.class, resourceDefinition.getRegionId());
        var iamClient = clientProvider.clientFor(IamAsyncClient.class, resourceDefinition.getRegionId());
        var stsClient = clientProvider.clientFor(StsAsyncClient.class, resourceDefinition.getRegionId());

        return S3ProvisionPipeline.Builder.newInstance(retryPolicy)
                .s3Client(s3AsyncClient)
                .iamClient(iamClient)
                .stsClient(stsClient)
                .sessionDuration(sessionDuration)
                .monitor(monitor)
                .build()
                .provision(resourceDefinition)
                .thenApply(result -> provisionSuccedeed(resourceDefinition, result.left(), result.right()))
                .exceptionally(throwable -> provisionFailed(resourceDefinition, throwable));
    }

    @Override
    public CompletableFuture<DeprovisionResponse> deprovision(S3BucketProvisionedResource resource) {
        var s3Client = clientProvider.clientFor(S3AsyncClient.class, resource.getRegion());
        var iamClient = clientProvider.clientFor(IamAsyncClient.class, resource.getRegion());

        monitor.info("S3 Deprovisioning: list bucket contents");

        return S3DeprovisionPipeline.Builder.newInstance(retryPolicy)
                .s3Client(s3Client)
                .iamClient(iamClient)
                .monitor(monitor)
                .build()
                .deprovision(resource)
                .thenApply(ignore -> DeprovisionResponse.Builder.newInstance().ok().resource(resource).build());
    }

    private ProvisionResponse provisionSuccedeed(S3BucketResourceDefinition resourceDefinition, Role role, Credentials credentials) {
        monitor.debug("S3ProvisionPipeline: STS credentials obtained, continuing...");
        var resource = S3BucketProvisionedResource.Builder.newInstance()
                .id(resourceDefinition.getBucketName())
                .resourceDefinitionId(resourceDefinition.getId())
                .region(resourceDefinition.getRegionId())
                .bucketName(resourceDefinition.getBucketName())
                .role(role.roleName())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .build();

        var secretToken = new AwsTemporarySecretToken(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken(), credentials.expiration().toEpochMilli());

        monitor.debug("Bucket request submitted: " + resourceDefinition.getBucketName());
        return ProvisionResponse.Builder.newInstance().resource(resource).secretToken(secretToken).build();
    }

    private ProvisionResponse provisionFailed(S3BucketResourceDefinition resourceDefinition, Throwable exception) {
        var exceptionToLog = exception.getCause() != null ? exception.getCause() : exception;
        S3BucketProvisionedResource erroredResource = S3BucketProvisionedResource.Builder.newInstance()
                .id(resourceDefinition.getBucketName())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .resourceDefinitionId(resourceDefinition.getId())
                .error(true)
                .errorMessage(exceptionToLog.getMessage())
                .build();

        return ProvisionResponse.Builder.newInstance().resource(erroredResource).build();
    }

}


