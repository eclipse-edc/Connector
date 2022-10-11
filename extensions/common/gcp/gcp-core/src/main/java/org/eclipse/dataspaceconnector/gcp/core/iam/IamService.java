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

package org.eclipse.dataspaceconnector.gcp.core.iam;


import org.eclipse.dataspaceconnector.gcp.core.common.GcpServiceAccount;
import org.eclipse.dataspaceconnector.gcp.core.storage.GcsAccessToken;

/**
 * Wrapper around GCP IAM-API for decoupling.
 */
public interface IamService {
    /**
     * Creates or returns the service account with the matching name and description.
     *
     * @param serviceAccountName        the name for the service account. Limited to 30 chars
     * @param serviceAccountDescription the unique description for the service account that is used to avoid reuse of service accounts
     * @return the {@link GcpServiceAccount} describing the service account
     */
    GcpServiceAccount getOrCreateServiceAccount(String serviceAccountName, String serviceAccountDescription);

    /**
     * Creates a temporary valid OAunth2.0 access token for the service account
     *
     * @param serviceAccount The service account the token should be created for
     * @return {@link GcsAccessToken}
     */
    GcsAccessToken createAccessToken(GcpServiceAccount serviceAccount);

    /**
     * Delete the specified service account if it exists.
     * Do nothing in case it doesn't exist (anymore)
     *
     * @param serviceAccount The service account that should be deleted
     */
    void deleteServiceAccountIfExists(GcpServiceAccount serviceAccount);
}