package org.eclipse.dataspaceconnector.provision.aws.s3;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.DeprovisionResponse;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRolePolicyResponse;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class S3DeprovisionPipeline {

    private final RetryPolicy<Object> retryPolicy;
    private final S3AsyncClient s3Client;
    private final IamAsyncClient iamClient;
    private final Monitor monitor;

    public S3DeprovisionPipeline(RetryPolicy<Object> retryPolicy, S3AsyncClient s3Client, IamAsyncClient iamClient, Monitor monitor) {
        this.retryPolicy = retryPolicy;
        this.s3Client = s3Client;
        this.iamClient = iamClient;
        this.monitor = monitor;
    }

    /**
     * Performs a non-blocking deprovisioning operation.
     */
    public CompletableFuture<?> deprovision(S3BucketProvisionedResource resource) {
        String bucketName = resource.getBucketName();
        String role = resource.getRole();

        var listObjectsRequest = ListObjectsV2Request.builder().bucket(bucketName).build();
        return s3Client.listObjectsV2(listObjectsRequest)
                .thenCompose(listObjectsResponse -> deleteObjects(s3Client, bucketName, listObjectsResponse))
                .thenCompose(deleteObjectsResponse -> deleteBucket(s3Client, bucketName))
                .thenCompose(listAttachedRolePoliciesResponse -> deleteRolePolicy(iamClient, role))
                .thenCompose(deleteRolePolicyResponse -> deleteRole(iamClient, role))
                .thenApply(response -> DeprovisionResponse.Builder.newInstance().ok().resource(resource).build());
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

    static class Builder {
        private final RetryPolicy<Object> retryPolicy;
        private S3AsyncClient s3AsyncClient;
        private IamAsyncClient iamClient;
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

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public S3DeprovisionPipeline build() {
            Objects.requireNonNull(retryPolicy);
            Objects.requireNonNull(s3AsyncClient);
            Objects.requireNonNull(iamClient);
            Objects.requireNonNull(monitor);
            return new S3DeprovisionPipeline(retryPolicy, s3AsyncClient, iamClient, monitor);
        }
    }
}
