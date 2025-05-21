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

package org.eclipse.edc.iam.identitytrust.spi;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.spi.iam.TokenRepresentation;

import java.util.Date;
import java.util.Map;

import static org.eclipse.edc.iam.identitytrust.spi.SelfIssuedTokenConstants.PRESENTATION_TOKEN_CLAIM;

public class TestFunctions {

    public static final Issuer TRUSTED_ISSUER = new Issuer("http://test.issuer", Map.of());

    public static TokenRepresentation createToken() {
        return createToken("did:web:test", "test-audience");
    }

    public static TokenRepresentation createToken(String issuer, String subject) {

        var claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .claim(PRESENTATION_TOKEN_CLAIM, createToken(new JWTClaimsSet.Builder().claim("scope", "fooscope").build()).getToken())
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        return createToken(claimsSet);

    }

    public static TokenRepresentation createToken(JWTClaimsSet claimsSet, ECKey key) {
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

    public static TokenRepresentation createToken(JWTClaimsSet claimsSet) {
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
                    .token("Bearer " + signedJwt.serialize())
                    .build();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
