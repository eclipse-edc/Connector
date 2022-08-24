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
import dev.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.azure.blob.core.AzureSasToken;
import org.eclipse.dataspaceconnector.azure.blob.core.api.BlobStoreApi;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectStorageProvisionerTest {

    private final BlobStoreApi blobStoreApiMock = mock(BlobStoreApi.class);
    private ObjectStorageProvisioner provisioner;
    private Policy policy;

    @BeforeEach
    void setup() {
        RetryPolicy<Object> retryPolicy = RetryPolicy.builder().withMaxRetries(0).build();
        provisioner = new ObjectStorageProvisioner(retryPolicy, mock(Monitor.class), blobStoreApiMock);
        policy = Policy.Builder.newInstance().build();
    }

    @Test
    void canProvision() {
        assertThat(provisioner.canProvision(new ObjectStorageResourceDefinition())).isTrue();
        assertThat(provisioner.canProvision(new ResourceDefinition() {
        })).isFalse();
    }

    @Test
    void canDeprovision() {
        var resource = createProvisionedResource();
        assertThat(provisioner.canDeprovision(resource)).isTrue();
        assertThat(provisioner.canDeprovision(new ProvisionedResource() {
        })).isFalse();
    }

    @Test
    void deprovision_should_not_do_anything() {
        ObjectContainerProvisionedResource resource = createProvisionedResource();
        var result = provisioner.deprovision(resource, policy);

        assertThat(result).succeedsWithin(1, SECONDS);
    }

    @Test
    void provision_success() {
        var resourceDef = createResourceDefinitionBuilder().transferProcessId("tpId").build();
        String accountName = resourceDef.getAccountName();
        String containerName = resourceDef.getContainerName();
        when(blobStoreApiMock.exists(anyString(), anyString())).thenReturn(false);
        when(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any())).thenReturn("some-sas");

        var response = provisioner.provision(resourceDef, policy).join().getContent();

        assertThat(response.getResource()).isInstanceOfSatisfying(ObjectContainerProvisionedResource.class, resource -> {
            assertThat(resource.getTransferProcessId()).isEqualTo("tpId");
        });
        assertThat(response.getSecretToken()).isInstanceOfSatisfying(AzureSasToken.class, secretToken -> {
            assertThat(secretToken.getSas()).isEqualTo("?some-sas");
        });
        verify(blobStoreApiMock).exists(anyString(), anyString());
        verify(blobStoreApiMock).createContainer(accountName, containerName);
    }

    @Test
    void provision_unique_name() {
        var resourceDef = createResourceDefinitionBuilder().id("id").transferProcessId("tpId").build();
        String accountName = resourceDef.getAccountName();
        String containerName = resourceDef.getContainerName();
        when(blobStoreApiMock.exists(accountName, containerName)).thenReturn(true);
        when(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any())).thenReturn("some-sas");

        var response = provisioner.provision(resourceDef, policy).join().getContent();

        var resourceDef2 = createResourceDefinitionBuilder().id("id2").transferProcessId("tpId2").build();
        var response2 = provisioner.provision(resourceDef2, policy).join().getContent();
        var resource1 = (ObjectContainerProvisionedResource) response.getResource();
        var resource2 = (ObjectContainerProvisionedResource) response2.getResource();
        assertThat(resource2.getResourceName()).isNotEqualTo(resource1.getResourceName());
    }

    @Test
    void provision_container_already_exists() {
        var resourceDef = createResourceDefinitionBuilder().transferProcessId("tpId").build();
        String accountName = resourceDef.getAccountName();
        String containerName = resourceDef.getContainerName();
        when(blobStoreApiMock.exists(accountName, containerName)).thenReturn(true);
        when(blobStoreApiMock.createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any())).thenReturn("some-sas");

        var response = provisioner.provision(resourceDef, policy).join().getContent();

        assertThat(response.getResource()).isInstanceOfSatisfying(ObjectContainerProvisionedResource.class, resource -> {
            assertThat(resource.getTransferProcessId()).isEqualTo("tpId");
        });
        assertThat(response.getSecretToken()).isInstanceOfSatisfying(AzureSasToken.class, secretToken -> {
            assertThat(secretToken.getSas()).isEqualTo("?some-sas");
        });
        verify(blobStoreApiMock).exists(anyString(), anyString());
        verify(blobStoreApiMock).createContainerSasToken(eq(accountName), eq(containerName), eq("w"), any());
    }

    @Test
    void provision_no_key_found_in_vault() {
        var resourceDefinition = createResourceDefinitionBuilder().build();
        when(blobStoreApiMock.exists(any(), anyString()))
                .thenThrow(new IllegalArgumentException("No Object Storage credential found in vault"));

        assertThatThrownBy(() -> provisioner.provision(resourceDefinition, policy).join()).hasCauseInstanceOf(IllegalArgumentException.class);
        verify(blobStoreApiMock).exists(any(), any());
    }

    @Test
    void provision_key_not_authorized() {
        var resourceDef = createResourceDefinitionBuilder().build();
        when(blobStoreApiMock.exists(anyString(), anyString())).thenReturn(false);
        doThrow(new BlobStorageException("not authorized", null, null))
                .when(blobStoreApiMock).createContainer(resourceDef.getAccountName(), resourceDef.getContainerName());

        assertThatThrownBy(() -> provisioner.provision(resourceDef, policy).join()).hasCauseInstanceOf(BlobStorageException.class);
        verify(blobStoreApiMock).exists(anyString(), anyString());
    }

    private ObjectStorageResourceDefinition.Builder createResourceDefinitionBuilder() {
        return ObjectStorageResourceDefinition.Builder
                .newInstance()
                .accountName("test-account-name")
                .containerName("test-container-name")
                .transferProcessId("test-process-id")
                .id("test-id");
    }

    private ObjectContainerProvisionedResource createProvisionedResource() {
        return ObjectContainerProvisionedResource.Builder.newInstance()
                .id("1")
                .transferProcessId("2")
                .resourceDefinitionId("3")
                .resourceName("resource")
                .build();
    }

}
