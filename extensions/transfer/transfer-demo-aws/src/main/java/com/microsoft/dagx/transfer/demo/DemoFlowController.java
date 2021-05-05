/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.schema.aws.S3BucketSchema;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import com.microsoft.dagx.transfer.provision.aws.provider.ClientProvider;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;

public class DemoFlowController implements DataFlowController {
    private final Vault vault;
    private final ClientProvider clientProvider;

    public DemoFlowController(Vault vault, ClientProvider clientProvider) {
        this.vault = vault;
        this.clientProvider = clientProvider;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return true;
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {

        var content = createRandomContent();
        var awsSecretName = dataRequest.getDataDestination().getKeyName();
        var awsSecret = vault.resolveSecret(awsSecretName);
        var bucketName = dataRequest.getDataDestination().getProperty(S3BucketSchema.BUCKET_NAME);

        var region = dataRequest.getDataDestination().getProperty(S3BucketSchema.REGION);

        var dt = convertSecret(awsSecret);

        return copyToBucket(content, bucketName, region);

    }

    @NotNull
    private DataFlowInitiateResponse copyToBucket(String content, String bucketName, String region) {

        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(getCredentialsProvider(Region.of(region)))
                .region(Region.of(region))
                .build()) {
            var response = s3.putObject(createRequest(bucketName, "demo-content"), RequestBody.fromString(content));
            return new DataFlowInitiateResponse(ResponseStatus.OK, response.eTag());
        } catch (S3Exception ex) {
            return new DataFlowInitiateResponse(ResponseStatus.FATAL_ERROR, ex.getLocalizedMessage());
        }
    }

    @NotNull
    private AwsCredentialsProvider getCredentialsProvider(Region region) {

        var stsClient = clientProvider.clientFor(StsAsyncClient.class, region.id());

        var str = GetSessionTokenRequest.builder()
                .durationSeconds(3600)
                .build();

        var creds = stsClient.getSessionToken(str).join().credentials();


        return StaticCredentialsProvider.create(AwsSessionCredentials.create(creds.accessKeyId(), creds.secretAccessKey(), creds.sessionToken()));

    }

    private DestinationSecretToken convertSecret(String awsSecret) {
        try {
            var mapper = new ObjectMapper();
            return mapper.readValue(awsSecret, DestinationSecretToken.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private PutObjectRequest createRequest(String bucketName, String objectKey) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
    }

    private String createRandomContent() {
        return "yo mama so fat";
    }
}

