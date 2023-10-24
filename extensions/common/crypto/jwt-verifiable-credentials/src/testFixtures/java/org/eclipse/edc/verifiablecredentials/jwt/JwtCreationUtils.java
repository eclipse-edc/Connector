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

package org.eclipse.edc.verifiablecredentials.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.sql.Date;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class JwtCreationUtils {
    public static String createJwt(ECKey privateKey, String issuerId, String subject, String audience, Map<String, Object> claims) {
        try {
            var signer = new ECDSASigner(privateKey.toECPrivateKey());

            // Prepare JWT with claims set
            var now = Date.from(Instant.now());
            var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuerId)
                    .subject(subject)
                    .issueTime(now)
                    .audience(audience)
                    .notBeforeTime(now)
                    .claim("jti", UUID.randomUUID().toString())
                    .expirationTime(Date.from(Instant.now().plusSeconds(60)));

            claims.forEach(claimsSet::claim);

            var signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(privateKey.getKeyID()).build(), claimsSet.build());

            signedJwt.sign(signer);

            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
