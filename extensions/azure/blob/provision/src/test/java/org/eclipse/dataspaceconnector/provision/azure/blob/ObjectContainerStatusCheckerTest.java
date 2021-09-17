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
import org.eclipse.dataspaceconnector.azure.testfixtures.AbstractAzureBlobTest;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.common.azure.BlobStoreApiImpl;
import org.eclipse.dataspaceconnector.common.testfixtures.TestUtils;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.eclipse.dataspaceconnector.common.configuration.ConfigurationFunctions.propOrEnv;

@IntegrationTest
class ObjectContainerStatusCheckerTest extends AbstractAzureBlobTest {

    private File helloTxt;
    private ObjectContainerStatusChecker checker;

    @BeforeEach
    void setUp() {
        RetryPolicy<Object> policy = new RetryPolicy<>().withMaxRetries(1);
        helloTxt = TestUtils.getFileFromResourceName("hello.txt");
        Vault vault = mock(Vault.class);
        var accountKey = propOrEnv("AZ_STORAGE_KEY", null);
        assertThat(accountKey).describedAs("Azure Storage Account Key cannot be null!").isNotNull();

        expect(vault.resolveSecret(ACCOUNT_NAME + "-key1")).andReturn(accountKey).anyTimes();
        replay(vault);
        var blobStoreApi = new BlobStoreApiImpl(vault);
        checker = new ObjectContainerStatusChecker(blobStoreApi, policy);
    }

    @Test
    void isComplete() {
        putBlob("hello.txt", helloTxt);
        putBlob(testRunId + ".complete", helloTxt);

        assertThat(checker.isComplete(createResource(containerName))).isTrue();
    }

    @Test
    void isComplete_notComplete() {
        putBlob("hello.txt", helloTxt);

        assertThat(checker.isComplete(createResource(containerName))).isFalse();
    }

    @Test
    void isComplete_containerNotExist() {
        assertThat(checker.isComplete(createResource("container-not-exists"))).isFalse();
    }

    private ObjectContainerProvisionedResource createResource(String containerName) {
        return ObjectContainerProvisionedResource.Builder.newInstance()
                .containerName(containerName)
                .accountName(ACCOUNT_NAME)
                .resourceDefinitionId(UUID.randomUUID().toString())
                .transferProcessId(testRunId)
                .id(testRunId)
                .build();
    }
}
