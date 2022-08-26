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

package org.eclipse.dataspaceconnector.gcp.storage.provision;

import org.eclipse.dataspaceconnector.gcp.lib.common.BucketWrapper;
import org.eclipse.dataspaceconnector.gcp.lib.common.GcpExtensionException;
import org.eclipse.dataspaceconnector.gcp.lib.common.ServiceAccountWrapper;
import org.eclipse.dataspaceconnector.gcp.lib.iam.IamService;
import org.eclipse.dataspaceconnector.gcp.lib.storage.GcsAccessToken;
import org.eclipse.dataspaceconnector.gcp.lib.storage.StorageService;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import static org.assertj.core.api.Assertions.assertThat;
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
        provisioner = new GcsProvisioner(mock(Monitor.class), storageServiceMock, iamServiceMock);
    }

    @Test
    void canProvisionGcsResource() {
        var gcsResource = GcsResourceDefinition.Builder.newInstance()
                .id("TEST").location("TEST").storageClass("TEST")
                .build();
        assertThat(provisioner.canProvision(gcsResource)).isTrue();
    }

    @Test
    void provisionSuccess() {
        GcsResourceDefinition resourceDefinition = createResourceDefinition("id", "location",
                "storage-class", "transfer-id");
        String bucketName = resourceDefinition.getId();
        String bucketLocation = resourceDefinition.getLocation();

        BucketWrapper bucket = new BucketWrapper(bucketName);
        ServiceAccountWrapper serviceAccount = new ServiceAccountWrapper("test-sa", "sa-name");
        GcsAccessToken token = new GcsAccessToken("token", 123);

        when(storageServiceMock.getOrCreateBucket(bucketName, bucketLocation)).thenReturn(bucket);
        when(storageServiceMock.isEmpty(bucketName)).thenReturn(true);
        when(iamServiceMock.getOrCreateServiceAccount(anyString())).thenReturn(serviceAccount);
        doNothing().when(storageServiceMock).addProviderPermissions(bucket, serviceAccount);
        when(iamServiceMock.createAccessToken(serviceAccount)).thenReturn(token);

        var response = provisioner.provision(resourceDefinition, testPolicy).join().getContent();

        assertThat(response.getResource()).isInstanceOfSatisfying(GcsProvisionedResource.class, resource -> {
            assertThat(resource.getId()).isEqualTo("id");
            assertThat(resource.getTransferProcessId()).isEqualTo("transfer-id");
            assertThat(resource.getLocation()).isEqualTo("location");
            assertThat(resource.getStorageClass()).isEqualTo("storage-class");
        });
        assertThat(response.getSecretToken()).isInstanceOfSatisfying(GcsAccessToken.class, secretToken -> {
            assertThat(secretToken.getToken()).isEqualTo("token");
        });

        verify(storageServiceMock).getOrCreateBucket(bucketName, bucketLocation);
        verify(storageServiceMock).addProviderPermissions(bucket, serviceAccount);
        verify(iamServiceMock).createAccessToken(serviceAccount);
    }

    @Test
    void provisionFailsIfBucketNotEmpty() {
        GcsResourceDefinition resourceDefinition = createResourceDefinition();
        String bucketName = resourceDefinition.getId();
        String bucketLocation = resourceDefinition.getLocation();

        when(storageServiceMock.getOrCreateBucket(bucketName, bucketLocation)).thenReturn(new BucketWrapper(bucketName));
        when(storageServiceMock.isEmpty(bucketName)).thenReturn(false);

        var response = provisioner.provision(resourceDefinition, testPolicy).join();

        assertThat(response.failed()).isTrue();
        assertThat(response.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);

        verify(storageServiceMock).getOrCreateBucket(bucketName, bucketLocation);
        verify(storageServiceMock, times(0)).addProviderPermissions(any(), any());
        verify(iamServiceMock, times(0)).createAccessToken(any());
    }


    @Test
    void provisionFailsBecauseOfApiError() {
        GcsResourceDefinition resourceDefinition = createResourceDefinition();
        String bucketName = resourceDefinition.getId();
        String bucketLocation = resourceDefinition.getLocation();

        doThrow(new GcpExtensionException("some error")).when(storageServiceMock).getOrCreateBucket(bucketName, bucketLocation);

        var response = provisioner.provision(resourceDefinition, testPolicy).join();
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
        String email = "test-email";
        String name = "test-name";
        String id = "test-id";
        ServiceAccountWrapper serviceAccount = new ServiceAccountWrapper(email, name);

        doNothing().when(iamServiceMock).deleteServiceAccountIfExists(argThat(matches(serviceAccount)));
        GcsProvisionedResource resource = createGcsProvisionedResource(email, name, id);

        var response = provisioner.deprovision(resource, testPolicy).join().getContent();
        verify(iamServiceMock).deleteServiceAccountIfExists(argThat(matches(serviceAccount)));
        assertThat(response.getProvisionedResourceId()).isEqualTo(id);
    }

    private GcsProvisionedResource createGcsProvisionedResource(String serviceAccountEmail, String serviceAccountName, String id) {
        return GcsProvisionedResource.Builder.newInstance().resourceName("name")
                .id(id)
                .resourceDefinitionId(id)
                .bucketName("bucket")
                .location("location")
                .storageClass("standard")
                .transferProcessId("transfer-id")
                .serviceAccountName(serviceAccountName)
                .serviceAccountEmail(serviceAccountEmail)
                .build();
    }

    private GcsResourceDefinition createResourceDefinition() {
        return createResourceDefinition("id", "location",
                "storage-class", "transfer-id");
    }

    private GcsResourceDefinition createResourceDefinition(String id, String location, String storageClass, String transferProcessId) {
        return GcsResourceDefinition.Builder.newInstance().id(id)
                .location(location).storageClass(storageClass)
                .transferProcessId(transferProcessId).build();
    }

    @Test
    void deprovisionFails() {
        String email = "test-email";
        String name = "test-name";
        String id = "test-id";
        ServiceAccountWrapper serviceAccount = new ServiceAccountWrapper(email, name);
        GcsProvisionedResource resource = createGcsProvisionedResource(email, name, id);

        doThrow(new GcpExtensionException("some error"))
                .when(iamServiceMock)
                .deleteServiceAccountIfExists(argThat(matches(serviceAccount)));
        var response = provisioner.deprovision(resource, testPolicy).join();

        verify(iamServiceMock).deleteServiceAccountIfExists(argThat(matches(serviceAccount)));
        assertThat(response.failed()).isTrue();
        assertThat(response.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
    }

    private ArgumentMatcher<ServiceAccountWrapper> matches(ServiceAccountWrapper serviceAccount) {
        return argument -> argument.getEmail().equals(serviceAccount.getEmail()) && argument.getName().equals(serviceAccount.getName());
    }
}
