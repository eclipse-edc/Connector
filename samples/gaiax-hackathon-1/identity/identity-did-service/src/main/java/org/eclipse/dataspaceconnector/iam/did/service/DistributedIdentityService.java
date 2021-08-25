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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import org.eclipse.dataspaceconnector.spi.security.PrivateKeyResolver;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Implements and identity service backed by an EDC service registry.
 */
public class DistributedIdentityService implements IdentityService {
    private String did;
    private IdentityHubClient hubClient;
    private DidResolver didResolver;
    private DidPublicKeyResolver publicKeyResolver;
    private PrivateKeyResolver privateKeyResolver;
    private Monitor monitor;

    public DistributedIdentityService(String did,
                                      IdentityHubClient hubClient,
                                      DidResolver didResolver,
                                      DidPublicKeyResolver publicKeyResolver,
                                      PrivateKeyResolver privateKeyResolver,
                                      Monitor monitor) {
        this.did = did;
        this.hubClient = hubClient;
        this.didResolver = didResolver;
        this.publicKeyResolver = publicKeyResolver;
        this.privateKeyResolver = privateKeyResolver;
        this.monitor = monitor;
    }

    @Override
    public TokenResult obtainClientCredentials(String scope) {
        var privateKey = privateKeyResolver.resolvePrivateKey(null);
        if (privateKey == null) {
            return TokenResult.Builder.newInstance().error("Private key not found").build();
        }
        try {
            var signer = new RSASSASigner(privateKey);

            var expiration = new Date().getTime() + TimeUnit.MINUTES.toMillis(10);
            var claimsSet = new JWTClaimsSet.Builder()
                    .subject(scope)
                    .issuer(did)
                    .expirationTime(new Date(expiration))
                    .build();

            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("primary").build(), claimsSet);
            jwt.sign(signer);

            var token = jwt.serialize();
            return TokenResult.Builder.newInstance().token(token).expiresIn(expiration).build();
        } catch (JOSEException e) {
            monitor.severe("Error creating JWT", e);
            return TokenResult.Builder.newInstance().error("error creating JWT").build();
        }
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        try {
            var jwt = SignedJWT.parse(token);

            var did = didResolver.resolveDid(jwt.getJWTClaimsSet().getIssuer());
            if (!validateToken(jwt, did)) {
                return new VerificationResult("Invalid token");
            }
            var credentials = resolveCredentials(did);
            var tokenBuilder = ClaimToken.Builder.newInstance();
            var claimToken = tokenBuilder.claims(credentials).build();
            return new VerificationResult(claimToken);
        } catch (ParseException e) {
            monitor.info("Error parsing JWT", e);
            return new VerificationResult("Error parsing JWT");
        }
    }

    private Map<String, String> resolveCredentials(Map<String, Object> didDocument) {
        var query = ObjectQuery.Builder.newInstance().context("GAIA-X").type("RegistrationCredentials").build();
        var queryRequest = ObjectQueryRequest.Builder.newInstance().query(query).iss("123").aud("aud").sub("sub").build();

        // TODO HACKATHON-1 resolve the Hub URL from the Hub's did
        var hubBaseUrl = resolveHubUrl(didDocument);
        if (hubBaseUrl == null) {
            monitor.info("Hub URL not found in DID");
            return Collections.emptyMap();
        }
        if (!hubBaseUrl.endsWith("/")) {
            hubBaseUrl += "/";
        }
        var publicKey = publicKeyResolver.resolvePublicKey(null);// TODO HACKATHON-1 this needs to resolve the public key of the Hub DID
        if (publicKey == null) {
            monitor.info("Unable to resolve DID public key");
            return Collections.emptyMap();
        }
        var credentials = hubClient.queryCredentials(queryRequest, hubBaseUrl, publicKey);
        if (credentials.isError()) {
            monitor.info(format("Error resolving credentials not found for: %s", credentials.getError()));
            return Collections.emptyMap();
        }

        // only support String credentials; filter out others
        var map = new HashMap<String, String>();
        credentials.getResponse().entrySet().stream().filter(entry -> entry.getValue() instanceof String).forEach(entry -> map.put(entry.getKey(), (String) entry.getValue()));
        return map;
    }

    private boolean validateToken(SignedJWT jwt, Map<String, Object> didDocument) {
        try {
            // TODO TODO HACKATHON-1 implement by verifying the token assertion against the public key contained in the DID, NOT the key from  publicKeyResolver.resolvePublicKey()
            // This will involve loading the Key from didDocument
            var publicKey = (RSAPublicKey) publicKeyResolver.resolvePublicKey("");   // this needs to be replaced
            if (publicKey == null) {
                monitor.info("Unable to resolve DID public key");
                return false;
            }
            var verifier = new RSASSAVerifier(publicKey);
            return jwt.verify(verifier);
        } catch (JOSEException e) {
            monitor.info("Error verifying client token", e);
            return false;
        }
    }

    /**
     * Returns the location of the Hub endpoint.
     *
     * TODO HACKATHON-1
     * The current implementation assumes the Hub endpoint is encoded in the client connector DID. We need to support the case where only the Hub did is referenced
     * in the current connector DID. This will involve resolving the Hub did and obtaining the endpoint address.
     */
    @SuppressWarnings({"ConditionCoveredByFurtherCondition", "rawtypes", "unchecked"})
    String resolveHubUrl(Map<String, Object> did) {
        var document = did.get("document");
        if (document == null || !(document instanceof Map)) {
            return null;
        }
        var services = ((Map) document).get("service");
        if (services == null || !(services instanceof List)) {
            return null;
        }
        var serviceList = (List<?>) services;
        for (Object service : serviceList) {
            if (!(service instanceof Map)) {
                continue;
            }
            var serviceMap = (Map) service;
            var type = serviceMap.get("type");
            if (!"IdentityHub".equals(type)) {
                continue;
            }
            var serviceEndpoint = serviceMap.get("serviceEndpoint");
            if (!(serviceEndpoint instanceof Map)) {
                continue;
            }
            var locations = ((Map) serviceEndpoint).get("locations");
            if (!(locations instanceof List)) {
                continue;
            }
            var locationsList = (List<String>) locations;
            if (((List<?>) locations).isEmpty()) {
                continue;
            }
            return locationsList.get(0);

        }
        return null;
    }

}
