/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.provision.aws.s3;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.aws.s3.AwsClientProvider;
import org.eclipse.edc.aws.s3.AwsTemporarySecretToken;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.GetUserResponse;
import software.amazon.awssdk.services.iam.model.PutRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.PutRolePolicyResponse;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.User;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.time.Instant;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3BucketProvisionerTest {

    private S3BucketProvisioner provisioner;
    private final IamAsyncClient iamClient = mock(IamAsyncClient.class);
    private final StsAsyncClient stsClient = mock(StsAsyncClient.class);
    private final S3AsyncClient s3Client = mock(S3AsyncClient.class);
    private final AwsClientProvider clientProvider = mock(AwsClientProvider.class);

    @BeforeEach
    void setUp() {
        when(clientProvider.iamAsyncClient()).thenReturn(iamClient);
        when(clientProvider.s3AsyncClient(anyString())).thenReturn(s3Client);
        when(clientProvider.stsAsyncClient(anyString())).thenReturn(stsClient);

        var configuration = new S3BucketProvisionerConfiguration(2, 3600);

        provisioner = new S3BucketProvisioner(clientProvider, mock(Monitor.class), RetryPolicy.ofDefaults(), configuration);
    }

    @Test
    void verify_basic_provision() {
        var userResponse = GetUserResponse.builder().user(User.builder().arn("testarn").build()).build();
        var createRoleResponse = CreateRoleResponse.builder().role(Role.builder().roleName("roleName").arn("testarn").build()).build();
        var putRolePolicyResponse = PutRolePolicyResponse.builder().build();
        when(iamClient.getUser()).thenReturn(completedFuture(userResponse));
        when(iamClient.createRole(isA(CreateRoleRequest.class))).thenReturn(completedFuture(createRoleResponse));
        when(iamClient.putRolePolicy(isA(PutRolePolicyRequest.class))).thenReturn(completedFuture(putRolePolicyResponse));

        var credentials = Credentials.builder()
                .accessKeyId("accessKeyId").secretAccessKey("secretAccessKey").sessionToken("sessionToken")
                .expiration(Instant.now()).build();
        var assumeRoleResponse = AssumeRoleResponse.builder().credentials(credentials).build();
        when(stsClient.assumeRole(isA(AssumeRoleRequest.class))).thenReturn(completedFuture(assumeRoleResponse));

        var createBucketResponse = CreateBucketResponse.builder().build();
        when(s3Client.createBucket(isA(CreateBucketRequest.class))).thenReturn(completedFuture(createBucketResponse));

        S3BucketResourceDefinition definition = S3BucketResourceDefinition.Builder.newInstance().id("test").regionId(Region.US_EAST_1.id()).bucketName("test").transferProcessId("test").build();
        var policy = Policy.Builder.newInstance().build();

        var response = provisioner.provision(definition, policy).join().getContent();

        assertThat(response.getResource()).isInstanceOfSatisfying(S3BucketProvisionedResource.class, resource -> assertThat(resource.getRole()).isEqualTo("roleName"));
        assertThat(response.getSecretToken()).isInstanceOfSatisfying(AwsTemporarySecretToken.class, secretToken -> {
            assertThat(secretToken.getAccessKeyId()).isEqualTo("accessKeyId");
            assertThat(secretToken.getSecretAccessKey()).isEqualTo("secretAccessKey");
            assertThat(secretToken.getSessionToken()).isEqualTo("sessionToken");
        });
        verify(iamClient).putRolePolicy(isA(PutRolePolicyRequest.class));
    }

    @Test
    void should_return_failed_future_on_error() {
        when(s3Client.createBucket(isA(CreateBucketRequest.class))).thenReturn(failedFuture(new RuntimeException("any")));
        S3BucketResourceDefinition definition = S3BucketResourceDefinition.Builder.newInstance().id("test").regionId(Region.US_EAST_1.id()).bucketName("test").transferProcessId("test").build();

        var policy = Policy.Builder.newInstance().build();

        var response = provisioner.provision(definition, policy);

        assertThat(response).failsWithin(1, SECONDS);
    }

}
