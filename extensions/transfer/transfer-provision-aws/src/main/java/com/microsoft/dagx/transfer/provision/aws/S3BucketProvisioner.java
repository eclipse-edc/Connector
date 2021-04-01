package com.microsoft.dagx.transfer.provision.aws;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.transfer.provision.Provisioner;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

import java.util.function.BiConsumer;

/**
 * Asynchronously provisions S3 buckets.
 */
public class S3BucketProvisioner implements Provisioner<S3BucketResourceDefinition, S3BucketProvisionedResource> {
    private ClientProvider clientProvider;
    private Monitor monitor;

    private ProvisionContext context;

    public S3BucketProvisioner(ClientProvider clientProvider, Monitor monitor) {
        this.clientProvider = clientProvider;
        this.monitor = monitor;
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
        String bucketName = resourceDefinition.getBucketName();

        CreateBucketRequest request = CreateBucketRequest.builder().bucket(bucketName).createBucketConfiguration(CreateBucketConfiguration.builder().build()).build();

        String region = resourceDefinition.getRegionId();
        clientProvider.clientFor(S3AsyncClient.class, region).createBucket(request).whenComplete(handleResponse(resourceDefinition));

        monitor.debug("Bucket request submitted: " + bucketName);
        return ResponseStatus.OK;
    }

    @Override
    public ResponseStatus deprovision(S3BucketProvisionedResource provisionedResource) {
        return ResponseStatus.OK;
    }

    @NotNull
    private BiConsumer<CreateBucketResponse, Throwable> handleResponse(S3BucketResourceDefinition resourceDefinition) {
        return (waiterResponse, exception) -> {
            String bucketName = resourceDefinition.getBucketName();
            if (waiterResponse != null) {
                context.callback(S3BucketProvisionedResource.Builder.newInstance().id(bucketName).resourceDefinitionId(resourceDefinition.getId()).build());
            } else if (exception != null) {
                sendErroredResource(resourceDefinition, bucketName, exception);
            }
        };
    }

    private void sendErroredResource(S3BucketResourceDefinition resourceDefinition, String bucketName, Throwable exception) {
        var exceptionToLog = exception.getCause() != null ? exception.getCause() : exception;
        S3BucketProvisionedResource erroredResource = S3BucketProvisionedResource.Builder.newInstance().
                id(bucketName).resourceDefinitionId(resourceDefinition.getId()).error(true).errorMessage(exceptionToLog.getMessage()).build();
        context.callback(erroredResource);
    }

}
