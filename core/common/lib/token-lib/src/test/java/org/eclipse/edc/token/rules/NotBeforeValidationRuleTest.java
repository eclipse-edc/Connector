/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.token.rules;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class NotBeforeValidationRuleTest {

    private final int notBeforeLeeway = 20;
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final Clock clock = Clock.fixed(now, UTC);
    private final TokenValidationRule rule = new NotBeforeValidationRule(clock, notBeforeLeeway, false);

    @Test
    void validNotBefore() {
        var token = ClaimToken.Builder.newInstance()
                .claim(NOT_BEFORE, now.plusSeconds(notBeforeLeeway).getEpochSecond())
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotRespected() {
        var token = ClaimToken.Builder.newInstance()
                .claim(NOT_BEFORE, now.plusSeconds(notBeforeLeeway + 1).getEpochSecond())
                .build();

        var result = rule.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Current date/time with leeway before the not before (nbf) claim in token");
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotProvided_doesNotAllowNull() {
        var r = new NotBeforeValidationRule(clock, notBeforeLeeway, false);
        var token = ClaimToken.Builder.newInstance().build();

        var result = r.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isFalse();
        assertThat(result.getFailureMessages()).hasSize(1)
                .contains("Required not before (nbf) claim is missing in token");
    }

    @Test
    void validationKoBecauseNotBeforeTimeNotProvided_allowsNull() {
        var r = new NotBeforeValidationRule(clock, notBeforeLeeway, true);
        var token = ClaimToken.Builder.newInstance().build();

        var result = r.checkRule(token, emptyMap());

        assertThat(result.succeeded()).isTrue();
    }

}
