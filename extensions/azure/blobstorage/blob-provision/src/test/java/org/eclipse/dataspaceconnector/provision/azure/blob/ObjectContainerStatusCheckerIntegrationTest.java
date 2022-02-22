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

package org.eclipse.dataspaceconnector.provision.azure.blob;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApiImpl;
import org.eclipse.dataspaceconnector.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@IntegrationTest
class ObjectContainerStatusCheckerIntegrationTest extends AbstractAzureBlobTest {

    private File helloTxt;
    private ObjectContainerStatusChecker checker;

    @BeforeEach
    void setUp() {
        var policy = new RetryPolicy<>().withMaxRetries(1);
        helloTxt = TestUtils.getFileFromResourceName("hello.txt");
        Vault vault = mock(Vault.class);

        when(vault.resolveSecret(account1Name + "-key1")).thenReturn(account1Key);
        var blobStoreApi = new BlobStoreApiImpl(vault, getEndpoint(account1Name));
        checker = new ObjectContainerStatusChecker(blobStoreApi, policy);
    }

    @Test
    void isComplete_noResources() {
        putBlob("hello.txt", helloTxt);
        putBlob(testRunId + ".complete", helloTxt);
        var transferProcess = createTransferProcess(account1ContainerName);

        boolean complete = checker.isComplete(transferProcess, emptyList());

        assertThat(complete).isTrue();
    }

    @Test
    void isComplete_noResources_notComplete() {
        putBlob("hello.txt", helloTxt);

        var tp = createTransferProcess(account1ContainerName);
        assertThat(checker.isComplete(tp, emptyList())).isFalse();
    }

    @Test
    void isComplete_noResources_containerNotExist() {
        var tp = createTransferProcess(account1ContainerName);
        assertThat(checker.isComplete(tp, emptyList())).isFalse();
    }

    @Test
    void isComplete_withResources() {
        putBlob("hello.txt", helloTxt);
        putBlob(testRunId + ".complete", helloTxt);

        var tp = createTransferProcess(account1ContainerName);
        var pr = createProvisionedResource(tp);
        assertThat(checker.isComplete(tp, singletonList(pr))).isTrue();
    }

    @Test
    void isComplete_withResources_notComplete() {
        putBlob("hello.txt", helloTxt);

        var tp = createTransferProcess(account1ContainerName);
        var pr = createProvisionedResource(tp);
        assertThat(checker.isComplete(tp, singletonList(pr))).isFalse();
    }

    @Test
    void isComplete_withResources_containerNotExist() {
        var tp = createTransferProcess(account1ContainerName);
        var pr = createProvisionedResource(tp);
        assertThat(checker.isComplete(tp, singletonList(pr))).isFalse();
    }

    private TransferProcess createTransferProcess(String containerName) {
        return TransferProcess.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .dataRequest(DataRequest.Builder.newInstance()
                        .destinationType(AzureBlobStoreSchema.TYPE)
                        .dataDestination(DataAddress.Builder.newInstance()
                                .type(AzureBlobStoreSchema.TYPE)
                                .property(AzureBlobStoreSchema.CONTAINER_NAME, containerName)
                                .property(AzureBlobStoreSchema.ACCOUNT_NAME, account1Name)
                                //.property(AzureBlobStoreSchema.BLOB_NAME, ???) omitted on purpose
                                .build())
                        .build())
                .build();
    }

    private ObjectContainerProvisionedResource createProvisionedResource(TransferProcess tp) {
        return ObjectContainerProvisionedResource.Builder.newInstance()
                .containerName(account1ContainerName)
                .accountName(account1Name)
                .resourceDefinitionId(UUID.randomUUID().toString())
                .transferProcessId(tp.getId())
                .id(UUID.randomUUID().toString())
                .build();
    }
}
