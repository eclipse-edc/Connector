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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implements and identity service backed by an EDC service registry.
 */
public class DistributedIdentityService implements IdentityService {
    private String resolverUrl;
    private Vault vault;
    private OkHttpClient httpClient;
    private TypeManager typeManager;

    public DistributedIdentityService(String resolverUrl, Vault vault, OkHttpClient httpClient, TypeManager typeManager) {
        this.resolverUrl = resolverUrl;
        this.vault = vault;
        this.httpClient = httpClient;
        this.typeManager = typeManager;
    }

    @Override
    public TokenResult obtainClientCredentials(String scope) {
        return null;
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {

        var did = resolveDid(token);
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

    /**
     * Resolves a DID against an external resolver service.
     */
    private LinkedHashMap<String, Object> resolveDid(String token) {
        var request = new Request.Builder().url(resolverUrl + token).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            var responseBody = response.body();
            if (responseBody == null) {
                throw new EdcException("Null response returned from DID resolver service");
            }
            //noinspection unchecked
            return (LinkedHashMap<String, Object>) typeManager.readValue(responseBody.string(), Map.class);
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }
}
