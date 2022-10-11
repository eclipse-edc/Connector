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

package org.eclipse.dataspaceconnector.gcp.core.storage;

import com.google.cloud.Binding;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import org.eclipse.dataspaceconnector.gcp.core.common.GcpException;
import org.eclipse.dataspaceconnector.gcp.core.common.GcpServiceAccount;
import org.eclipse.dataspaceconnector.gcp.core.common.GcsBucket;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.ArrayList;
import java.util.Optional;

public class StorageServiceImpl implements StorageService {

    private final Storage storageClient;
    private final Monitor monitor;


    public StorageServiceImpl(Storage storageClient, Monitor monitor) {
        this.storageClient = storageClient;
        this.monitor = monitor;
    }

    @Override
    public GcsBucket getOrCreateEmptyBucket(String bucketName, String location) {
        var bucket = Optional.ofNullable(storageClient.get(bucketName))
                .orElseGet(() -> {
                    monitor.debug("Creating new bucket " + bucketName);
                    return storageClient.create(BucketInfo.newBuilder(bucketName).setLocation(location).build());
                });
        if (!bucket.getLocation().equals(location)) {
            throw new GcpException("Bucket " + bucketName + " already exists but in wrong location");
        }

        return new GcsBucket(bucket.getName());
    }

    @Override
    public void addRoleBinding(GcsBucket bucket, GcpServiceAccount serviceAccount, String role) {
        var originalPolicy = storageClient.getIamPolicy(bucket.getName());
        var bindings = new ArrayList<>(originalPolicy.getBindingsList());

        var serviceAccountMemberName = "serviceAccount:" + serviceAccount.getEmail();
        var roleBinding = Binding.newBuilder().addMembers(serviceAccountMemberName).setRole(role).build();
        if (!bindings.contains(roleBinding)) {
            bindings.add(roleBinding);
        }

        var updatedPolicy = originalPolicy.toBuilder()
                .setBindings(bindings)
                .setVersion(3)
                .build();
        storageClient.setIamPolicy(bucket.getName(), updatedPolicy);
        monitor.debug("Added role binding " + roleBinding + " binding to bucket " + bucket.getName());
    }

    @Override
    public void addProviderPermissions(GcsBucket bucket, GcpServiceAccount serviceAccount) {
        addRoleBinding(bucket, serviceAccount, "roles/storage.objectCreator");
    }

    @Override
    public boolean deleteBucket(String bucketName) {
        var deleted = storageClient.delete(bucketName);
        if (deleted) {
            monitor.debug("The bucket " + bucketName + "was deleted");
            return true;
        } else {
            monitor.debug("Bucket " + bucketName + "not found");
            return false;
        }
    }

    @Override
    public boolean isEmpty(String bucket) {
        var blobs = storageClient.list(bucket, Storage.BlobListOption.pageSize(1)).getValues();
        var blobList = new ArrayList<>();
        blobs.forEach(blobList::add);
        return blobList.isEmpty();
    }
}
