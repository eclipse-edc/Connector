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

import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubClient;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQuery;
import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.ObjectQueryRequest;
import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidPublicKeyResolver;
import org.eclipse.dataspaceconnector.iam.did.spi.resolver.DidResolver;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * Implements and identity service backed by an EDC service registry.
 */
public class DistributedIdentityService implements IdentityService {
    private IdentityHubClient hubClient;
    private DidResolver didResolver;
    private DidPublicKeyResolver publicKeyResolver;
    private Monitor monitor;

    public DistributedIdentityService(IdentityHubClient hubClient, DidResolver didResolver, DidPublicKeyResolver publicKeyResolver, Monitor monitor) {
        this.hubClient = hubClient;
        this.didResolver = didResolver;
        this.publicKeyResolver = publicKeyResolver;
        this.monitor = monitor;
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
        var query = ObjectQuery.Builder.newInstance().context("GAIA-X").type("RegistrationCredentials").build();
        var queryRequest = ObjectQueryRequest.Builder.newInstance().query(query).iss("123").aud("aud").sub("sub").build();

        // TODO pull the hub url from the DID document
        var hubBaseUrl = "http://localhost:8181/api/identity-hub/";
        var publicKey = publicKeyResolver.resolvePublicKey(null);
        var credentials = hubClient.queryCredentials(queryRequest, hubBaseUrl, publicKey);
        if (credentials.isError()) {
            monitor.info(format("Error resolving credentials not found for: %s", credentials.getError()));
            return Collections.emptyMap();
        }

        var map = new HashMap<String, String>();
        credentials.getResponse().entrySet().stream().filter(entry -> entry.getValue() instanceof String).forEach(entry -> map.put(entry.getKey(), (String) entry.getValue()));
        return map;
    }

    private boolean validateToken(String token, Map<String, Object> did) {
        // TODO implement by verifying the token assertion against the public key contained in the DID
        return true;
    }


}
