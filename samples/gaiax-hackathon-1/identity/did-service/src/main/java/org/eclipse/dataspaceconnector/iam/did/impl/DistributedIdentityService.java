/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.iam.did.impl;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;

/**
 * Implements and identity service backed by an EDC service registry.
 */
public class DistributedIdentityService implements IdentityService {
    private OkHttpClient httpClient;

    public DistributedIdentityService(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public TokenResult obtainClientCredentials(String scope) {
        return null;
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        return null;
    }
}
