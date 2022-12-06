/*
 *  Copyright (c) 2021 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *       Microsoft Corporation - Use IDS Webhook address for JWT audience claim
 *
 */

package org.eclipse.edc.iam.did.service;

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.crypto.JwtUtils;
import org.eclipse.edc.iam.did.crypto.key.KeyConverter;
import org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.time.Clock;
import java.util.Optional;

public class DecentralizedIdentityService implements IdentityService {
    private final DidResolverRegistry resolverRegistry;
    private final CredentialsVerifier credentialsVerifier;
    private final Monitor monitor;
    private final PrivateKeyWrapper privateKey;
    private final String issuer;
    private final Clock clock;

    public DecentralizedIdentityService(DidResolverRegistry resolverRegistry, CredentialsVerifier credentialsVerifier, Monitor monitor, PrivateKeyWrapper privateKey, String issuer, Clock clock) {
        this.resolverRegistry = resolverRegistry;
        this.credentialsVerifier = credentialsVerifier;
        this.monitor = monitor;
        this.privateKey = privateKey;
        this.issuer = issuer;
        this.clock = clock;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var jwt = JwtUtils.create(privateKey, issuer, issuer, parameters.getAudience(), clock);
        var token = jwt.serialize();
        return Result.success(TokenRepresentation.Builder.newInstance().token(token).build());
    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, String audience) {
        try {
            var jwt = SignedJWT.parse(tokenRepresentation.getToken());
            monitor.debug("Starting verification...");

            monitor.debug("Resolving other party's DID Document");
            var didResult = resolverRegistry.resolve(jwt.getJWTClaimsSet().getIssuer());
            if (didResult.failed()) {
                return Result.failure("Unable to resolve DID: " + String.join(", ", didResult.getFailureMessages()));
            }
            monitor.debug("Extracting public key");

            // this will return the _first_ public key entry
            var publicKey = getPublicKey(didResult.getContent());
            if (publicKey.isEmpty()) {
                return Result.failure("Public Key not found in DID Document!");
            }

            //convert the POJO into a usable PK-wrapper:
            var publicKeyJwk = publicKey.get().getPublicKeyJwk();
            var publicKeyWrapper = KeyConverter.toPublicKeyWrapper(publicKeyJwk, publicKey.get().getId());

            monitor.debug("Verifying JWT with public key...");
            var verified = JwtUtils.verify(jwt, publicKeyWrapper, audience);
            if (verified.failed()) {
                monitor.debug(() -> "Failure in token verification: " + verified.getFailureDetail());
                return Result.failure("Token could not be verified!");
            }

            monitor.debug("verification successful! Fetching data from IdentityHub");
            var credentialsResult = credentialsVerifier.getVerifiedCredentials(didResult.getContent());
            if (credentialsResult.failed()) {
                monitor.debug(() -> "Failed to retrieve verified credentials: " + credentialsResult.getFailureDetail());
                return Result.failure("Failed to get verifiable credentials: " + credentialsResult.getFailureDetail());
            }

            monitor.debug("Building ClaimToken");
            var tokenBuilder = ClaimToken.Builder.newInstance();
            var claimToken = tokenBuilder.claims(credentialsResult.getContent()).build();

            return Result.success(claimToken);
        } catch (ParseException e) {
            monitor.severe("Error parsing JWT", e);
            return Result.failure("Error parsing JWT");
        }
    }

    @NotNull
    private Optional<VerificationMethod> getPublicKey(DidDocument did) {
        return did.getVerificationMethod().stream().filter(vm -> DidConstants.ALLOWED_VERIFICATION_TYPES.contains(vm.getType())).findFirst();
    }
}
