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

package org.eclipse.dataspaceconnector.iam.oauth2.core.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultJwtDecoratorTest {

    private static final long TOKEN_EXPIRATION = 500;

    private String audience;
    private String clientId;
    private Instant now;

    private DefaultJwtDecorator decorator;

    @BeforeEach
    void setUp() {
        audience = "test-audience";
        clientId = UUID.randomUUID().toString();
        byte[] certificate = new byte[50];
        new Random().nextBytes(certificate);
        now = Instant.now();
        var clock = Clock.fixed(now, UTC);
        decorator = new DefaultJwtDecorator(audience, clientId, certificate, clock, TOKEN_EXPIRATION);
    }

    @Test
    void decorate() {
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256);
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();

        decorator.decorate(headerBuilder, claimsBuilder);

        JWSHeader header = headerBuilder.build();
        JWTClaimsSet claims = claimsBuilder.build();

        assertThat(header.getIncludedParams()).contains("x5t");
        assertThat(claims.getClaims())
                .hasEntrySatisfying("aud", o -> assertThat(List.class.cast(o)).contains(audience))
                .hasFieldOrPropertyWithValue("iss", clientId)
                .hasFieldOrPropertyWithValue("sub", clientId)
                .containsKey("jti")
                .hasEntrySatisfying("iat", issueDate -> assertDateIs((Date) issueDate, now))
                .hasEntrySatisfying("exp", expiration -> assertDateIs((Date) expiration, now.plusSeconds(TOKEN_EXPIRATION)));
    }

    private static void assertDateIs(Date date1, Instant date2) {
        assertThat(date1).isEqualTo(Date.from(date2));
    }
}