/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identitytrust;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.identitytrust.model.CredentialFormat;
import org.eclipse.edc.identitytrust.model.CredentialSubject;
import org.eclipse.edc.identitytrust.model.Issuer;
import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.identitytrust.model.VerifiablePresentation;
import org.eclipse.edc.identitytrust.model.VerifiablePresentationContainer;
import org.eclipse.edc.spi.iam.TokenRepresentation;

import java.util.Date;
import java.util.Map;

import static java.time.Instant.now;

public class TestFunctions {

    public static VerifiableCredential createCredential() {
        return createCredentialBuilder().build();
    }

    public static VerifiableCredential.Builder createCredentialBuilder() {
        return VerifiableCredential.Builder.newInstance()
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("test-subject-id")
                        .claim("test-claim", "test-value")
                        .build())
                .type("test-type")
                .issuer(new Issuer("http://test.issuer", Map.of()))
                .issuanceDate(now());
    }

    public static VerifiablePresentation.Builder createPresentationBuilder() {
        return VerifiablePresentation.Builder.newInstance()
                .credential(createCredentialBuilder().build())
                .holder("did:web:testholder234")
                .id("test-id");
    }

    public static VerifiablePresentationContainer createPresentationContainer() {
        return new VerifiablePresentationContainer("RAW_VP", CredentialFormat.JSON_LD, createPresentationBuilder().type("VerifiableCredential").build());
    }

    public static TokenRepresentation createJwt() {
        return createJwt("did:web:test", "test-audience");
    }

    public static TokenRepresentation createJwt(String issuer, String subject) {

        var claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        return createJwt(claimsSet);

    }

    public static TokenRepresentation createJwt(JWTClaimsSet claimsSet, ECKey key) {
        // Generate an EC key pair
        try {

            var signer = new ECDSASigner(key);

            var signedJwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID()).build(),
                    claimsSet);

            signedJwt.sign(signer);

            return TokenRepresentation.Builder.newInstance()
                    .token(signedJwt.serialize())
                    .build();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public static TokenRepresentation createJwt(JWTClaimsSet claimsSet) {
        // Generate an EC key pair
        ECKey ecJwk;
        try {
            ecJwk = new ECKeyGenerator(Curve.P_256)
                    .keyID("123")
                    .generate();

            var signer = new ECDSASigner(ecJwk);

            var signedJwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(ecJwk.getKeyID()).build(),
                    claimsSet);

            signedJwt.sign(signer);

            return TokenRepresentation.Builder.newInstance()
                    .token(signedJwt.serialize())
                    .build();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
