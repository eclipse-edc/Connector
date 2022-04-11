package org.eclipse.dataspaceconnector.aws.testfixtures;

import org.eclipse.dataspaceconnector.aws.s3.core.S3ClientProvider;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

public class TestS3ClientProvider implements S3ClientProvider {

    private final AwsCredentials credentials;
    private final String s3Endpoint;

    public TestS3ClientProvider(AwsCredentials credentials, String s3Endpoint) {
        this.credentials = credentials;
        this.s3Endpoint = s3Endpoint;
    }

    @Override
    public S3Client provide(String region, SecretToken secretToken) {
        return testClient();
    }

    @Override
    public S3Client provide(String region, AwsCredentials credentials) {
        return testClient();
    }

    private S3Client testClient() {
        return S3Client.builder()
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("region"))
                .endpointOverride(URI.create(s3Endpoint))
                .build();
    }
}
