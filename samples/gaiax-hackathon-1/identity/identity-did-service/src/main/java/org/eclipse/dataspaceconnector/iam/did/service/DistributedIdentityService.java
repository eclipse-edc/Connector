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
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsResult;
import org.eclipse.dataspaceconnector.iam.did.spi.credentials.CredentialsVerifier;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Implements an identity service backed by an EDC service registry.
 */
public class DistributedIdentityService implements IdentityService {
    private String did;
    private CredentialsVerifier credentialsVerifier;
    private DidResolver didResolver;
    private DidPublicKeyResolver publicKeyResolver;
    private PrivateKeyResolver privateKeyResolver;
    private Monitor monitor;

    public DistributedIdentityService(String did,
                                      CredentialsVerifier credentialsVerifier,
                                      DidResolver didResolver,
                                      DidPublicKeyResolver publicKeyResolver,
                                      PrivateKeyResolver privateKeyResolver,
                                      Monitor monitor) {
        this.did = did;
        this.credentialsVerifier = credentialsVerifier;
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
            return TokenResult.Builder.newInstance().error("Error creating JWT").build();
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

            var credentialsResult = resolveCredentials(did);
            if (!credentialsResult.success()) {
                return new VerificationResult(credentialsResult.error());
            }

            var tokenBuilder = ClaimToken.Builder.newInstance();
            var claimToken = tokenBuilder.claims(credentialsResult.getValidatedCredentials()).build();
            return new VerificationResult(claimToken);
        } catch (ParseException e) {
            monitor.info("Error parsing JWT", e);
            return new VerificationResult("Error parsing JWT");
        }
    }

    private CredentialsResult resolveCredentials(Map<String, Object> didDocument) {
        // TODO HACKATHON-1 TASK 6B resolve the Hub URL from the Hub's did
        var hubBaseUrl = resolveHubUrl(didDocument);
        if (hubBaseUrl == null) {
            return new CredentialsResult("Hub URL not found in DID");
        }
        if (!hubBaseUrl.endsWith("/")) {
            hubBaseUrl += "/";
        }
        var publicKey = publicKeyResolver.resolvePublicKey(null);// TODO HACKATHON-1 this needs to resolve the public key of the Hub DID
        if (publicKey == null) {
            return new CredentialsResult("Unable to resolve DID public key");
        }
        return credentialsVerifier.verifyCredentials(hubBaseUrl, publicKey);
    }

    private boolean validateToken(SignedJWT jwt, Map<String, Object> didDocument) {
        try {
            // TODO HACKATHON-1 TASK 6B implement by verifying the token assertion against the public key contained in the DID, NOT the key from  publicKeyResolver.resolvePublicKey()
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
     * TODO HACKATHON-1 TASK 6B
     * The current implementation assumes the Hub endpoint is encoded in the client connector DID. We need to support the case where only the Hub did is referenced
     * in the current connector DID. This will involve resolving the Hub did and obtaining the endpoint address.
     */
    @SuppressWarnings({"ConditionCoveredByFurtherCondition", "rawtypes"})
    String resolveHubUrl(Map<String, Object> did) {
        var services = did.get("services");
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
            return (String) serviceMap.get("serviceEndpoint");

// Resolve ION/IdentityHub discrepancy

//            if (!(serviceEndpoint instanceof Map)) {
//                continue;
//            }
//            var locations = ((Map) serviceEndpoint).get("locations");
//            if (!(locations instanceof List)) {
//                continue;
//            }
//            var locationsList = (List<String>) locations;
//            if (((List<?>) locations).isEmpty()) {
//                continue;
//            }
//            return locationsList.get(0);
// End Resolve ION/IdentityHub discrepancy

        }
        return null;
    }

}
