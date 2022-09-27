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

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.jwt.JwtRegisteredClaimNames.NOT_BEFORE;

class Oauth2NotBeforeValidationRuleTest {

    private final int notBeforeLeeway = 20;
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final Clock clock = Clock.fixed(now, UTC);
    private final TokenValidationRule rule = new Oauth2NotBeforeValidationRule(clock, notBeforeLeeway);

    @Test
    void validNotBefore() {
        var token = ClaimToken.Builder.newInstance()
                .claim(NOT_BEFORE, Date.from(now.plusSeconds(notBeforeLeeway)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotRespected() {
        var token = ClaimToken.Builder.newInstance()
                .claim(NOT_BEFORE, Date.from(now.plusSeconds(notBeforeLeeway + 1)))
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Current date/time with leeway before the not before (nbf) claim in token");
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotProvided() {
        var token = ClaimToken.Builder.newInstance().build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required not before (nbf) claim is missing in token");
    }

}