/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.edc.connector.provision.gcp;


import org.eclipse.edc.gcp.common.GcpAccessToken;
import org.eclipse.edc.gcp.common.GcpCredentials;
import org.eclipse.edc.gcp.common.GcpException;
import org.eclipse.edc.gcp.common.GcpServiceAccount;
import org.eclipse.edc.gcp.common.GcsBucket;
import org.eclipse.edc.gcp.iam.IamService;
import org.eclipse.edc.gcp.storage.StorageService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class GcsProvisionerTest {


    private GcsProvisioner provisioner;
    private StorageService storageServiceMock;
    private IamService iamServiceMock;
    private Policy testPolicy;

    @BeforeEach
    void setUp() {
        storageServiceMock = mock(StorageService.class);
        iamServiceMock = mock(IamService.class);
        testPolicy = Policy.Builder.newInstance().build();
        provisioner = new GcsProvisioner(mock(Monitor.class), mock(GcpCredentials.class), null);
    }

    @Test
    void canProvisionGcsResource() {
        var gcsResource = GcsResourceDefinition.Builder.newInstance()
                .id("TEST").location("TEST").storageClass("TEST")
                .build();
        assertThat(provisioner.canProvision(gcsResource)).isTrue();
    }

    @Test
    void provisionSuccessWithoutProjectId() {
        var resourceDefinitionId = "id";
        var location = "location";
        var storageClass = "storage-class";
        var transferProcessId = UUID.randomUUID().toString();
        var resourceDefinition = createResourceDefinition(resourceDefinitionId, location,
                storageClass, transferProcessId, null);
        assertThatExceptionOfType(GcpException.class).isThrownBy(() ->
                provisioner.provision(resourceDefinition, testPolicy));

    }

    @Test
    void provisionSuccess() {
        var resourceDefinitionId = "id";
        var location = "location";
        var storageClass = "storage-class";
        var projectId = "projectIdTest";
        var transferProcessId = UUID.randomUUID().toString();
        var resourceDefinition = createResourceDefinition(resourceDefinitionId, location,
                storageClass, transferProcessId, projectId);
        var bucketName = resourceDefinition.getId();
        var bucketLocation = resourceDefinition.getLocation();

        var bucket = new GcsBucket(bucketName);
        var serviceAccount = new GcpServiceAccount("test-sa", "sa-name", "description");
        var token = new GcpAccessToken("token", 123);

        when(storageServiceMock.getOrCreateEmptyBucket(bucketName, bucketLocation)).thenReturn(bucket);
        when(storageServiceMock.isEmpty(bucketName)).thenReturn(true);
        when(iamServiceMock.getOrCreateServiceAccount(anyString(), anyString())).thenReturn(serviceAccount);
        doNothing().when(storageServiceMock).addProviderPermissions(bucket, serviceAccount);
        when(iamServiceMock.createAccessToken(serviceAccount)).thenReturn(token);

        var response = provisioner.provision(resourceDefinition, testPolicy, iamServiceMock, storageServiceMock).join().getContent();

        assertThat(response.getResource()).isInstanceOfSatisfying(GcsProvisionedResource.class, resource -> {
            assertThat(resource.getId()).isEqualTo(resourceDefinitionId);
            assertThat(resource.getTransferProcessId()).isEqualTo(transferProcessId);
            assertThat(resource.getLocation()).isEqualTo(location);
            assertThat(resource.getStorageClass()).isEqualTo(storageClass);
            assertThat(resource.getProjectId()).isEqualTo(projectId);
        });

        assertThat(response.getSecretToken()).isInstanceOfSatisfying(GcpAccessToken.class, secretToken -> assertThat(secretToken.getToken()).isEqualTo("token"));

        verify(storageServiceMock).getOrCreateEmptyBucket(bucketName, bucketLocation);
        verify(storageServiceMock).addProviderPermissions(bucket, serviceAccount);
        verify(iamServiceMock).createAccessToken(serviceAccount);
    }

    @Test
    void provisionFailsIfBucketNotEmpty() {
        var resourceDefinition = createResourceDefinition();
        var bucketName = resourceDefinition.getId();
        var bucketLocation = resourceDefinition.getLocation();

        when(storageServiceMock.getOrCreateEmptyBucket(bucketName, bucketLocation)).thenReturn(new GcsBucket(bucketName));
        when(storageServiceMock.isEmpty(bucketName)).thenReturn(false);

        var response = provisioner.provision(resourceDefinition, testPolicy, iamServiceMock, storageServiceMock).join();

        assertThat(response.failed()).isTrue();
        assertThat(response.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);

        verify(storageServiceMock).getOrCreateEmptyBucket(bucketName, bucketLocation);
        verify(storageServiceMock, times(0)).addProviderPermissions(any(), any());
        verify(iamServiceMock, times(0)).createAccessToken(any());
    }


    @Test
    void provisionFailsBecauseOfApiError() {
        var resourceDefinition = createResourceDefinition();
        var bucketName = resourceDefinition.getId();
        var bucketLocation = resourceDefinition.getLocation();

        doThrow(new GcpException("some error")).when(storageServiceMock).getOrCreateEmptyBucket(bucketName, bucketLocation);

        var response = provisioner.provision(resourceDefinition, testPolicy, iamServiceMock, storageServiceMock).join();
        assertThat(response.failed()).isTrue();
    }

    @Test
    void canDeprovisionGcsResource() {
        var gcsProvisionedResource = GcsProvisionedResource.Builder.newInstance().id("TEST")
                .transferProcessId("TEST").resourceDefinitionId("TEST").resourceName("TEST").build();
        assertThat(provisioner.canDeprovision(gcsProvisionedResource)).isTrue();
    }

    @Test
    void deprovisionSuccess() {
        var email = "test-email";
        var name = "test-name";
        var id = "test-id";
        var description = "sa-description";
        var serviceAccount = new GcpServiceAccount(email, name, description);

        doNothing().when(iamServiceMock).deleteServiceAccountIfExists(argThat(matches(serviceAccount)));
        var resource = createGcsProvisionedResource(email, name, id);

        var response = provisioner.deprovision(resource, iamServiceMock).join().getContent();
        verify(iamServiceMock).deleteServiceAccountIfExists(argThat(matches(serviceAccount)));
        assertThat(response.getProvisionedResourceId()).isEqualTo(id);
    }

    @Test
    void deprovisionFailIfProjectIdIsNotProvided() {
        var resource = GcsProvisionedResource.Builder.newInstance().resourceName("name")
                .id("test-id")
                .resourceDefinitionId("test-id")
                .bucketName("bucket")
                .location("location")
                .storageClass("standard")
                .transferProcessId("transfer-id")
                .serviceAccountName("test-name")
                .serviceAccountEmail("test-name")
                .build();

        assertThatExceptionOfType(GcpException.class).isThrownBy(() ->
                provisioner.deprovision(resource, testPolicy));
    }


    private GcsProvisionedResource createGcsProvisionedResource(String serviceAccountEmail, String serviceAccountName, String id) {
        return GcsProvisionedResource.Builder.newInstance().resourceName("name")
                .id(id)
                .resourceDefinitionId(id)
                .bucketName("bucket")
                .location("location")
                .storageClass("standard")
                .transferProcessId("transfer-id")
                .projectId("project-id")
                .serviceAccountName(serviceAccountName)
                .serviceAccountEmail(serviceAccountEmail)
                .build();
    }

    private GcsResourceDefinition createResourceDefinition() {
        return createResourceDefinition("id", "location",
                "storage-class", "transfer-id", "projectId-test");
    }

    private GcsResourceDefinition createResourceDefinition(String id, String location, String storageClass, String transferProcessId, String projectId) {
        return GcsResourceDefinition.Builder.newInstance().id(id)
                .location(location).storageClass(storageClass)
                .transferProcessId(transferProcessId)
                .projectId(projectId).build();
    }

    @Test
    void deprovisionFails() {
        var email = "test-email";
        var name = "test-name";
        var id = "test-id";
        var description = "sa-description";
        var serviceAccount = new GcpServiceAccount(email, name, description);
        GcsProvisionedResource resource = createGcsProvisionedResource(email, name, id);

        doThrow(new GcpException("some error"))
                .when(iamServiceMock)
                .deleteServiceAccountIfExists(argThat(matches(serviceAccount)));
        var response = provisioner.deprovision(resource, iamServiceMock).join();

        verify(iamServiceMock).deleteServiceAccountIfExists(argThat(matches(serviceAccount)));
        assertThat(response.failed()).isTrue();
        assertThat(response.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
    }

    private ArgumentMatcher<GcpServiceAccount> matches(GcpServiceAccount serviceAccount) {
        return argument -> argument.getEmail().equals(serviceAccount.getEmail()) && argument.getName().equals(serviceAccount.getName());
    }
}
