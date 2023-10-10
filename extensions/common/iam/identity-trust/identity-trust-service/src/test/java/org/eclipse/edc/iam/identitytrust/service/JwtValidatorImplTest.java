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

package org.eclipse.edc.iam.identitytrust.service;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.edc.iam.identitytrust.validation.JwtValidatorImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.eclipse.edc.identitytrust.TestFunctions.createJwt;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class JwtValidatorImplTest {

    private static final String EXPECTED_OWN_DID = "did:web:provider";
    private static final String CONSUMER_DID = "did:web:consumer";
    private final JwtValidatorImpl validator = new JwtValidatorImpl();

    @BeforeEach
    void setUp() {
    }

    @Test
    void success() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject(CONSUMER_DID)
                .issuer(CONSUMER_DID)
                .audience(EXPECTED_OWN_DID)
                .claim("jti", UUID.randomUUID().toString())
                .claim("client_id", CONSUMER_DID)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        var token = createJwt(claimsSet);
        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isSucceeded();
    }

    @Test
    void issAndSubNotEqual() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject("did:web:anotherconsumer")
                .issuer(CONSUMER_DID)
                .audience(EXPECTED_OWN_DID)
                .claim("jti", UUID.randomUUID().toString())
                .claim("client_id", CONSUMER_DID)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        assertThat(validator.validateToken(createJwt(claimsSet), "some-aud"))
                .isFailed()
                .messages().hasSize(1)
                .containsExactly("The iss and aud claims must be identical.");
    }

    @Disabled("not testable")
    @Test
    void issAndSubNotSetToConsumerDid() {
        //this is not testable, since we have no way of obtaining the consumer's DID
    }

    @Test
    void audNotEqualToOwnDid() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject(CONSUMER_DID)
                .issuer(CONSUMER_DID)
                .audience("invalid-audience")
                .claim("jti", UUID.randomUUID().toString())
                .claim("client_id", CONSUMER_DID)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        var token = createJwt(claimsSet);
        assertThat(validator.validateToken(token, EXPECTED_OWN_DID))
                .isFailed()
                .messages().hasSize(1)
                .containsExactly("aud claim expected to be %s but was [%s]".formatted(EXPECTED_OWN_DID, "invalid-audience"));
    }

    @Test
    void clientIdClaim_NotEqualToConsumerDid() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject(CONSUMER_DID)
                .issuer(CONSUMER_DID)
                .audience(EXPECTED_OWN_DID)
                .claim("jti", UUID.randomUUID().toString())
                .claim("client_id", "invalid_client_id")
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();
        var token = createJwt(claimsSet);
        assertThat(validator.validateToken(token, EXPECTED_OWN_DID))
                .isFailed()
                .messages().hasSize(1)
                .containsExactly("client_id must be equal to the issuer ID");
    }

    @Test
    void subJwkClaimPresent() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject(CONSUMER_DID)
                .issuer(CONSUMER_DID)
                .audience(EXPECTED_OWN_DID)
                .claim("sub_jwk", "somejwk")
                .claim("jti", UUID.randomUUID().toString())
                .claim("client_id", CONSUMER_DID)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        var token = createJwt(claimsSet);
        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isFailed()
                .messages().hasSize(1)
                .containsExactly("The sub_jwk claim must not be present.");
    }


    @Test
    void jtiNotPresent() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject(CONSUMER_DID)
                .issuer(CONSUMER_DID)
                .audience(EXPECTED_OWN_DID)
                .claim("client_id", CONSUMER_DID)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        var token = createJwt(claimsSet);
        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isFailed()
                .messages().hasSize(1)
                .containsExactly("The jti claim is mandatory.");
    }

    @Test
    void expNotPresent() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject(CONSUMER_DID)
                .issuer(CONSUMER_DID)
                .audience(EXPECTED_OWN_DID)
                .claim("jti", UUID.randomUUID().toString())
                .claim("client_id", CONSUMER_DID)
                .build();

        var token = createJwt(claimsSet);
        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isFailed()
                .messages().hasSize(1)
                .containsExactly("The exp claim is mandatory.");
    }

    @Test
    void tokenExpired() {
        var claimsSet = new JWTClaimsSet.Builder()
                .subject(CONSUMER_DID)
                .issuer(CONSUMER_DID)
                .audience(EXPECTED_OWN_DID)
                .claim("jti", UUID.randomUUID().toString())
                .claim("client_id", CONSUMER_DID)
                .expirationTime(Date.from(Instant.now().minusSeconds(3600)))
                .build();

        var token = createJwt(claimsSet);
        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isFailed()
                .messages().hasSize(1)
                .containsExactly("The token must not be expired.");
    }

}