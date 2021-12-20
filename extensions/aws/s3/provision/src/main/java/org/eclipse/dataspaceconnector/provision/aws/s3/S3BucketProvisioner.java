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

import net.jodah.failsafe.Failsafe;
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
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyResponse;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleResponse;
import software.amazon.awssdk.services.iam.model.GetUserResponse;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.Tag;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.Pair;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

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

        String bucketName = resource.getBucketName();

        monitor.info("S3 Deprovisioning: list bucket contents");
        String role = resource.getRole();
        return s3Client.listObjectsV2(listBucket(bucketName))
                .thenCompose(listObjectsResponse -> deleteObjects(s3Client, bucketName, listObjectsResponse))
                .thenCompose(deleteObjectsResponse -> deleteBucket(s3Client, bucketName))
                .thenCompose(listAttachedRolePoliciesResponse -> deleteRolePolicy(iamClient, role))
                .thenCompose(deleteRolePolicyResponse -> deleteRole(iamClient, role))
                .thenApply(response -> DeprovisionResponse.Builder.newInstance().ok().resource(resource).build());
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

    private CompletableFuture<DeleteRoleResponse> deleteRole(IamAsyncClient iamClient, String role) {
        return Failsafe.with(retryPolicy).getStageAsync(() -> {
            monitor.info("S3 Deprovisioning: delete role");
            return iamClient.deleteRole(DeleteRoleRequest.builder().roleName(role).build());
        });
    }

    private CompletableFuture<DeleteRolePolicyResponse> deleteRolePolicy(IamAsyncClient iamClient, String role) {
        return Failsafe.with(retryPolicy).getStageAsync(() -> {
            monitor.info("S3 Deprovisioning: deleting inline policies for Role " + role);
            return iamClient.deleteRolePolicy(DeleteRolePolicyRequest.builder().roleName(role).policyName(role).build());
        });
    }

    private CompletableFuture<DeleteBucketResponse> deleteBucket(S3AsyncClient s3Client, String bucketName) {
        return Failsafe.with(retryPolicy).getStageAsync(() -> {
            monitor.info("S3 Deprovisioning: delete bucket");
            return s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
        });
    }

    private CompletableFuture<DeleteObjectsResponse> deleteObjects(S3AsyncClient s3Client, String bucketName, ListObjectsV2Response listObjectsResponse) {
        var identifiers = listObjectsResponse.contents().stream()
                .map(s3object -> ObjectIdentifier.builder().key(s3object.key()).build())
                .collect(Collectors.toList());

        var deleteRequest = DeleteObjectsRequest.builder()
                .bucket(bucketName).delete(Delete.builder().objects(identifiers).build())
                .build();
        monitor.info("S3 Deprovisioning: delete bucket contents: " + identifiers.stream().map(ObjectIdentifier::key).collect(joining(", ")));
        return s3Client.deleteObjects(deleteRequest);
    }

    private ListObjectsV2Request listBucket(String bucketName) {
        return ListObjectsV2Request.builder().bucket(bucketName).build();
    }
}


