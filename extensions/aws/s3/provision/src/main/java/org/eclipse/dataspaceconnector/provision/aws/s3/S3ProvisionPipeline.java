package org.eclipse.dataspaceconnector.provision.aws.s3;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.GetUserResponse;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.Tag;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.Pair;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;

public class S3ProvisionPipeline {

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
    private final S3AsyncClient s3AsyncClient;
    private final IamAsyncClient iamClient;
    private final StsAsyncClient stsClient;
    private final Monitor monitor;
    private final int sessionDuration;

    private S3ProvisionPipeline(RetryPolicy<Object> retryPolicy, S3AsyncClient s3AsyncClient, IamAsyncClient iamClient,
                                StsAsyncClient stsClient, Monitor monitor, int sessionDuration) {
        this.retryPolicy = retryPolicy;
        this.s3AsyncClient = s3AsyncClient;
        this.iamClient = iamClient;
        this.stsClient = stsClient;
        this.monitor = monitor;
        this.sessionDuration = sessionDuration;
    }

    /**
     * Performs a non-blocking provisioning operation.
     */
    public CompletableFuture<Pair<Role, Credentials>> provision(S3BucketResourceDefinition resourceDefinition) {
        var request = CreateBucketRequest.builder()
                .bucket(resourceDefinition.getBucketName())
                .createBucketConfiguration(CreateBucketConfiguration.builder().build())
                .build();

        return s3AsyncClient.createBucket(request)
                .thenCompose(r -> getUser(iamClient))
                .thenCompose(response -> createRole(iamClient, resourceDefinition, response))
                .thenCompose(response -> createRolePolicy(iamClient, resourceDefinition, response))
                .thenCompose(role -> assumeRole(stsClient, role));
    }

    private CompletableFuture<Role> createRolePolicy(IamAsyncClient iamAsyncClient, S3BucketResourceDefinition resourceDefinition, CreateRoleResponse response) {
        return Failsafe.with(retryPolicy).getStageAsync(() -> {
            Role role = response.role();
            PutRolePolicyRequest policyRequest = PutRolePolicyRequest.builder()
                    .policyName(resourceDefinition.getTransferProcessId())
                    .roleName(role.roleName())
                    .policyDocument(format(BUCKET_POLICY, resourceDefinition.getBucketName()))
                    .build();

            monitor.debug("S3ProvisionPipeline: attach bucket policy to role " + role.arn());
            return iamAsyncClient.putRolePolicy(policyRequest)
                    .thenApply(policyResponse -> role);
        });
    }

    private CompletableFuture<CreateRoleResponse> createRole(IamAsyncClient iamClient, S3BucketResourceDefinition resourceDefinition, GetUserResponse response) {
        return Failsafe.with(retryPolicy).getStageAsync(() -> {
            String userArn = response.user().arn();
            Tag tag = Tag.builder().key("dataspaceconnector:process").value(resourceDefinition.getTransferProcessId()).build();

            monitor.debug("S3ProvisionPipeline: create role for user" + userArn);
            CreateRoleRequest createRoleRequest = CreateRoleRequest.builder()
                    .roleName(resourceDefinition.getTransferProcessId()).description("EDC transfer process role")
                    .assumeRolePolicyDocument(format(ASSUME_ROLE_TRUST, userArn))
                    .maxSessionDuration(sessionDuration)
                    .tags(tag)
                    .build();

            return iamClient.createRole(createRoleRequest);
        });
    }

    private CompletableFuture<GetUserResponse> getUser(IamAsyncClient iamAsyncClient) {
        return Failsafe.with(retryPolicy).getStageAsync(() -> {
            monitor.debug("S3ProvisionPipeline: get user");
            return iamAsyncClient.getUser();
        });
    }

    private CompletableFuture<Pair<Role, Credentials>> assumeRole(StsAsyncClient stsClient, Role role) {
        return Failsafe.with(retryPolicy).getStageAsync(() -> {
            monitor.debug("S3ProvisionPipeline: attempting to assume the role");
            AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                    .roleArn(role.arn())
                    .roleSessionName("transfer")
                    .externalId("123")
                    .build();

            return stsClient.assumeRole(roleRequest)
                    .thenApply(response -> Pair.of(role, response.credentials()));
        });
    }

    static class Builder {
        private final RetryPolicy<Object> retryPolicy;
        private S3AsyncClient s3AsyncClient;
        private IamAsyncClient iamClient;
        private StsAsyncClient stsClient;
        private int sessionDuration;
        private Monitor monitor;

        private Builder(RetryPolicy<Object> retryPolicy) {
            this.retryPolicy = retryPolicy;
        }

        public static Builder newInstance(RetryPolicy<Object> policy) {
            return new Builder(policy);
        }

        public Builder s3Client(S3AsyncClient s3AsyncClient) {
            this.s3AsyncClient = s3AsyncClient;
            return this;
        }

        public Builder iamClient(IamAsyncClient iamClient) {
            this.iamClient = iamClient;
            return this;
        }

        public Builder stsClient(StsAsyncClient stsClient) {
            this.stsClient = stsClient;
            return this;
        }

        public Builder sessionDuration(int sessionDuration) {
            this.sessionDuration = sessionDuration;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public S3ProvisionPipeline build() {
            Objects.requireNonNull(retryPolicy);
            Objects.requireNonNull(s3AsyncClient);
            Objects.requireNonNull(iamClient);
            Objects.requireNonNull(stsClient);
            Objects.requireNonNull(monitor);
            return new S3ProvisionPipeline(retryPolicy, s3AsyncClient, iamClient, stsClient, monitor, sessionDuration);
        }
    }

}
