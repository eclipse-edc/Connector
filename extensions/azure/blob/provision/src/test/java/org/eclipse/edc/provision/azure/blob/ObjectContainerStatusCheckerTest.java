/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.edc.provision.azure.blob;

import org.eclipse.edc.common.annotations.IntegrationTest;
import org.eclipse.edc.common.azure.BlobStoreApiImpl;
import org.eclipse.edc.common.testfixtures.AbstractAzureBlobTest;
import org.eclipse.edc.common.testfixtures.TestUtils;
import org.eclipse.edc.spi.security.Vault;
import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.eclipse.edc.common.ConfigurationFunctions.propOrEnv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.*;

@IntegrationTest
class ObjectContainerStatusCheckerTest extends AbstractAzureBlobTest {

    private File helloTxt;
    private ObjectContainerStatusChecker checker;

    @BeforeEach
    void setUp() {
        RetryPolicy<Object> policy = new RetryPolicy<>().withMaxRetries(1);
        helloTxt = TestUtils.getFileFromResourceName("hello.txt");
        final Vault vault = mock(Vault.class);
        var accountKey = propOrEnv("AZ_STORAGE_KEY", null);
        assertThat(accountKey).describedAs("Azure Storage Account Key cannot be null!").isNotNull();

        expect(vault.resolveSecret(accountName + "-key1")).andReturn(accountKey).anyTimes();
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

    private ObjectContainerProvisionedResource createResource(String containerName) {
        return ObjectContainerProvisionedResource.Builder.newInstance()
                .containerName(containerName)
                .accountName(accountName)
                .resourceDefinitionId(UUID.randomUUID().toString())
                .transferProcessId(testRunId)
                .id(testRunId)
                .build();
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
}