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

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.crypto.CryptoException;
import org.eclipse.edc.iam.did.crypto.JwtUtils;
import org.eclipse.edc.iam.did.crypto.key.KeyConverter;
import org.eclipse.edc.iam.did.spi.credentials.CredentialsVerifier;
import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.document.VerificationMethod;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.eclipse.edc.spi.agent.ParticipantAgentService.DEFAULT_IDENTITY_CLAIM_KEY;

/**
 * This Service is perfoming identity verification based on DIDs and the (discontinued) DWN spec. It is going to be replaced by the {@code IdentityAndTrustService}.
 *
 * @deprecated this implementation is deprecated and scheduled for removal. please use {@code IdentAndTrustService} instead!
 */
@Deprecated(forRemoval = true, since = "0.4.1")
public class DecentralizedIdentityService implements IdentityService {

    private final DidResolverRegistry resolverRegistry;
    private final CredentialsVerifier credentialsVerifier;
    private final Monitor monitor;
    private final Supplier<PrivateKey> privateKeySupplier;
    private final String issuer;
    private final Clock clock;

    public DecentralizedIdentityService(DidResolverRegistry resolverRegistry, CredentialsVerifier credentialsVerifier, Monitor monitor, Supplier<PrivateKey> privateKeySupplier, String issuer, Clock clock) {
        this.resolverRegistry = resolverRegistry;
        this.credentialsVerifier = credentialsVerifier;
        this.monitor = monitor;
        this.privateKeySupplier = privateKeySupplier;
        this.issuer = issuer;
        this.clock = clock;
    }

    @Override
    public Result<TokenRepresentation> obtainClientCredentials(TokenParameters parameters) {
        var claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(issuer)
                .audience(parameters.getAudience())
                .expirationTime(Date.from(clock.instant().plus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS)))
                .jwtID(UUID.randomUUID().toString())
                .build();
        try {
            var signer = getSigner(privateKeySupplier.get());
            //prefer ES256 if available, otherwise use the "next best"
            var algorithm = signer.supportedJWSAlgorithms().contains(JWSAlgorithm.ES256) ?
                    JWSAlgorithm.ES256 :
                    signer.supportedJWSAlgorithms().stream().min(Comparator.comparing(Algorithm::getRequirement))
                            .orElseThrow(() -> new CryptoException("No recommended JWS Algorithms for Private Key Signer " + signer.getClass()));
            var header = new JWSHeader(algorithm);

            var jwt = new SignedJWT(header, claimsSet);

            jwt.sign(signer);
            var token = jwt.serialize();
            return Result.success(TokenRepresentation.Builder.newInstance().token(token).build());
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }

    }

    @Override
    public Result<ClaimToken> verifyJwtToken(TokenRepresentation tokenRepresentation, VerificationContext context) {
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
            var publicKey = firstVerificationMethod(didResult.getContent());
            if (publicKey.isEmpty()) {
                return Result.failure("Public Key not found in DID Document!");
            }

            //convert the POJO into a usable PK-wrapper:
            var publicKeyJwk = publicKey.get().getPublicKeyJwk();
            var publicKeyWrapperResult = KeyConverter.toPublicKeyWrapper(publicKeyJwk, publicKey.get().getId());
            if (publicKeyWrapperResult.failed()) {
                monitor.debug("Failed to convert JWK into public key wrapper");
                return publicKeyWrapperResult.mapTo();
            }

            monitor.debug("Verifying JWT with public key...");
            var verified = JwtUtils.verify(jwt, publicKeyWrapperResult.getContent(), context.getAudience());
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
            var claimToken = ClaimToken.Builder.newInstance()
                    .claims(credentialsResult.getContent())
                    .claim(DEFAULT_IDENTITY_CLAIM_KEY, jwt.getJWTClaimsSet().getIssuer())
                    .build();

            return Result.success(claimToken);
        } catch (ParseException e) {
            monitor.severe("Error parsing JWT", e);
            return Result.failure("Error parsing JWT");
        }
    }

    private JWSSigner getSigner(PrivateKey privateKey) throws JOSEException {
        if (privateKey instanceof RSAPrivateKey) {
            return new RSASSASigner(privateKey);
        } else if (privateKey instanceof ECPrivateKey) {
            return new ECDSASigner(privateKey, Curve.P_256);
        }
        throw new EdcException("Only supports RSAPrivateKeys and ECPrivateKeys for now, but got " + privateKey.getClass());
    }

    @NotNull
    private Optional<VerificationMethod> firstVerificationMethod(DidDocument did) {
        return did.getVerificationMethod().stream()
                .filter(vm -> DidConstants.ALLOWED_VERIFICATION_TYPES.contains(vm.getType()))
                .findFirst();
    }
}
