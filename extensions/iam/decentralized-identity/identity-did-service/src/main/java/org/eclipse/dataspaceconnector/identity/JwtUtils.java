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
 *
 */

package org.eclipse.dataspaceconnector.identity;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import org.eclipse.dataspaceconnector.iam.did.crypto.CryptoException;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;
import org.eclipse.dataspaceconnector.spi.result.Result;

import java.text.ParseException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Convenience/helper class to generate and verify Signed JSON Web Tokens (JWTs) for communicating between connector instances.
 */
class JwtUtils {
    // RFC 7519 Registered (standard) claims
    private static final String ISSUER_CLAIM = "iss";
    private static final String SUBJECT_CLAIM = "sub";
    private static final String EXPIRATION_TIME_CLAIM = "exp";

    /**
     * Creates a signed JWT {@link SignedJWT} that contains a set of claims and an issuer. Although all private key types are possible, in the context of Distributed Identity
     * using an Elliptic Curve key ({@code P-256}) is advisable.
     *
     * @param privateKey A Private Key represented as {@link PrivateKeyWrapper}.
     * @param issuer     the value of the token issuer claim.
     * @param subject    the value of the token subject claim. For Distributed Identity, this value is identical to the issuer claim.
     * @param audience   the value of the token audience claim, e.g. the IDS Webhook address.
     * @param clock      clock used to get current time.
     * @return a {@code SignedJWT} that is signed with the private key and contains all claims listed.
     */

    static SignedJWT create(PrivateKeyWrapper privateKey, String issuer, String subject, String audience, Clock clock) {
        var claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(subject)
                .audience(audience)
                .expirationTime(Date.from(clock.instant().plus(10, ChronoUnit.MINUTES)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        var signer = privateKey.signer();
        //prefer ES256 if available, otherwise use the "next best"
        var algorithm = signer.supportedJWSAlgorithms().contains(JWSAlgorithm.ES256) ?
                JWSAlgorithm.ES256 :
                signer.supportedJWSAlgorithms().stream().min(Comparator.comparing(Algorithm::getRequirement))
                        .orElseThrow(() -> new CryptoException("No recommended JWS Algorithms for Private Key Signer " + signer.getClass()));
        var header = new JWSHeader(algorithm);

        var vc = new SignedJWT(header, claimsSet);
        try {
            vc.sign(signer);
            return vc;
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Verifies a VerifiableCredential using the issuer's public key
     *
     * @param jwt       a {@link SignedJWT} that was sent by the claiming party.
     * @param publicKey The claiming party's public key, passed as a {@link PublicKeyWrapper}
     * @param audience  The intended audience
     * @return true if verified, false otherwise
     */
    static Result<Void> verify(SignedJWT jwt, PublicKeyWrapper publicKey, String audience) {
        // verify JWT signature
        try {
            var verified = jwt.verify(publicKey.verifier());
            if (!verified) {
                return Result.failure("Invalid signature");
            }
        } catch (JOSEException e) {
            return Result.failure("Unable to verify JWT token. " + e.getMessage()); // e.g. the JWS algorithm is not supported
        }

        JWTClaimsSet jwtClaimsSet;
        try {
            jwtClaimsSet = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            return Result.failure("Error verifying JWT token. The payload must represent a valid JSON object and a JWT claims set. " + e.getMessage());
        }

        // verify claims
        var exactMatchClaims = new JWTClaimsSet.Builder()
                .audience(audience)
                .build();
        var requiredClaims = Set.of(
                ISSUER_CLAIM,
                SUBJECT_CLAIM,
                EXPIRATION_TIME_CLAIM);

        var claimsVerifier = new DefaultJWTClaimsVerifier<>(exactMatchClaims, requiredClaims);
        try {
            claimsVerifier.verify(jwtClaimsSet);
        } catch (BadJWTException e) {
            return Result.failure("Claim verification failed. " + e.getMessage());
        }

        return Result.success();
    }
}
