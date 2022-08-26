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

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.util.Date;

/**
 * Factory for creating storage clients. Uses OAuth2.0 token for authentication
 */
public class StorageClientFromTokenFactory implements StorageClientFactory {
    private final String projectId;
    private final String accessToken;
    private final Date expirationTime;

    public StorageClientFromTokenFactory(String projectId, String accessToken, Date expirationTime) {
        this.projectId = projectId;
        this.accessToken = accessToken;
        this.expirationTime = expirationTime;
    }

    @Override
    public Storage create() {
        Credentials credentials = GoogleCredentials.create(new AccessToken(accessToken, expirationTime));
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
