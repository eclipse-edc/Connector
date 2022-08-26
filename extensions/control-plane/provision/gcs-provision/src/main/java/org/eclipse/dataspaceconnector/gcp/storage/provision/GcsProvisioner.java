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
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class GcsProvisioner implements Provisioner<GcsResourceDefinition, GcsProvisionedResource> {

    private Monitor monitor;
    private StorageService storageService;
    private IamService iamService;

    public GcsProvisioner(Monitor monitor, StorageService storageService, IamService iamService) {
        this.monitor = monitor;
        this.storageService = storageService;
        this.iamService = iamService;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof GcsResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof GcsProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(
            GcsResourceDefinition resourceDefinition, Policy policy) {
        String bucketName = resourceDefinition.getId();
        String bucketLocation = resourceDefinition.getLocation();

        monitor.debug("GCS Bucket request submitted: " + bucketName);

        String resourceName = resourceDefinition.getId() + "-bucket";
        try {
            BucketWrapper bucket = storageService.getOrCreateBucket(bucketName, bucketLocation);
            if (!storageService.isEmpty(bucketName)) {
                return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, String.format("Bucket: %s already exists and is not empty.", bucketName)));
            }
            ServiceAccountWrapper serviceAccount = createServiceAccount();
            GcsAccessToken token = createBucketAccessToken(bucket, serviceAccount);

            GcsProvisionedResource resource = getProvisionedResource(resourceDefinition, resourceName, serviceAccount);

            var response = ProvisionResponse.Builder.newInstance().resource(resource).secretToken(token).build();
            return CompletableFuture.completedFuture(StatusResult.success(response));
        } catch (GcpExtensionException e) {
            return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, e.toString()));
        }
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(
            GcsProvisionedResource provisionedResource, Policy policy) {
        try {
            iamService.deleteServiceAccountIfExists(
                    new ServiceAccountWrapper(provisionedResource.getServiceAccountEmail(),
                            provisionedResource.getServiceAccountName()));
        } catch (GcpExtensionException e) {
            return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR,
                    String.format("Deprovision failed with: %s", e.getMessage())));
        }
        return CompletableFuture.completedFuture(StatusResult.success(
                DeprovisionedResource.Builder.newInstance()
                        .provisionedResourceId(provisionedResource.getId()).build()));
    }

    private ServiceAccountWrapper createServiceAccount() {
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 26);
        String saName = "edc-" + uniqueId;
        return iamService.getOrCreateServiceAccount(saName);
    }

    private GcsAccessToken createBucketAccessToken(BucketWrapper bucket, ServiceAccountWrapper serviceAccount) {
        storageService.addProviderPermissions(bucket, serviceAccount);
        return iamService.createAccessToken(serviceAccount);
    }

    private GcsProvisionedResource getProvisionedResource(GcsResourceDefinition resourceDefinition, String resourceName, ServiceAccountWrapper serviceAccount) {
        return GcsProvisionedResource.Builder.newInstance()
                .id(resourceDefinition.getId())
                .resourceDefinitionId(resourceDefinition.getId())
                .location(resourceDefinition.getLocation())
                .storageClass(resourceDefinition.getStorageClass())
                .serviceAccountEmail(serviceAccount.getEmail())
                .serviceAccountName(serviceAccount.getName())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .resourceName(resourceName)
                .hasToken(true).build();
    }
}
