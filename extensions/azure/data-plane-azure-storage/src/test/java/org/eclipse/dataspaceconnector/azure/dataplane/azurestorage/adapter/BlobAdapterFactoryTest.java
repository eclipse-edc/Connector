package org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.adapter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createAccountName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createBlobName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createContainerName;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.pipeline.AzureStorageTestFixtures.createSharedKey;

class BlobAdapterFactoryTest {

    @Test
    void getBlobAdapter_succeeds() {
        BlobAdapterFactory f = new BlobAdapterFactory(null);
        assertThatNoException()
                .isThrownBy(() -> f.getBlobAdapter(
                                createAccountName(),
                                createContainerName(),
                                createBlobName(),
                                createSharedKey()));
    }
}