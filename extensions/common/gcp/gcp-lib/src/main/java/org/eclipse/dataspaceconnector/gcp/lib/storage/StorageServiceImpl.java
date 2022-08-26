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

package org.eclipse.dataspaceconnector.gcp.lib.storage;

import com.google.cloud.Binding;
import com.google.cloud.Policy;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import org.eclipse.dataspaceconnector.gcp.lib.common.BucketWrapper;
import org.eclipse.dataspaceconnector.gcp.lib.common.GcpExtensionException;
import org.eclipse.dataspaceconnector.gcp.lib.common.ServiceAccountWrapper;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.ArrayList;
import java.util.List;

public class StorageServiceImpl implements StorageService {

    private final Storage storageClient;
    private Monitor monitor;


    public StorageServiceImpl(StorageClientFactory storageClientFactory, Monitor monitor) {
        this.storageClient = storageClientFactory.create();
        this.monitor = monitor;
    }

    @Override
    public BucketWrapper getOrCreateBucket(String bucketName, String location) {
        Bucket bucket = storageClient.get(bucketName);
        if (bucket == null) {
            monitor.info("Creating new bucket " + bucketName);
            bucket = storageClient.create(BucketInfo.newBuilder(bucketName).setLocation(location).build());
        } else if (!bucket.getLocation().equals(location)) {
            throw new GcpExtensionException("Bucket " + bucketName + " aleady exists but in wrong location");
        }

        return new BucketWrapper(bucket);
    }

    @Override
    public void addRoleBinding(BucketWrapper bucket, ServiceAccountWrapper serviceAccount, String role) {
        var originalPolicy = storageClient.getIamPolicy(bucket.getName());
        List<Binding> bindings = new ArrayList<>(originalPolicy.getBindingsList());

        String serviceAccountMemberName = "serviceAccount:" + serviceAccount.getEmail();
        Binding roleBinding = Binding.newBuilder().addMembers(serviceAccountMemberName).setRole(role).build();
        if (!bindings.contains(roleBinding)) {
            bindings.add(roleBinding);
        }

        Policy updatedPolicy = originalPolicy.toBuilder()
                .setBindings(bindings)
                .setVersion(3)
                .build();
        storageClient.setIamPolicy(bucket.getName(), updatedPolicy);
        monitor.info("Added role binding to bucket " + bucket.getName() + "\n" + roleBinding);
    }

    @Override
    public void addProviderPermissions(BucketWrapper bucket, ServiceAccountWrapper serviceAccount) {
        addRoleBinding(bucket, serviceAccount, "roles/storage.objectCreator");
    }

    @Override
    public boolean deleteBucket(String bucketName) {
        boolean deleted = storageClient.delete(bucketName);
        if (deleted) {
            monitor.info("The bucket " + bucketName + "was deleted");
            return true;
        } else {
            monitor.info("Bucket " + bucketName + "not found");
            return false;
        }
    }

    @Override
    public boolean isEmpty(String bucket) {
        var blobs = storageClient.list(bucket, Storage.BlobListOption.pageSize(1)).getValues();
        List<Blob> blobList = new ArrayList<>();
        blobs.forEach(blob -> blobList.add(blob));
        return blobList.isEmpty();
    }
}
