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
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import static java.lang.String.format;

public class DemoFlowController implements DataFlowController {
    private static final String ASSUME_ROLE_TRUST = "{" +
            "  \"Version\": \"2012-10-17\"," +
            "  \"Statement\": [" +
            "    {" +
            "      \"Effect\": \"Allow\"," +
            "      \"Principal\": {" +
            "        \"AWS\": \"%s\"" +
            "      }," +
            "      \"Action\": \"sts:AssumeRole\"" +
            "    }" +
            "  ]" +
            "}";

    private static final String BUCKET_POLICY = "{" +
            "    \"Version\": \"2012-10-17\"," +
            "    \"Statement\": [" +
            "        {" +
            "            \"Effect\": \"Allow\"," +
            "            \"Action\": \"s3:PutObject\"," +
            "            \"Resource\": \"arn:aws:s3:::%s/*\"" +
            "        }" +
            "    ]" +
            "}";
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

        return copyToBucket(content, bucketName, region, dt);

    }

    @NotNull
    private DataFlowInitiateResponse copyToBucket(String content, String bucketName, String region, DestinationSecretToken dt) {

        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(dt.getAccessKeyId(), dt.getSecretAccessKey(), dt.getToken())))
                .region(Region.of(region))
                .build()) {
            var wait = 2;//seconds
            var count = 0;
            var maxRetries = 3;
            var success = false;
            String etag = null;
            while (!success && count <= maxRetries) {
                try {
                    var response = s3.putObject(createRequest(bucketName, "demo-content"), RequestBody.fromString(content));
                    success = true;
                    etag = response.eTag();
                } catch (S3Exception tmpEx) {
                    System.out.println("not successful, retrying after " + wait + " seconds...");
                    count++;
                    Thread.sleep(1000L * wait);
                    wait *= wait;

                }
            }
            return new DataFlowInitiateResponse(ResponseStatus.OK, etag);
        } catch (S3Exception | InterruptedException ex) {
            return new DataFlowInitiateResponse(ResponseStatus.FATAL_ERROR, ex.getLocalizedMessage());
        }
    }

    @NotNull
    private AwsCredentialsProvider getCredentialsProvider(Region region, String bucketName) {

        var stsClient = clientProvider.clientFor(StsAsyncClient.class, region.id());

        var iamClient = clientProvider.clientFor(IamAsyncClient.class, region.id());
        User user = iamClient.getUser().join().user();
        var arn = user.arn();
        CreateRoleRequest.Builder roleBuilder = CreateRoleRequest.builder();
        String processId = "foobar-process-id";
        Tag tag = Tag.builder().key("dagx:process").value(processId).build();
        String roleName = "Put-To-Bucket-Role-" + System.currentTimeMillis();
        roleBuilder.roleName(roleName)
                .description("DA-GX transfer process role")
                .assumeRolePolicyDocument(format(ASSUME_ROLE_TRUST, arn))
                .maxSessionDuration(3600).tags(tag);

        Role role = iamClient.createRole(roleBuilder.build()).join().role();
        String policyDocument = format(BUCKET_POLICY, "arn:aws:s3::" + bucketName + "/*");
        PutRolePolicyRequest policyRequest = PutRolePolicyRequest.builder().policyName(processId).roleName(processId).roleName(role.roleName()).policyDocument(policyDocument).build();

        iamClient.putRolePolicy(policyRequest).join();

        AssumeRoleRequest.Builder assumeRoleBuilder = AssumeRoleRequest.builder();
        assumeRoleBuilder.roleArn(role.arn()).roleSessionName("foobar-role-session").externalId("yomama");

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        var creds = stsClient.assumeRole(assumeRoleBuilder.build()).join().credentials();

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

