package org.eclipse.dataspaceconnector.aws.s3.operator;

import org.eclipse.dataspaceconnector.aws.s3.core.AwsSecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.s3.core.S3ClientProvider;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.inline.DataReader;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.ByteArrayInputStream;

public class S3BucketReader implements DataReader {

    private final Monitor monitor;
    private final Vault vault;
    private final S3ClientProvider clientProvider;

    public S3BucketReader(Monitor monitor, Vault vault, S3ClientProvider clientProvider) {
        this.monitor = monitor;
        this.vault = vault;
        this.clientProvider = clientProvider;
    }

    @Override
    public boolean canHandle(String type) {
        return S3BucketSchema.TYPE.equals(type);
    }

    @Override
    public Result<ByteArrayInputStream> read(DataAddress source) {
        var region = source.getProperty(S3BucketSchema.REGION);
        var bucketName = source.getProperty(S3BucketSchema.BUCKET_NAME);
        var accessKeyId = this.vault.resolveSecret("aws-access-key-id");
        var secretAccessKey = this.vault.resolveSecret("aws-secret-access-key");
        var token = new AwsSecretToken(accessKeyId, secretAccessKey);

        try (var s3 = clientProvider.provide(region, token)) {
            var request = GetObjectRequest.builder().bucket(bucketName).key(source.getKeyName()).build();
            monitor.debug("Data request: begin transfer...");
            var response = s3.getObject(request);
            var inputStream = new ByteArrayInputStream(response.readAllBytes());
            monitor.debug("Data request done.");
            return Result.success(inputStream);
        } catch (Exception ex) {
            this.monitor.severe("Data request: transfer failed!", ex);
            return Result.failure("Data request: transfer failed!");
        }
    }

}
