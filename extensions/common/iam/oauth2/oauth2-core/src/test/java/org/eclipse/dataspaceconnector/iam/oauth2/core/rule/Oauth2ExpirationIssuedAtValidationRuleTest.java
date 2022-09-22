/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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


package org.eclipse.dataspaceconnector.iam.oauth2.core.rule;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.jwt.TokenValidationRule;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.NOT_BEFORE;

class Oauth2ExpirationIssuedAtValidationRuleTest {

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final Clock clock = Clock.fixed(now, UTC);
    private final TokenValidationRule rule = new Oauth2ExpirationIssuedAtValidationRule(clock);

    @Test
    void validationOk() {
        var token = ClaimToken.Builder.newInstance()
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(600)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validationKoBecauseExpirationTimeNotRespected() {
        var token = ClaimToken.Builder.newInstance()
                .claim(EXPIRATION_TIME, Date.from(now.minusSeconds(10)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Token has expired (exp)");
    }

    @Test
    void validationKoBecauseExpirationTimeNotProvided() {
        var token = ClaimToken.Builder.newInstance().build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required expiration time (exp) claim is missing in token");
    }

    @Test
    void validationKoBecauseIssuedAtAfterExpires() {
        var token = ClaimToken.Builder.newInstance()
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
                .claim(EXPIRATION_TIME, Date.from(now.plusSeconds(60)))
                .claim(ISSUED_AT, Date.from(now.plusSeconds(10)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1).contains("Current date/time before issued at (iat) claim in token");
    }


}