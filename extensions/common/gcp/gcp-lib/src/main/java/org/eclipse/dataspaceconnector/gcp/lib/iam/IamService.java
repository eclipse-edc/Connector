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

package org.eclipse.dataspaceconnector.gcp.lib.iam;


import org.eclipse.dataspaceconnector.gcp.lib.common.ServiceAccountWrapper;
import org.eclipse.dataspaceconnector.gcp.lib.storage.GcsAccessToken;

/**
 * Wrapper around GCP IAM-API for decoupling.
 */
public interface IamService {
    ServiceAccountWrapper getOrCreateServiceAccount(String serviceAccountName);

    GcsAccessToken createAccessToken(ServiceAccountWrapper serviceAccount);

    void deleteServiceAccountIfExists(ServiceAccountWrapper serviceAccount);
}