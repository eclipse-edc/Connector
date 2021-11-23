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

package org.eclipse.dataspaceconnector.provision.aws.s3;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.aws.testfixtures.AbstractS3Test;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.provision.aws.provider.ClientProvider;
import org.eclipse.dataspaceconnector.schema.s3.S3BucketSchema;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFileFromResourceName;

@IntegrationTest
class S3StatusCheckerIntegrationTest extends AbstractS3Test {
    private static final String PROCESS_ID = UUID.randomUUID().toString();
    private S3StatusChecker checker;

    @BeforeEach
    void setup() {
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxRetries(3).withBackoff(200, 1000, ChronoUnit.MILLIS);
        ClientProvider providerMock = mock(ClientProvider.class);
        expect(providerMock.clientFor(S3AsyncClient.class, REGION)).andReturn(S3AsyncClient.builder()
                .region(Region.of(REGION))
                .credentialsProvider(() -> AwsBasicCredentials.create(credentials.getAWSAccessKeyId(), credentials.getAWSSecretKey())).build());
        replay(providerMock);
        checker = new S3StatusChecker(providerMock, retryPolicy);
    }

    @Test
    void isComplete_noResources_whenNotComplete() {
        assertThat(checker.isComplete(createTransferProcess(bucketName), emptyList())).isFalse();
    }

    @Test
    void isComplete_noResources_whenComplete() {
        //arrange
        putTestFile(PROCESS_ID + ".complete", getFileFromResourceName("hello.txt"), bucketName);

        //act-assert
        await().atMost(10, TimeUnit.SECONDS).until(() -> checker.isComplete(createTransferProcess(bucketName), emptyList()));
    }

    @Test
    void isComplete_noResources_whenBucketNotExist() {
        assertThat(checker.isComplete(createTransferProcess(bucketName), emptyList())).isFalse();
    }

    @Test
    void isComplete_withResources_whenNotComplete() {

        TransferProcess tp = createTransferProcess(bucketName);
        S3BucketProvisionedResource provisionedResource = createProvisionedResource(tp);

        assertThat(checker.isComplete(tp, Collections.singletonList(provisionedResource))).isFalse();
    }

    @Test
    void isComplete_withResources_whenComplete() {
        //arrange
        putTestFile(PROCESS_ID + ".complete", getFileFromResourceName("hello.txt"), bucketName);

        //act-assert
        TransferProcess tp = createTransferProcess(bucketName);
        S3BucketProvisionedResource provisionedResource = createProvisionedResource(tp);
        await().atMost(10, TimeUnit.SECONDS).until(() -> checker.isComplete(tp, Collections.singletonList(provisionedResource)));
    }

    @Test
    void isComplete_withResources_whenBucketNotExist() {
        TransferProcess tp = createTransferProcess(bucketName);
        S3BucketProvisionedResource provisionedResource = createProvisionedResource(tp);
        assertThat(checker.isComplete(tp, Collections.singletonList(provisionedResource))).isFalse();
    }

    @Override
    protected @NotNull String createBucketName() {
        return "s3-checker-test-" + PROCESS_ID + "-" + REGION;
    }

    private S3BucketProvisionedResource createProvisionedResource(TransferProcess tp) {
        return S3BucketProvisionedResource.Builder.newInstance()
                .bucketName(bucketName)
                .region(REGION)
                .resourceDefinitionId(UUID.randomUUID().toString())
                .transferProcessId(tp.getId())
                .id(UUID.randomUUID().toString())
                .build();
    }

    private TransferProcess createTransferProcess(String bucketName) {
        return TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataRequest(DataRequest.Builder.newInstance()
                        .destinationType(S3BucketSchema.TYPE)
                        .dataDestination(DataAddress.Builder.newInstance()
                                .type(S3BucketSchema.TYPE)
                                .property(S3BucketSchema.REGION, AbstractS3Test.REGION)
                                .property(S3BucketSchema.BUCKET_NAME, bucketName)
                                .build())
                        .build())
                .build();
    }


}
