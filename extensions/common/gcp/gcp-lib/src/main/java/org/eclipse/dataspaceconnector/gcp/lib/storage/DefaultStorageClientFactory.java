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

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * Factory for creating storage clients. Uses application-default credentials
 */
public class DefaultStorageClientFactory implements StorageClientFactory {
    private final String projectId;

    public DefaultStorageClientFactory(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public Storage create() {
        return StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    }
}