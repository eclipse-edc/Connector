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

import com.google.cloud.storage.Blob;

import java.util.Iterator;

import org.eclipse.edc.gcp.storage.GcsStoreSchema;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.StatusChecker;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.gcp.storage.StorageServiceImpl;
import org.eclipse.edc.spi.EdcException;

import java.util.List;

import static org.eclipse.edc.connector.transfer.spi.types.TransferProcess.Type.PROVIDER;

import static java.lang.String.format;

public class GcsProvisionerStatusChecker implements StatusChecker {
    private StorageServiceImpl storageService;

    public GcsProvisionerStatusChecker(StorageServiceImpl storageService) {
        this.storageService = storageService;
    }

    @Override
    public boolean isComplete(TransferProcess transferProcess, List<ProvisionedResource> resources) {
        if (transferProcess.getType() == PROVIDER) {
            // TODO check if PROVIDER process implementation is needed
        }

        var bucketName = transferProcess.getDataRequest().getDataDestination().getProperty(GcsStoreSchema.BUCKET_NAME);
        if (resources != null && !resources.isEmpty()) {
            for (var resource : resources) {
                if (resource instanceof GcsProvisionedResource) {
                    return checkBucketTransferComplete(bucketName);
                }
            }
        } else {
            return checkBucketTransferComplete(bucketName);
        }
        throw new EdcException(format("Cannot determine completion: no resource associated with transfer process %s.", transferProcess));
    }

    private boolean checkBucketTransferComplete(String bucketName) {
        String testBlobName = bucketName+".complete";
        var blobs = storageService.list(bucketName);
        // TODO rewrite with stream
        Iterator<Blob> blobIterator = blobs.iterateAll().iterator();
        while (blobIterator.hasNext()) {
            Blob blob = blobIterator.next();
            if (blob.getName().equals(testBlobName)) {
                return true;
            }
        }
        return false;
    }
}
