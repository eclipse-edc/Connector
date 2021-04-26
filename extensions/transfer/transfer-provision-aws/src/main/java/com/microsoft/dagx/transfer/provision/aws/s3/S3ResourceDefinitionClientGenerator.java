package com.microsoft.dagx.transfer.provision.aws.s3;

import com.microsoft.dagx.spi.transfer.provision.ResourceDefinitionGenerator;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import com.microsoft.dagx.transfer.types.aws.S3Destination;
import software.amazon.awssdk.regions.Region;

import static java.util.UUID.randomUUID;

/**
 * Generates S3 buckets on the client (requesting connector) that serve as data destinations.
 */
public class S3ResourceDefinitionClientGenerator implements ResourceDefinitionGenerator {

    @Override
    public ResourceDefinition generate(TransferProcess process) {
        var request = process.getDataRequest();
        if (request.getDestinationType() != null) {
            if (!S3Destination.TYPE.equals(request.getDestinationType())) {
                return null;
            }
            // FIXME generate region from policy engine
            return S3BucketResourceDefinition.Builder.newInstance().id(randomUUID().toString()).bucketName(process.getId()).regionId(Region.US_EAST_1.id()).build();

        } else if (!(request.getDataDestination() instanceof S3Destination)) {
            return null;
        }
        S3Destination s3Destination = (S3Destination) request.getDataDestination();
        return S3BucketResourceDefinition.Builder.newInstance().id(randomUUID().toString()).bucketName(s3Destination.getBucketName()).regionId(s3Destination.getRegion()).build();
    }
}
