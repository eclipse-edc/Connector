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

import org.eclipse.dataspaceconnector.gcp.core.common.BucketWrapper;
import org.eclipse.dataspaceconnector.gcp.core.common.GcpException;
import org.eclipse.dataspaceconnector.gcp.core.common.ServiceAccountWrapper;
import org.eclipse.dataspaceconnector.gcp.core.iam.IamService;
import org.eclipse.dataspaceconnector.gcp.core.storage.GcsAccessToken;
import org.eclipse.dataspaceconnector.gcp.core.storage.StorageService;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.response.ResponseStatus;
import org.eclipse.dataspaceconnector.spi.response.StatusResult;
import org.eclipse.dataspaceconnector.spi.transfer.provision.Provisioner;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ResourceDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class GcsProvisioner implements Provisioner<GcsResourceDefinition, GcsProvisionedResource> {

    private final Monitor monitor;
    private final StorageService storageService;
    private final IamService iamService;

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
        var bucketName = resourceDefinition.getId();
        var bucketLocation = resourceDefinition.getLocation();

        monitor.debug("GCS Bucket request submitted: " + bucketName);

        var resourceName = resourceDefinition.getId() + "-bucket";
        var processId = resourceDefinition.getTransferProcessId();
        try {
            var bucket = storageService.getOrCreateEmptyBucket(bucketName, bucketLocation);
            if (!storageService.isEmpty(bucketName)) {
                return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, String.format("Bucket: %s already exists and is not empty.", bucketName)));
            }
            var serviceAccount = createServiceAccount(processId, bucketName);
            var token = createBucketAccessToken(bucket, serviceAccount);

            var resource = getProvisionedResource(resourceDefinition, resourceName, serviceAccount);

            var response = ProvisionResponse.Builder.newInstance().resource(resource).secretToken(token).build();
            return CompletableFuture.completedFuture(StatusResult.success(response));
        } catch (GcpException e) {
            return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, e.toString()));
        }
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(
            GcsProvisionedResource provisionedResource, Policy policy) {
        try {
            iamService.deleteServiceAccountIfExists(
                    new ServiceAccountWrapper(provisionedResource.getServiceAccountEmail(),
                            provisionedResource.getServiceAccountName(), ""));
        } catch (GcpException e) {
            return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR,
                    String.format("Deprovision failed with: %s", e.getMessage())));
        }
        return CompletableFuture.completedFuture(StatusResult.success(
                DeprovisionedResource.Builder.newInstance()
                        .provisionedResourceId(provisionedResource.getId()).build()));
    }

    private ServiceAccountWrapper createServiceAccount(String processId, String buckedName) {
        var serviceAccountName = sanitizeServiceAccountName(processId);
        var uniqueServiceAccountDescription = generateUniqueServiceAccountDescription(processId, buckedName);
        return iamService.getOrCreateServiceAccount(serviceAccountName, uniqueServiceAccountDescription);
    }

    @NotNull
    private String sanitizeServiceAccountName(String processId) {
        // service account ID must be between 6 and 30 characters and can contain lowercase alphanumeric characters and dashes
        String processIdWithoutConstantChars = processId.replace("-", "");
        var maxAllowedSubstringLength = Math.min(26, processIdWithoutConstantChars.length());
        var uniqueId = processIdWithoutConstantChars.substring(0, maxAllowedSubstringLength);
        return "edc-" + uniqueId;
    }

    @NotNull
    private String generateUniqueServiceAccountDescription(String transferProcessId, String bucketName) {
        return String.format("transferProcess:%s\nbucket:%s", transferProcessId, bucketName);
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
