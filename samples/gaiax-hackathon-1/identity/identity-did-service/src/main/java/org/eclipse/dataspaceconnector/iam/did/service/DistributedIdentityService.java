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
package org.eclipse.dataspaceconnector.iam.did.service;

import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidResolver;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.security.Vault;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements and identity service backed by an EDC service registry.
 */
public class DistributedIdentityService implements IdentityService {
    private DidResolver didResolver;
    private Vault vault;

    public DistributedIdentityService(DidResolver didResolver,  Vault vault) {
        this.didResolver = didResolver;
        this.vault = vault;
    }

    @Override
    public TokenResult obtainClientCredentials(String scope) {
        return null;
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        var did = didResolver.resolveDid(token);
        if (!validateToken(token, did)) {
            return new VerificationResult("Invalid token");
        }
        var credentials = resolveCredentials(did);

        var tokenBuilder = ClaimToken.Builder.newInstance();
        var claimToken = tokenBuilder.claims(credentials).build();
        return new VerificationResult(claimToken);
    }

    private Map<String, String> resolveCredentials(Map<String, Object> did) {
        // TODO implement by resolving credentials from the listed identity hub
        return new HashMap<>();
    }

    private boolean validateToken(String token, Map<String, Object> did) {
        // TODO implement by verifying the token assertion against the public key contained in the DID
        return true;
    }

}
