/*
 *  Copyright (c) 2020 - 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.oauth2.spi;

import org.eclipse.edc.spi.iam.TokenParameters;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.EXPIRATION_TIME;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUED_AT;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.JWT_ID;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;

class Oauth2AssertionDecoratorTest {

    private static final long TOKEN_EXPIRATION = 500;

    private final Instant now = Instant.now();
    private final Clock clock = Clock.fixed(now, UTC);
    private final String audience = "test-audience";
    private final String clientId = UUID.randomUUID().toString();

    @ParameterizedTest
    @EmptySource
    @NullSource
    @ValueSource(strings = {"test-kid", " ", "    "})
    void verifyDecorate(String kid) {
        var decorator = Oauth2AssertionDecorator.Builder.newInstance()
                .audience(audience)
                .clientId(clientId)
                .clock(clock)
                .validity(TOKEN_EXPIRATION)
                .kid(kid)
                .build();

        var b = TokenParameters.Builder.newInstance();
        decorator.decorate(b);

        var t = b.build();
        assertThat(t.getHeaders().get("kid")).isEqualTo(kid);
        assertThat(t.getClaims())
                .hasEntrySatisfying(AUDIENCE, o -> assertThat(o).asInstanceOf(list(String.class)).contains(audience))
                .hasFieldOrPropertyWithValue(ISSUER, clientId)
                .hasFieldOrPropertyWithValue(SUBJECT, clientId)
                .containsKey(JWT_ID)
                .hasEntrySatisfying(ISSUED_AT, issueDate -> assertThat((Date) issueDate).isEqualTo(now))
                .hasEntrySatisfying(EXPIRATION_TIME, expiration -> assertThat((Date) expiration).isEqualTo(now.plusSeconds(TOKEN_EXPIRATION)));
    }

}