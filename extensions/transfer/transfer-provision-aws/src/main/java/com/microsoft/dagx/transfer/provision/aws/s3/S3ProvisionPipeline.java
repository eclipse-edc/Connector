/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.provision.aws.s3;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import com.microsoft.dagx.transfer.provision.aws.provider.ClientProvider;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;

import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

/**
 * Asynchronously provisions an S3 bucket to serve as a data destination and a temporary token with write permissions to the bucket.
 */
class S3ProvisionPipeline {
    // Do not modify this trust policy
    private static final String ASSUME_ROLE_TRUST = "{" +
            "  \"Version\": \"2012-10-17\"," +
            "  \"Statement\": [" +
            "    {" +
            "      \"Effect\": \"Allow\"," +
            "      \"Principal\": {" +
            "        \"AWS\": \"%s\"" +
            "      }," +
            "      \"Action\": \"sts:AssumeRole\"" +
            "    }" +
            "  ]" +
            "}";
    // Do not modify this bucket policy
    private static final String BUCKET_POLICY = "{" +
            "    \"Version\": \"2012-10-17\"," +
            "    \"Statement\": [" +
            "        {" +
            "            \"Sid\": \"TemporaryAccess\", " +
            "            \"Effect\": \"Allow\"," +
            "            \"Action\": \"s3:PutObject\"," +
            "            \"Resource\": \"arn:aws:s3:::%s/*\"" +
            "        }" +
            "    ]" +
            "}";
    private final RetryPolicy<Object> retryPolicy;
    private ClientProvider clientProvider;
    private S3BucketResourceDefinition resourceDefinition;
    private ProvisionContext context;
    private int sessionDuration;
    private Monitor monitor;

    private S3ProvisionPipeline(RetryPolicy<Object> generalPolicy) {
        retryPolicy = generalPolicy.copy()
                .withMaxRetries(10)
                .handle(AwsServiceException.class);
    }

    /**
     * Performs a non-blocking provisioning operation.
     */
    public void provision() {
        AsyncContext asyncContext = new AsyncContext();

        String region = resourceDefinition.getRegionId();

        String bucketName = resourceDefinition.getBucketName();

        S3AsyncClient s3AsyncClient = clientProvider.clientFor(S3AsyncClient.class, region);

        CreateBucketRequest request = CreateBucketRequest.builder().bucket(bucketName).createBucketConfiguration(CreateBucketConfiguration.builder().build()).build();
        s3AsyncClient.createBucket(request)
                .thenCompose(r -> Failsafe.with(retryPolicy).getStageAsync(() -> getUser(resourceDefinition)))
                .thenCompose(r -> Failsafe.with(retryPolicy).getStageAsync(() -> createRole(resourceDefinition, r)))
                .thenCompose(r -> Failsafe.with(retryPolicy).getStageAsync(() -> createRolePolicy(resourceDefinition, bucketName, asyncContext, r)))
                .thenCompose(r -> Failsafe.with(retryPolicy).getStageAsync(() -> assumeRole(resourceDefinition, asyncContext)))
                .whenComplete((r, e) -> createAndSendToken(resourceDefinition, r, e));
    }

    private CompletableFuture<GetUserResponse> getUser(S3BucketResourceDefinition resourceDefinition) {
        monitor.debug("S3ProvisionPipeline: get user");
        return clientProvider.clientFor(IamAsyncClient.class, resourceDefinition.getRegionId()).getUser();
    }

    private CompletableFuture<CreateRoleResponse> createRole(S3BucketResourceDefinition resourceDefinition, GetUserResponse response) {
        String userArn = response.user().arn();
        CreateRoleRequest.Builder roleBuilder = CreateRoleRequest.builder();
        Tag tag = Tag.builder().key("dagx:process").value(resourceDefinition.getTransferProcessId()).build();
        roleBuilder.roleName(resourceDefinition.getTransferProcessId()).description("DA-GX transfer process role")
                .assumeRolePolicyDocument(format(ASSUME_ROLE_TRUST, userArn)).maxSessionDuration(sessionDuration).tags(tag);
        monitor.debug("S3ProvisionPipeline: create role for user" + userArn);
        return clientProvider.clientFor(IamAsyncClient.class, resourceDefinition.getRegionId()).createRole(roleBuilder.build());
    }

