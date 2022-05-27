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

package org.eclipse.dataspaceconnector.aws.s3.provision;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.aws.s3.core.AwsTemporarySecretToken;
import org.eclipse.dataspaceconnector.aws.s3.core.ClientProvider;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.SdkClient;
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
import static org.mockito.ArgumentMatchers.any;
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
    private final ClientProvider clientProvider = mock(ClientProvider.class);

    @BeforeEach
    void setUp() {
        when(clientProvider.clientFor(any(), anyString())).thenAnswer(chooseClient());

        var configuration = new S3BucketProvisionerConfiguration(2, 3600);

        provisioner = new S3BucketProvisioner(clientProvider, mock(Monitor.class), new RetryPolicy<>(), configuration);
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

    @NotNull
    private Answer<Object> chooseClient() {
        return invocation -> {
            Class<SdkClient> skdClient = invocation.getArgument(0, Class.class);
            if (skdClient.isAssignableFrom(IamAsyncClient.class)) {
                return iamClient;
            } else if (skdClient.isAssignableFrom(StsAsyncClient.class)) {
                return stsClient;
            } else if (skdClient.isAssignableFrom(S3AsyncClient.class)) {
                return s3Client;
            } else {
                throw new RuntimeException("unexpected provider SdkClient type");
            }
        };
    }

}
