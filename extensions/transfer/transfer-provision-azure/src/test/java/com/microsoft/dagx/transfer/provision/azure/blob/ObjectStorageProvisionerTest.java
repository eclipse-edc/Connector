/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.transfer.provision.azure.blob;

import com.azure.storage.blob.models.BlobStorageException;
import com.microsoft.dagx.common.azure.BlobStoreApi;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.provision.ProvisionContext;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import com.microsoft.dagx.spi.types.domain.transfer.ProvisionedResource;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceDefinition;
import com.microsoft.dagx.transfer.provision.azure.AzureSasToken;
import net.jodah.failsafe.RetryPolicy;
import org.easymock.Capture;
import org.easymock.MockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.easymock.EasyMock.*;

class ObjectStorageProvisionerTest {

    private ObjectStorageProvisioner provisioner;
    private ProvisionContext provisionContextMock;
    private BlobStoreApi blobStoreApiMock;

    @BeforeEach
    void setup() {
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxRetries(0);
        Monitor monitor = niceMock(Monitor.class);
        blobStoreApiMock = mock(MockType.STRICT, BlobStoreApi.class);
        provisionContextMock = mock(ProvisionContext.class);

        provisioner = new ObjectStorageProvisioner(retryPolicy, monitor, blobStoreApiMock);
        provisioner.initialize(provisionContextMock);
    }

    @Test
    void canProvision() {
        assertThat(provisioner.canProvision(new ObjectStorageResourceDefinition())).isTrue();
        assertThat(provisioner.canProvision(new ResourceDefinition() {
        })).isFalse();
    }

    @Test
    void canDeprovision() {
        assertThat(provisioner.canDeprovision(new ObjectContainerProvisionedResource())).isTrue();
        assertThat(provisioner.canDeprovision(new ProvisionedResource() {
        })).isFalse();
    }

    @Test
    void deprovision_shouldNotDoAnything() {
        assertThat(provisioner.deprovision(new ObjectContainerProvisionedResource())).isEqualTo(ResponseStatus.OK);
    }

    @Test
    void provision_success() {
        //arrange
        var resourceDef = createResourceDef();

        //setup blob store api mock
        expect(blobStoreApiMock.exists(anyString(), anyString())).andReturn(false);
        String accountName = resourceDef.getAccountName();
        String containerName = resourceDef.getContainerName();
        blobStoreApiMock.createContainer(accountName, containerName);
        expectLastCall();
        expect(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), anyObject())).andReturn("some-sas");
        replay(blobStoreApiMock);

        //setup context mock
        Capture<ProvisionedDataDestinationResource> resourceArgument = newCapture();
        Capture<AzureSasToken> tokenArgument = newCapture();
        provisionContextMock.callback(capture(resourceArgument), capture(tokenArgument));
        replay(provisionContextMock);

        //act
        var status = provisioner.provision(resourceDef);

        //assert
        assertThat(status).isEqualTo(ResponseStatus.OK);
        verify(blobStoreApiMock);
        assertThat(resourceArgument.getValue().getErrorMessage()).isNull();
        assertThat(resourceArgument.getValue().getTransferProcessId()).isEqualTo(resourceDef.getTransferProcessId());
        assertThat(tokenArgument.getValue().getSas()).isEqualTo("?some-sas");

    }

    @Test
    void provision_containerAlreadyExists() {
        //arrange
        var resourceDef = createResourceDef();
        String accountName = resourceDef.getAccountName();
        String containerName = resourceDef.getContainerName();

        //setup blob store api mock
        expect(blobStoreApiMock.exists(accountName, containerName)).andReturn(true);
        expect(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), anyObject())).andReturn("some-sas");

        replay(blobStoreApiMock);

        //setup context mock
        Capture<ProvisionedDataDestinationResource> resourceArgument = newCapture();
        Capture<AzureSasToken> tokenArgument = newCapture();
        provisionContextMock.callback(capture(resourceArgument), capture(tokenArgument));
        replay(provisionContextMock);

        //act
        var status = provisioner.provision(resourceDef);

        //assert
        assertThat(status).isEqualTo(ResponseStatus.OK);
        verify(blobStoreApiMock);
        verifyUnexpectedCalls(blobStoreApiMock);
        assertThat(resourceArgument.getValue().getErrorMessage()).isNull();
        assertThat(resourceArgument.getValue().getTransferProcessId()).isEqualTo(resourceDef.getTransferProcessId());
        assertThat(tokenArgument.getValue().getSas()).isEqualTo("?some-sas");
    }


    @Test
    void provision_noKeyFoundInVault() {

        expect(blobStoreApiMock.exists(anyObject(), anyString()))
                .andThrow(new IllegalArgumentException("No Object Storage credential found in vault!"));
        replay(blobStoreApiMock);

        assertThatThrownBy(() -> provisioner.provision(createResourceDef())).isInstanceOf(IllegalArgumentException.class);
        verify(blobStoreApiMock);
    }

    @Test
    void provision_keyNotAuthorized() {
        //arrange
        var resourceDef = createResourceDef();

        //setup blob store api mock
        expect(blobStoreApiMock.exists(anyString(), anyString())).andReturn(false);
        String accountName = resourceDef.getAccountName();
        String containerName = resourceDef.getContainerName();
        blobStoreApiMock.createContainer(accountName, containerName);
        expectLastCall().andThrow(new BlobStorageException("not authorized", null, null));

        replay(blobStoreApiMock);

        assertThatThrownBy(() -> provisioner.provision(resourceDef)).isInstanceOf(BlobStorageException.class);
        verify(blobStoreApiMock);
    }

    private ObjectStorageResourceDefinition createResourceDef() {
        return ObjectStorageResourceDefinition.Builder
                .newInstance()
                .accountName("test-account-name")
                .containerName("test-container-name")
                .transferProcessId("test-process-id")
                .id("test-id")
                .build();
    }
}