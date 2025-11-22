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

package org.eclipse.edc.iam.decentralizedclaims.service.validation;

class SelfIssuedIdTokenValidatorTest {

//    private static final String EXPECTED_OWN_DID = "did:web:provider";
//    private static final String CONSUMER_DID = "did:web:consumer";
//    private final SelfIssuedIdTokenValidator validator = new SelfIssuedIdTokenValidator();
//
//    @BeforeEach
//    void setUp() {
//    }
//
//    @Test
//    void success() {
//        var claimsSet = new JWTClaimsSet.Builder()
//                .subject(CONSUMER_DID)
//                .issuer(CONSUMER_DID)
//                .audience(EXPECTED_OWN_DID)
//                .claim("jti", UUID.randomUUID().toString())
//                .claim(PRESENTATION_ACCESS_TOKEN_CLAIM, "foobar")
//                .claim("client_id", CONSUMER_DID)
//                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
//                .build();
//
//        var token = createJwt(claimsSet);
//        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isSucceeded();
//    }
//
//    @Test
//    void issAndSubNotEqual() {
//        var claimsSet = new JWTClaimsSet.Builder()
//                .subject("did:web:anotherconsumer")
//                .issuer(CONSUMER_DID)
//                .audience(EXPECTED_OWN_DID)
//                .claim("jti", UUID.randomUUID().toString())
//                .claim("client_id", CONSUMER_DID)
//                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
//                .build();
//        assertThat(validator.validateToken(createJwt(claimsSet), "some-aud"))
//                .isFailed()
//                .detail().isEqualTo("The iss and aud claims must be identical.");
//    }
//
//    @Disabled("not testable")
//    @Test
//    void issAndSubNotSetToConsumerDid() {
//        //this is not testable, since we have no way of obtaining the consumer's DID
//    }
//
//    @Test
//    void audNotEqualToOwnDid() {
//        var claimsSet = new JWTClaimsSet.Builder()
//                .subject(CONSUMER_DID)
//                .issuer(CONSUMER_DID)
//                .audience("invalid-audience")
//                .claim("jti", UUID.randomUUID().toString())
//                .claim("client_id", CONSUMER_DID)
//                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
//                .build();
//        var token = createJwt(claimsSet);
//        assertThat(validator.validateToken(token, EXPECTED_OWN_DID))
//                .isFailed()
//                .detail().isEqualTo("The aud claim expected to be %s but was [%s]".formatted(EXPECTED_OWN_DID, "invalid-audience"));
//    }
//
//    @Test
//    void subJwkClaimPresent() {
//        var claimsSet = new JWTClaimsSet.Builder()
//                .subject(CONSUMER_DID)
//                .issuer(CONSUMER_DID)
//                .audience(EXPECTED_OWN_DID)
//                .claim("sub_jwk", "somejwk")
//                .claim("jti", UUID.randomUUID().toString())
//                .claim("client_id", CONSUMER_DID)
//                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
//                .build();
//
//        var token = createJwt(claimsSet);
//        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isFailed()
//                .detail().isEqualTo("The sub_jwk claim must not be present.");
//    }
//
//
//    @Test
//    void jtiNotPresent() {
//        var claimsSet = new JWTClaimsSet.Builder()
//                .subject(CONSUMER_DID)
//                .issuer(CONSUMER_DID)
//                .audience(EXPECTED_OWN_DID)
//                .claim("client_id", CONSUMER_DID)
//                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
//                .build();
//
//        var token = createJwt(claimsSet);
//        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isFailed()
//                .detail().isEqualTo("The jti claim is mandatory.");
//    }
//
//    @Test
//    void expNotPresent() {
//        var claimsSet = new JWTClaimsSet.Builder()
//                .subject(CONSUMER_DID)
//                .issuer(CONSUMER_DID)
//                .audience(EXPECTED_OWN_DID)
//                .claim("jti", UUID.randomUUID().toString())
//                .claim("client_id", CONSUMER_DID)
//                .build();
//
//        var token = createJwt(claimsSet);
//        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isFailed()
//                .detail().isEqualTo("The exp claim is mandatory.");
//    }
//
//    @Test
//    void tokenExpired() {
//        var claimsSet = new JWTClaimsSet.Builder()
//                .subject(CONSUMER_DID)
//                .issuer(CONSUMER_DID)
//                .audience(EXPECTED_OWN_DID)
//                .claim("jti", UUID.randomUUID().toString())
//                .claim("client_id", CONSUMER_DID)
//                .expirationTime(Date.from(Instant.now().minusSeconds(3600)))
//                .build();
//
//        var token = createJwt(claimsSet);
//        assertThat(validator.validateToken(token, EXPECTED_OWN_DID)).isFailed()
//                .detail().isEqualTo("The token must not be expired.");
//    }

}