package org.eclipse.dataspaceconnector.transfer;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.provision.aws.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.schema.s3.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.temporal.ChronoUnit;

class S3BucketWriter implements DataWriter {

    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final TypeManager typeManager;

    public S3BucketWriter(Monitor monitor, TypeManager typeManager) {
        this.monitor = monitor;
        this.typeManager = typeManager;
        retryPolicy = new RetryPolicy<>()
                .withBackoff(500, 5000, ChronoUnit.MILLIS)
                .withMaxRetries(3);
    }

    @Override
    public void write(DataAddress destination, String name, byte[] data, String secretToken) {
//        if (!(secretToken instanceof AwsSecretToken)) {
//            throw new IllegalArgumentException("Can only handle AwsSecretTokens!");
//        }

        var bucketName = destination.getProperty(S3BucketSchema.BUCKET_NAME);
        var region = destination.getProperty(S3BucketSchema.REGION);
        var awsSecretToken = typeManager.readValue(secretToken, AwsTemporarySecretToken.class);


        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(awsSecretToken.getAccessKeyId(), awsSecretToken.getSecretAccessKey(), awsSecretToken.getSessionToken())))
                .region(Region.of(region))
                .build()) {

            String etag = null;
            PutObjectRequest request = createRequest(bucketName, name);
            PutObjectRequest completionMarker = createRequest(bucketName, name + ".complete");

            try {
                monitor.debug("Data request: begin transfer...");
                var response = Failsafe.with(retryPolicy).get(() -> s3.putObject(request, RequestBody.fromBytes(data)));
                var response2 = Failsafe.with(retryPolicy).get(() -> s3.putObject(completionMarker, RequestBody.empty()));
                monitor.debug("Data request done.");
                etag = response.eTag();
            } catch (S3Exception tmpEx) {
                monitor.info("Data request: transfer not successful");
            }

        } catch (S3Exception ex) {
            monitor.severe("Data request: transfer failed!");
        }
    }

    private PutObjectRequest createRequest(String bucketName, String objectKey) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
    }

}
