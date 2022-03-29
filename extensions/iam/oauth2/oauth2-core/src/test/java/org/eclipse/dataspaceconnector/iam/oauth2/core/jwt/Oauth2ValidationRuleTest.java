/*
 *  Copyright (c) 2020 - 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.iam.oauth2.core.rule.Oauth2ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class Oauth2ValidationRuleTest {

    public static final String TEST_AUDIENCE = "test-audience";
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private Oauth2ValidationRule rule;
    private JWSHeader jwsHeader;

    @BeforeEach
    public void setUp() {
        var configuration = Oauth2Configuration.Builder.newInstance().providerAudience("test-audience").build();
        rule = new Oauth2ValidationRule(configuration);
        jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotRespected() {
        var claims = new JWTClaimsSet.Builder()
                .audience(TEST_AUDIENCE)
                .notBeforeTime(Date.from(now.plusSeconds(20)))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Current date/time with leeway before the not before (nbf) claim in token");
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotProvided() {
        var claims = new JWTClaimsSet.Builder()
                .audience(TEST_AUDIENCE)
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required not before (nbf) claim is missing in token");
    }

    @Test
    void validationKoBecauseExpirationTimeNotRespected() {
        var claims = new JWTClaimsSet.Builder()
                .audience(TEST_AUDIENCE)
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.minusSeconds(10)))
                .build();

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Token has expired (exp)");
    }

    @Test
    void validationKoBecauseExpirationTimeNotProvided() {
        var claims = new JWTClaimsSet.Builder()
                .audience(TEST_AUDIENCE)
                .notBeforeTime(Date.from(now))
                .build();

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required expiration time (exp) claim is missing in token");
    }

    @Test
    void validationKoBecauseAudienceNotRespected() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("fake-audience")
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Token audience (aud) claim did not contain connector audience: test-audience");
    }

    @Test
    void validationKoBecauseAudienceNotProvided() {
        var claims = new JWTClaimsSet.Builder()
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required audience (aud) claim is missing in token");
    }

    @Test
    void validationOkWhenLeewayOnNotBefore() {
        var claims = new JWTClaimsSet.Builder()
                .audience(TEST_AUDIENCE)
                .notBeforeTime(Date.from(now.plusSeconds(20)))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();
        var configuration = Oauth2Configuration.Builder.newInstance()
                .providerAudience(TEST_AUDIENCE)
                .notBeforeValidationLeeway(20)
                .build();
        rule = new Oauth2ValidationRule(configuration);

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validationOk() {
        var claims = new JWTClaimsSet.Builder()
                .audience(TEST_AUDIENCE)
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(600)))
                .build();

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validationKoBecauseIssuedAtAfterExpires() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience(TEST_AUDIENCE)
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))
                .issueTime(Date.from(now.plusSeconds(65)))
                .build();

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1).contains("Issued at (iat) claim is after expiration time (exp) claim in token");
    }

    @Test
    void validationKoBecauseIssuedAtInFuture() {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience(TEST_AUDIENCE)
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(60)))
                .issueTime(Date.from(now.plusSeconds(10)))
                .build();

        var jwt = new SignedJWT(jwsHeader, claims);
        var result = rule.checkRule(jwt);

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1).contains("Current date/time before issued at (iat) claim in token");
    }
}
