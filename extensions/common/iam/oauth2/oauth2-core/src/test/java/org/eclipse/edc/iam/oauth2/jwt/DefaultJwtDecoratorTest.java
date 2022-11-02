/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.iam.oauth2.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Random;
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

class DefaultJwtDecoratorTest {

    private static final long TOKEN_EXPIRATION = 500;

    private String audience;
    private String clientId;
    private final Instant now = Instant.now();

    private DefaultJwtDecorator decorator;

    @BeforeEach
    void setUp() {
        audience = "test-audience";
        clientId = UUID.randomUUID().toString();
        byte[] certificate = new byte[50];
        new Random().nextBytes(certificate);
        var clock = Clock.fixed(now, UTC);
        decorator = new DefaultJwtDecorator(audience, clientId, certificate, clock, TOKEN_EXPIRATION);
    }

    @Test
    void claims() {
        var claims = decorator.claims();

        assertThat(claims)
                .hasEntrySatisfying(AUDIENCE, o -> assertThat(o).asInstanceOf(list(String.class)).contains(audience))
                .hasFieldOrPropertyWithValue(ISSUER, clientId)
                .hasFieldOrPropertyWithValue(SUBJECT, clientId)
                .containsKey(JWT_ID)
                .hasEntrySatisfying(ISSUED_AT, issueDate -> assertThat((Date) issueDate).isEqualTo(now))
                .hasEntrySatisfying(EXPIRATION_TIME, expiration -> assertThat((Date) expiration).isEqualTo(now.plusSeconds(TOKEN_EXPIRATION)));
    }

    @Test
    void headers() {
        var result = decorator.headers();

        assertThat(result).containsKey("x5t");
    }

}