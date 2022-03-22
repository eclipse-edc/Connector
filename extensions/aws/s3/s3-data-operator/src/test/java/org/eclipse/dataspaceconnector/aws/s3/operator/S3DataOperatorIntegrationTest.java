package org.eclipse.dataspaceconnector.aws.s3.operator;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.s3.core.S3ClientProvider;
import org.eclipse.dataspaceconnector.aws.testfixtures.AbstractS3Test;
import org.eclipse.dataspaceconnector.aws.testfixtures.annotations.AwsS3IntegrationTest;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.SecretToken;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.io.ByteArrayInputStream;
import java.net.URI;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema.TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AwsS3IntegrationTest
public class S3DataOperatorIntegrationTest extends AbstractS3Test {

    private final TypeManager typeManager = new TypeManager();
    private final S3ClientProvider clientProvider = new TestS3ClientProvider(getCredentials(), S3_ENDPOINT);
    private final Monitor monitor = new ConsoleMonitor();
    private final Vault vault = mock(Vault.class);

    @Test
    void shouldWriteAndReadFromS3Bucket() {
        var reader = new S3BucketReader(monitor, vault, clientProvider);
        var writer = new S3BucketWriter(monitor, typeManager, new RetryPolicy<>(), clientProvider);
        var address = DataAddress.Builder.newInstance()
                .type(TYPE)
                .keyName("key")
                .property(S3BucketSchema.REGION, "any")
                .property(S3BucketSchema.BUCKET_NAME, bucketName)
                .build();
        var credentials = getCredentials();
        var secret = typeManager.writeValueAsString(new AwsTemporarySecretToken(credentials.accessKeyId(), credentials.secretAccessKey(), "", 3600));
        when(vault.resolveSecret("aws-credentials")).thenReturn(secret);
        when(vault.resolveSecret("aws-access-key-id")).thenReturn(credentials.accessKeyId());
        when(vault.resolveSecret("aws-secret-access-key")).thenReturn(credentials.secretAccessKey());

        var writeResult = writer.write(address, "key", new ByteArrayInputStream("content".getBytes()), secret);
        assertThat(writeResult.succeeded()).isEqualTo(true);

        var result = reader.read(address);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).hasBinaryContent("content".getBytes());
    }

    private static class TestS3ClientProvider implements S3ClientProvider {

        private final AwsCredentials credentials;
        private final String s3Endpoint;

        public TestS3ClientProvider(AwsCredentials credentials, String s3Endpoint) {
            this.credentials = credentials;
            this.s3Endpoint = s3Endpoint;
        }

        @Override
        public S3Client provide(String region, SecretToken secretToken) {
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
}