    private CompletableFuture<PutRolePolicyResponse> createRolePolicy(S3BucketResourceDefinition resourceDefinition, String bucketName, AsyncContext asyncContext, CreateRoleResponse response) {
        String processId = resourceDefinition.getTransferProcessId();
        asyncContext.roleArn = response.role().arn();
        String policyDocument = format(BUCKET_POLICY, bucketName);
        PutRolePolicyRequest policyRequest = PutRolePolicyRequest.builder().policyName(processId).roleName(response.role().roleName()).policyDocument(policyDocument).build();
        monitor.debug("S3ProvisionPipeline: attach bucket policy to role " + asyncContext.roleArn);
        return clientProvider.clientFor(IamAsyncClient.class, resourceDefinition.getRegionId()).putRolePolicy(policyRequest);
    }

    private CompletableFuture<AssumeRoleResponse> assumeRole(S3BucketResourceDefinition resourceDefinition, AsyncContext asyncContext) {

        AssumeRoleRequest.Builder roleBuilder = AssumeRoleRequest.builder();
        roleBuilder.roleArn(asyncContext.roleArn).roleSessionName("transfer").externalId("123");
        monitor.debug("S3ProvisionPipeline: attempting to assume the role");
        return clientProvider.clientFor(StsAsyncClient.class, resourceDefinition.getRegionId()).assumeRole(roleBuilder.build());
    }


    private void createAndSendToken(S3BucketResourceDefinition resourceDefinition, AssumeRoleResponse response, Throwable exception) {
        String bucketName = resourceDefinition.getBucketName();
        if (response != null) {
            var credentials = response.credentials();

            var transferProcessId = resourceDefinition.getTransferProcessId();

            monitor.debug("S3ProvisionPipeline: STS credentials obtained, continuing...");
            var resource = S3BucketProvisionedResource.Builder.newInstance().id(bucketName)
                    .resourceDefinitionId(resourceDefinition.getId())
                    .region(resourceDefinition.getRegionId())
                    .bucketName(resourceDefinition.getBucketName())
                    .transferProcessId(transferProcessId).build();

            var secretToken = new DestinationSecretToken(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken(), credentials.expiration().toEpochMilli());

            context.callback(resource, secretToken);
        } else if (exception != null) {
            sendErroredResource(resourceDefinition, bucketName, exception);
        }
    }

    private void sendErroredResource(S3BucketResourceDefinition resourceDefinition, String bucketName, Throwable exception) {
        var exceptionToLog = exception.getCause() != null ? exception.getCause() : exception;
        String resourceId = resourceDefinition.getId();
        String errorMessage = exceptionToLog.getMessage();
        S3BucketProvisionedResource erroredResource = S3BucketProvisionedResource.Builder.newInstance().id(bucketName).transferProcessId(resourceDefinition.getTransferProcessId()).resourceDefinitionId(resourceId).error(true).errorMessage(errorMessage).build();
        context.callback(erroredResource, null);
    }

    private static class AsyncContext {
        String roleArn;
    }

    static class Builder {
        private final S3ProvisionPipeline pipeline;

        private Builder(RetryPolicy<Object> policy) {
            pipeline = new S3ProvisionPipeline(policy);
        }

        public static Builder newInstance(RetryPolicy<Object> policy) {
            return new Builder(policy);
        }

        public Builder clientProvider(ClientProvider clientProvider) {
            pipeline.clientProvider = clientProvider;
            return this;
        }

        public Builder resourceDefinition(S3BucketResourceDefinition resourceDefinition) {
            pipeline.resourceDefinition = resourceDefinition;
            return this;
        }

        public Builder sessionDuration(int sessionDuration) {
            pipeline.sessionDuration = sessionDuration;
            return this;
        }

        public Builder context(ProvisionContext context) {
            pipeline.context = context;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            pipeline.monitor = monitor;
            return this;
        }

        public S3ProvisionPipeline build() {
            return pipeline;
        }
    }


}
