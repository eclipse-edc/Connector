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

package org.eclipse.dataspaceconnector.iam.did.crypto.credentials;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.did.crypto.CryptoException;
import org.eclipse.dataspaceconnector.iam.did.crypto.key.EcPrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PrivateKeyWrapper;
import org.eclipse.dataspaceconnector.iam.did.spi.key.PublicKeyWrapper;

import java.text.ParseException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Convenience/helper class to generate, verify and deserialize verifiable credentials, which are, in fact, Signed JSON Web Tokens (JWTs).
 */
public class VerifiableCredentialFactory {

    public static final String OWNER_CLAIM = "owner";


    /**
     * Creates a signed JWT {@link SignedJWT} that contains a set of claims and an issuer.
     *
     * Although all private key types are possible, in the context of Distributed Identity and ION using an Elliptic Curve key ({@code prime256v1}) is advisable. This can be
     * achieved using OpenSSL CLI:
     *
     * {@code openssl ecparam -name prime256v1 -genkey -noout -out prime256v1-key.pem}
     *
     * @param privateKeyPemContent The contents of a private key stored in PEM format.
     * @param claims a list of key-value-pairs that contain claims
     * @param issuer the "owner" of the VC, in most cases this will be the connector ID. The VC will store this in the "iss" claim
     * @param clock clock used to get current time
     * @return a {@code SignedJWT} that is signed with the private key and contains all claims listed
     */
    public static SignedJWT create(String privateKeyPemContent, Map<String, String> claims, String issuer, Clock clock) {
        try {
            var key = ECKey.parseFromPEMEncodedObjects(privateKeyPemContent);
            return create((ECKey) key, claims, issuer, clock);
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Creates a signed JWT {@link SignedJWT} that contains a set of claims and an issuer. Although all private key types are possible, in the context of Distributed Identity
     * and ION using an Elliptic Curve key ({@code P-256}) is advisable.
     *
     * @param privateKey A Private Key represented as {@link ECKey}.
     * @param claims a list of key-value-pairs that contain claims
     * @param issuer the "owner" of the VC, in most cases this will be the DID ID. The VC will store this in the "iss" claim
     * @param clock clock used to get current time
     * @return a {@code SignedJWT} that is signed with the private key and contains all claims listed
     */
    public static SignedJWT create(ECKey privateKey, Map<String, String> claims, String issuer, Clock clock) {
        return create(new EcPrivateKeyWrapper(privateKey), claims, issuer, clock);
    }

    /**
     * Creates a signed JWT {@link SignedJWT} that contains a set of claims and an issuer. Although all private key types are possible, in the context of Distributed Identity and ION
     * using an Elliptic Curve key ({@code P-256}) is advisable.
     *
     * @param privateKey A Private Key represented as {@link PrivateKeyWrapper}.
     * @param claims a list of key-value-pairs that contain claims
     * @param issuer the "owner" of the VC, in most cases this will be the DID ID. The VC will store this in the "iss" claim
     * @param clock clock used to get current time
     * @return a {@code SignedJWT} that is signed with the private key and contains all claims listed
     */
    public static SignedJWT create(PrivateKeyWrapper privateKey, Map<String, String> claims, String issuer, Clock clock) {
        var claimssetBuilder = new JWTClaimsSet.Builder();

        claims.forEach(claimssetBuilder::claim);
        var claimsSet = claimssetBuilder.issuer(issuer)
                .subject("verifiable-credential")
                .expirationTime(Date.from(clock.instant().plus(10, ChronoUnit.MINUTES)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSSigner signer = privateKey.signer();
        //prefer ES256 if available, otherwise use the "next best"
        JWSAlgorithm algorithm = signer.supportedJWSAlgorithms().contains(JWSAlgorithm.ES256) ?
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
     * @param verifiableCredential a {@link SignedJWT} that was sent by the claiming party.
     * @param publicKey The claiming party's public key
     * @return true if verified, false otherwise
     */
    public static boolean verify(SignedJWT verifiableCredential, ECKey publicKey) {
        try {
            return verifiableCredential.verify(new ECDSAVerifier(publicKey));
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Verifies a VerifiableCredential using the issuer's public key
     *
     * @param verifiableCredential a {@link SignedJWT} that was sent by the claiming party.
     * @param publicKey The claiming party's public key, passed as a {@link PublicKeyWrapper}
     * @return true if verified, false otherwise
     */
    public static boolean verify(SignedJWT verifiableCredential, PublicKeyWrapper publicKey) {
        try {
            return verifiableCredential.verify(publicKey.verifier());
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * Verifies a VerifiableCredential using the issuer's public key
     *
     * @param verifiableCredential a {@link SignedJWT} that was sent by the claiming party.
     * @param publicKeyPemContent The claiming party's public key, i.e. the contents of the public key PEM file.
     * @return true if verified, false otherwise
     */
    public static boolean verify(SignedJWT verifiableCredential, String publicKeyPemContent) {
        try {
            var key = ECKey.parseFromPEMEncodedObjects(publicKeyPemContent);
            return verify(verifiableCredential, (ECKey) key);
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }

    }

    /**
     * Parses a {@link SignedJWT} back to a Java object from its serialized form.
     *
     * @param jwtString The serialized form of the {@code SignedJWT}, which can be generated using {@link SignedJWT#serialize()}.
     * @return a {@link SignedJWT} containing the decoded information
     */
    public static SignedJWT parse(String jwtString) {
        try {
            return SignedJWT.parse(jwtString);
        } catch (ParseException e) {
            throw new CryptoException(e);
        }
    }
}
