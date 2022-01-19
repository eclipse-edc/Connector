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
import org.eclipse.dataspaceconnector.aws.s3.core.ClientProvider;
import org.eclipse.dataspaceconnector.aws.s3.core.S3BucketSchema;
import org.eclipse.dataspaceconnector.aws.testfixtures.AbstractS3Test;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFileFromResourceName;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@IntegrationTest
class S3StatusCheckerIntegrationTest extends AbstractS3Test {

    public static final int ONE_MINUTE_MILLIS = 1000 * 60;
    private static final String PROCESS_ID = UUID.randomUUID().toString();

    private S3StatusChecker checker;

    @BeforeEach
    void setup() {
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxRetries(3).withBackoff(200, 1000, ChronoUnit.MILLIS);
        ClientProvider providerMock = mock(ClientProvider.class);
        when(providerMock.clientFor(eq(S3AsyncClient.class), anyString())).thenReturn(client);
        checker = new S3StatusChecker(providerMock, retryPolicy);
    }

    @Test
    void isComplete_noResources_whenNotComplete() {
        var complete = checker.isComplete(createTransferProcess(bucketName), emptyList());

        assertThat(complete).isFalse();
    }

    @Test
    void isComplete_noResources_whenComplete() throws InterruptedException {
        putTestFile(PROCESS_ID + ".complete", getFileFromResourceName("hello.txt"), bucketName);
        var transferProcess = createTransferProcess(bucketName);

        var hasCompleted = waitUntil(() -> checker.isComplete(transferProcess, emptyList()), ONE_MINUTE_MILLIS);

        assertThat(hasCompleted).isTrue();
    }

    @Test
    void isComplete_noResources_whenBucketNotExist() {
        var complete = checker.isComplete(createTransferProcess(bucketName), emptyList());

        assertThat(complete).isFalse();
    }

    @Test
    void isComplete_withResources_whenNotComplete() {
        var transferProcess = createTransferProcess(bucketName);
        var provisionedResource = createProvisionedResource(transferProcess);

        var complete = checker.isComplete(transferProcess, List.of(provisionedResource));

        assertThat(complete).isFalse();
    }

    @Test
    void isComplete_withResources_whenComplete() throws InterruptedException {
        putTestFile(PROCESS_ID + ".complete", getFileFromResourceName("hello.txt"), bucketName);
        TransferProcess tp = createTransferProcess(bucketName);

        var hasCompleted = waitUntil(() -> checker.isComplete(tp, emptyList()), ONE_MINUTE_MILLIS);

        assertThat(hasCompleted).isTrue();
    }

    @Test
    void isComplete_withResources_whenBucketNotExist() {
        var transferProcess = createTransferProcess(bucketName);
        var provisionedResource = createProvisionedResource(transferProcess);

        boolean complete = checker.isComplete(transferProcess, List.of(provisionedResource));

        assertThat(complete).isFalse();
    }

    private boolean waitUntil(Supplier<Boolean> conditionSupplier, long maxTimeMillis) throws InterruptedException {
        var done = false;
        var start = System.currentTimeMillis();
        var complete = false;
        while (!done) {
            complete = conditionSupplier.get();
            if (complete) {
                done = true;
            } else {
                done = System.currentTimeMillis() - start > maxTimeMillis;
            }
            Thread.sleep(5);
        }
        return complete;
    }

    private S3BucketProvisionedResource createProvisionedResource(TransferProcess transferProcess) {
        return S3BucketProvisionedResource.Builder.newInstance()
                .bucketName(bucketName)
                .region(REGION)
                .resourceDefinitionId(UUID.randomUUID().toString())
                .transferProcessId(transferProcess.getId())
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
