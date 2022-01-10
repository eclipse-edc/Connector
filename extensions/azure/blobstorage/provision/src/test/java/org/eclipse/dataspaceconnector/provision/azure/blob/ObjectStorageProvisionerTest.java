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

import com.azure.storage.blob.models.BlobStorageException;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.common.azure.BlobStoreApi;
import org.eclipse.dataspaceconnector.provision.azure.AzureSasToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionContext;
import org.eclipse.dataspaceconnector.spi.transfer.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedDataDestinationResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectStorageProvisionerTest {

    private ObjectStorageProvisioner provisioner;
    private ProvisionContext provisionContextMock;
    private BlobStoreApi blobStoreApiMock;

    @BeforeEach
    void setup() {
        RetryPolicy<Object> retryPolicy = new RetryPolicy<>().withMaxRetries(0);
        Monitor monitor = mock(Monitor.class);
        blobStoreApiMock = mock(BlobStoreApi.class);
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
        var resourceDef = createResourceDef();

        String accountName = resourceDef.getAccountName();
        String containerName = resourceDef.getContainerName();
        when(blobStoreApiMock.exists(anyString(), anyString())).thenReturn(false);
        when(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any())).thenReturn("some-sas");

        var resourceArgument = ArgumentCaptor.forClass(ProvisionedDataDestinationResource.class);
        var tokenArgument = ArgumentCaptor.forClass(AzureSasToken.class);
        doNothing().when(provisionContextMock).callback(resourceArgument.capture(), tokenArgument.capture());

        var status = provisioner.provision(resourceDef);

        assertThat(status).isEqualTo(ResponseStatus.OK);
        assertThat(resourceArgument.getValue().getErrorMessage()).isNull();
        assertThat(resourceArgument.getValue().getTransferProcessId()).isEqualTo(resourceDef.getTransferProcessId());
        assertThat(tokenArgument.getValue().getSas()).isEqualTo("?some-sas");
        verify(blobStoreApiMock).exists(anyString(), anyString());
        verify(blobStoreApiMock).createContainer(accountName, containerName);
        verify(provisionContextMock).callback(resourceArgument.capture(), tokenArgument.capture());
    }

    @Test
    void provision_containerAlreadyExists() {
        var resourceDef = createResourceDef();
        String accountName = resourceDef.getAccountName();
        String containerName = resourceDef.getContainerName();

        when(blobStoreApiMock.exists(accountName, containerName)).thenReturn(true);
        when(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any())).thenReturn("some-sas");

        var resourceArgument = ArgumentCaptor.forClass(ProvisionedDataDestinationResource.class);
        var tokenArgument = ArgumentCaptor.forClass(AzureSasToken.class);
        doNothing().when(provisionContextMock).callback(resourceArgument.capture(), tokenArgument.capture());

        var status = provisioner.provision(resourceDef);

        assertThat(status).isEqualTo(ResponseStatus.OK);
        assertThat(resourceArgument.getValue().getErrorMessage()).isNull();
        assertThat(resourceArgument.getValue().getTransferProcessId()).isEqualTo(resourceDef.getTransferProcessId());
        assertThat(tokenArgument.getValue().getSas()).isEqualTo("?some-sas");
        verify(blobStoreApiMock).exists(anyString(), anyString());
        verify(blobStoreApiMock).createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any());
        verify(provisionContextMock).callback(resourceArgument.capture(), tokenArgument.capture());
    }


    @Test
    void provision_noKeyFoundInVault() {

        when(blobStoreApiMock.exists(any(), anyString()))
                .thenThrow(new IllegalArgumentException("No Object Storage credential found in vault!"));

        assertThatThrownBy(() -> provisioner.provision(createResourceDef())).isInstanceOf(IllegalArgumentException.class);
        verify(blobStoreApiMock).exists(any(), any());
    }

    @Test
    void provision_keyNotAuthorized() {
        var resourceDef = createResourceDef();

        when(blobStoreApiMock.exists(anyString(), anyString())).thenReturn(false);
        String accountName = resourceDef.getAccountName();
        String containerName = resourceDef.getContainerName();
        doThrow(new BlobStorageException("not authorized", null, null)).when(blobStoreApiMock).createContainer(accountName, containerName);

        assertThatThrownBy(() -> provisioner.provision(resourceDef)).isInstanceOf(BlobStorageException.class);
        verify(blobStoreApiMock).exists(anyString(), anyString());
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
