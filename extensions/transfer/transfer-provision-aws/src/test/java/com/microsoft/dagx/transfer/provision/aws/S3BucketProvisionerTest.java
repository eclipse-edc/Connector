package com.microsoft.dagx.transfer.provision.aws;

import com.microsoft.dagx.spi.monitor.Monitor;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class S3BucketProvisionerTest {

    @Test
    void verifyBasicProvision() {

        S3AsyncClient mock = EasyMock.createMock(S3AsyncClient.class);
        var future = new CompletableFuture<CreateBucketResponse>();
        EasyMock.expect(mock.createBucket(EasyMock.isA(CreateBucketRequest.class))).andReturn(future);

        EasyMock.replay(mock);

        ClientProvider clientProvider = new ClientProvider() {
            @Override
            public <T extends SdkClient> T clientFor(Class<T> type, String key) {
                return type.cast(mock);
            }
        };
        
        S3BucketProvisioner provisioner = new S3BucketProvisioner(clientProvider, new Monitor() {
        });

        AtomicBoolean passed = new AtomicBoolean();
        provisioner.initialize(resource -> {
            if (resource.isError()) {
                throw new AssertionError("Resource creation errored");
            }
            passed.set(true);
        });

        provisioner.provision(S3BucketResourceDefinition.Builder.newInstance().id("test").regionId(Region.US_EAST_1.id()).bucketName("test").build());
        future.complete(CreateBucketResponse.builder().build());

        EasyMock.verify(mock);

        assertTrue(passed.get());
    }
}
