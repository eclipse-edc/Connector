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
import org.eclipse.dataspaceconnector.spi.security.Vault;

/**
 * Implements and identity service backed by an EDC service registry.
 */
public class DistributedIdentityService implements IdentityService {
    private Vault vault;
    private OkHttpClient httpClient;

    public DistributedIdentityService(Vault vault, OkHttpClient httpClient) {
        this.vault = vault;
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
