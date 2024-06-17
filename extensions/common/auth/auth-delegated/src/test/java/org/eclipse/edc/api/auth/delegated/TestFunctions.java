/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.auth.delegated;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.security.token.jwt.CryptoConverter;

public class TestFunctions {
    public static JWK generateKey() {
        return generateKey("test-key");
    }

    public static JWK generateKey(String keyId) {
        try {
            return new OctetKeyPairGenerator(Curve.Ed25519).keyID(keyId).keyUse(KeyUse.SIGNATURE).generate();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public static String createToken(JWK key) {
        var signer = CryptoConverter.createSigner(key);
        var algorithm = CryptoConverter.getRecommendedAlgorithm(signer);

        var header = new JWSHeader.Builder(algorithm).keyID(key.getKeyID()).build();
        var claims = new JWTClaimsSet.Builder()
                .audience("test-audience")
                .issuer("test-issuer")
                .subject("test-subject")
                .build();

        var jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
