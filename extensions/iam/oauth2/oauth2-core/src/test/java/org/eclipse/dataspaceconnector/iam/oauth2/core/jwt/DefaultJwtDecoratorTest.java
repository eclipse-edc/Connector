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

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultJwtDecoratorTest {

    private static final long TOKEN_EXPIRATION = 500;

    private static final long DELTA_MILLISECONDS = TimeUnit.SECONDS.toMillis(1);

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
        decorator = new DefaultJwtDecorator(audience, clientId, certificate, TOKEN_EXPIRATION);
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
                .hasEntrySatisfying("iat", issueDate -> assertDatesAreClose((Date) issueDate, Date.from(now)))
                .hasEntrySatisfying("nbf", notBefore -> assertDatesAreClose((Date) notBefore, Date.from(now)))
                .hasEntrySatisfying("exp", expiration -> assertDatesAreClose((Date) expiration, Date.from(now.plusSeconds(TOKEN_EXPIRATION))));
    }

    private static void assertDatesAreClose(Date date1, Date date2) {
        assertThat(date1).isCloseTo(date2, DELTA_MILLISECONDS);
    }
}