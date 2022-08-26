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

import org.eclipse.dataspaceconnector.gcp.lib.common.BucketWrapper;
import org.eclipse.dataspaceconnector.gcp.lib.common.ServiceAccountWrapper;

/**
 * Wrapper around Google Cloud Storage API for decoupling.
 */
public interface StorageService {

    BucketWrapper getOrCreateBucket(String bucketName, String location);

    void addRoleBinding(BucketWrapper bucket, ServiceAccountWrapper serviceAccount, String role);

    void addProviderPermissions(BucketWrapper bucket, ServiceAccountWrapper serviceAccount);

    boolean deleteBucket(String bucketName);

    boolean isEmpty(String bucketName);
}
