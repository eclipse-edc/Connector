package org.eclipse.dataspaceconnector.aws.s3.operator;


import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.transfer.inline.spi.DataWriter;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

public class S3BucketWriter implements DataWriter {

    private final RetryPolicy<Object> retryPolicy;
    private final Monitor monitor;
    private final TypeManager typeManager;

    public S3BucketWriter(Monitor monitor, TypeManager typeManager, RetryPolicy<Object> retryPolicy) {
        this.monitor = monitor;
        this.typeManager = typeManager;
        this.retryPolicy = retryPolicy;
    }

    private static StaticCredentialsProvider buildCredentialsProvider(AwsTemporarySecretToken awsSecretToken) {
        return StaticCredentialsProvider.create(AwsSessionCredentials.create(awsSecretToken.getAccessKeyId(), awsSecretToken.getSecretAccessKey(), awsSecretToken.getSessionToken()));
    }

    private static PutObjectRequest createRequest(String bucketName, String objectKey) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
    }

    @Override
    public boolean canHandle(String type) {
        return S3BucketSchema.TYPE.equals(type);
    }

    @Override
    public Result<Void> write(DataAddress destination, String name, InputStream data, String secretToken) {
        var bucketName = destination.getProperty(S3BucketSchema.BUCKET_NAME);
        var region = destination.getProperty(S3BucketSchema.REGION);
        var awsSecretToken = typeManager.readValue(secretToken, AwsTemporarySecretToken.class);

        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(buildCredentialsProvider(awsSecretToken))
                .region(Region.of(region))
                .build()) {
            PutObjectRequest request = createRequest(bucketName, name);
            PutObjectRequest completionMarker = createRequest(bucketName, name + ".complete");
            monitor.debug("Data request: begin transfer...");
            Failsafe.with(retryPolicy).get(() -> s3.putObject(request, RequestBody.fromBytes(data.readAllBytes())));
            Failsafe.with(retryPolicy).get(() -> s3.putObject(completionMarker, RequestBody.empty()));
            monitor.debug("Data request done.");
            return Result.success();
        } catch (S3Exception ex) {
            monitor.severe("Data request: transfer failed!");
            return Result.failure("Data transfer failed");
        }
    }
}

