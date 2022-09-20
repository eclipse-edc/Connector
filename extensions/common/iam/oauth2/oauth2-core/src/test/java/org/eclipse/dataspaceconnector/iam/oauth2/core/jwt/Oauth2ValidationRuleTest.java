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

import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.iam.oauth2.core.rule.Oauth2ValidationRule;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.AUDIENCE;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.EXPIRATION_TIME;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.ISSUED_AT;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtClaimNames.NOT_BEFORE;

class Oauth2ValidationRuleTest {

    public static final String TEST_AUDIENCE = "test-audience";
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final Clock clock = Clock.fixed(now, UTC);
    private Oauth2ValidationRule rule;

    @BeforeEach
    public void setUp() {
        var configuration = Oauth2Configuration.Builder.newInstance().providerAudience("test-audience").build();
        rule = new Oauth2ValidationRule(configuration, clock);
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotRespected() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of(TEST_AUDIENCE))
                .claim(NOT_BEFORE, Date.from(now.plusSeconds(20)))
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Current date/time with leeway before the not before (nbf) claim in token");
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotProvided() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of(TEST_AUDIENCE))
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required not before (nbf) claim is missing in token");
    }

    @Test
    void validationKoBecauseExpirationTimeNotRespected() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of(TEST_AUDIENCE))
                .claim(NOT_BEFORE, Date.from(now))
                .claim(EXPIRATION_TIME, Date.from(now.minusSeconds(10)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Token has expired (exp)");
    }

    @Test
    void validationKoBecauseExpirationTimeNotProvided() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of(TEST_AUDIENCE))
                .claim(NOT_BEFORE, Date.from(now))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required expiration time (exp) claim is missing in token");
    }

    @Test
    void validationKoBecauseAudienceNotRespected() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of("fake-audience"))
                .claim(NOT_BEFORE, Date.from(now))
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Token audience (aud) claim did not contain connector audience: test-audience");
    }

    @Test
    void validationKoBecauseAudienceNotProvided() {
        var token = ClaimToken.Builder.newInstance()
                .claim(NOT_BEFORE, Date.from(now))
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required audience (aud) claim is missing in token");
    }

    @Test
    void validationOkWhenLeewayOnNotBefore() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of(TEST_AUDIENCE))
                .claim(NOT_BEFORE, Date.from(now.plusSeconds(20)))
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(600)))
                .build();

        var configuration = Oauth2Configuration.Builder.newInstance()
                .providerAudience(TEST_AUDIENCE)
                .notBeforeValidationLeeway(20)
                .build();
        rule = new Oauth2ValidationRule(configuration, clock);

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validationOk() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of(TEST_AUDIENCE))
                .claim(NOT_BEFORE, Date.from(now))
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validationKoBecauseIssuedAtAfterExpires() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of(TEST_AUDIENCE))
                .claim(NOT_BEFORE, Date.from(now))
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(60)))
                .claim(ISSUED_AT, Date.from(now.plusSeconds(65)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1).contains("Issued at (iat) claim is after expiration time (exp) claim in token");
    }

    @Test
    void validationKoBecauseIssuedAtInFuture() {
        var token = ClaimToken.Builder.newInstance()
                .claim(AUDIENCE, List.of(TEST_AUDIENCE))
                .claim(NOT_BEFORE, Date.from(now))
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(60)))
                .claim(ISSUED_AT, Date.from(now.plusSeconds(10)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1).contains("Current date/time before issued at (iat) claim in token");
    }
}
